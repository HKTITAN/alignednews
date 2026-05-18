package ai.aligned.ui.summarize

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.StoryDto
import ai.aligned.net.dto.TweetDto
import ai.aligned.net.dto.TweetInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pasted X/Twitter URL → resolve the related-tweet cluster → POST /api/summarize.
 *
 * Resolution strategy:
 *  1. Extract tweet ID from the URL via [tweetIdFromUrl].
 *  2. Look in the cached feed for a story whose tweets contain that ID. Stories
 *     on alignednews.ai are already clusters of the original tweet + its quotes
 *     and replies, so finding one story gives us the whole thread for free.
 *  3. If nothing in cache, try `/api/search` with the username from the URL.
 *  4. As a final fallback, send a single placeholder tweet so the model can
 *     still respond with something meaningful.
 */
data class SummarizeState(
    val loading: Boolean = false,
    val source: String = "",         // "Cached cluster", "Searched feed", "Direct"
    val story: StoryDto? = null,     // resolved story, if any
    val tweets: List<TweetDto> = emptyList(),
    val overview: String = "",
    val perTweet: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SummarizeViewModel @Inject constructor(
    private val api: AlignedApi,
    private val repo: StoryRepository
) : ViewModel() {

    val input: MutableState<String> = mutableStateOf("")
    private val _state = MutableStateFlow(SummarizeState())
    val state: StateFlow<SummarizeState> = _state.asStateFlow()

    fun setInput(s: String) { input.value = s }

    fun submit() {
        val url = input.value.trim()
        if (url.isBlank() || _state.value.loading) return
        val tweetId = tweetIdFromUrl(url)
        val handle = handleFromUrl(url)

        _state.value = SummarizeState(loading = true)
        viewModelScope.launch {
            runCatching {
                val resolved = resolveCluster(tweetId, handle, url)
                if (resolved.tweets.isEmpty()) {
                    error("Couldn't find any tweets for this URL. Try pasting the tweet text instead.")
                }
                val resp = api.summarize(resolved.tweets.map {
                    TweetInput(text = it.text, authorUsername = it.authorUsername)
                })
                _state.value = SummarizeState(
                    loading = false,
                    source = resolved.source,
                    story = resolved.story,
                    tweets = resolved.tweets,
                    overview = resp.overview,
                    perTweet = resp.individual
                )
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to summarize") }
            }
        }
    }

    private data class Cluster(
        val source: String,
        val story: StoryDto?,
        val tweets: List<TweetDto>
    )

    private suspend fun resolveCluster(tweetId: String?, handle: String?, originalUrl: String): Cluster {
        // 1. Cached cluster
        if (tweetId != null) {
            repo.findStoryByTweetId(tweetId)?.let { story ->
                return Cluster("Found in cached feed cluster", story, story.tweets)
            }
        }
        // 2. Search by handle, then look for the tweet within those story results
        if (handle != null) {
            val searched = runCatching { api.search(handle, limit = 20) }.getOrNull()
            searched?.stories?.let { stories ->
                if (tweetId != null) {
                    val token = "/status/$tweetId"
                    for (story in stories) {
                        if (story.tweets.any { it.url.contains(token) || it.id == tweetId }) {
                            return Cluster("Resolved via /api/search", story, story.tweets)
                        }
                    }
                }
                stories.firstOrNull()?.let { story ->
                    return Cluster("Closest match by author", story, story.tweets)
                }
            }
        }
        // 3. Direct: send a single placeholder so the model can still try.
        val placeholder = TweetDto(
            id = tweetId ?: "0", text = "Tweet at $originalUrl",
            authorName = handle ?: "", authorUsername = handle ?: "",
            authorProfileImage = "", createdAt = "",
            url = originalUrl
        )
        return Cluster("Direct (tweet not in feed)", null, listOf(placeholder))
    }

    fun clear() {
        input.value = ""
        _state.value = SummarizeState()
    }
}

private val TweetIdRegex = Regex("""(?:twitter\.com|x\.com)/([^/?#]+)/status/(\d+)""", RegexOption.IGNORE_CASE)

fun tweetIdFromUrl(url: String): String? = TweetIdRegex.find(url)?.groupValues?.get(2)
fun handleFromUrl(url: String): String?  = TweetIdRegex.find(url)?.groupValues?.get(1)
