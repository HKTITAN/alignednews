package ai.aligned.ui.story

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.AlignedApi
import ai.aligned.net.Vote as ApiVote
import ai.aligned.net.dto.StoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Vote { NONE, UP, DOWN }

data class StoryState(
    val loading: Boolean = true,
    val story: StoryDto? = null,
    val error: String? = null,
    val voteHighlight: Vote = Vote.NONE,
    val bookmarked: Boolean = false
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val api: AlignedApi,
    private val repo: StoryRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val storyId: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow(StoryState())
    val state: StateFlow<StoryState> = _state.asStateFlow()

    init {
        reload()
        observeBookmark()
    }

    private fun observeBookmark() {
        viewModelScope.launch {
            repo.observeIsBookmarked(storyId).collect { b ->
                _state.update { it.copy(bookmarked = b) }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repo.story(storyId) }
                .onSuccess { s -> _state.update { it.copy(loading = false, story = s) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun vote(up: Boolean) {
        val s = _state.value.story ?: return
        _state.update { it.copy(voteHighlight = if (up) Vote.UP else Vote.DOWN) }
        viewModelScope.launch {
            runCatching { api.feedback(s.id, s.category, if (up) ApiVote.Up else ApiVote.Down) }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, voteHighlight = Vote.NONE) }
                }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch { repo.toggleBookmark(storyId) }
    }

    fun shareUrl(id: String): String = "https://alignednews.ai/story/$id"

    fun infographicUrl(id: String): String = api.infographicUrl(id)
}
