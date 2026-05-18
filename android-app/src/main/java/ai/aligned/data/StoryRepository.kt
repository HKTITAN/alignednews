package ai.aligned.data

import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.CategoriesDto
import ai.aligned.net.dto.EventsDto
import ai.aligned.net.dto.HistoryEntryDto
import ai.aligned.net.dto.ListsDto
import ai.aligned.net.dto.MapDto
import ai.aligned.net.dto.StoryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache-then-network repository. Story rows are retained for 7 days
 * ([STORY_TTL_MS]); /api/lists, /api/map, /api/events, /api/categories use the
 * warm tier TTL ([WARM_TTL_MS]); /api/history is "cold" tier ([COLD_TTL_MS]).
 *
 * For each `getXxx()` we:
 *   1. Try cache. If fresh, return it without hitting the network.
 *   2. Otherwise try the network; on success, cache the result and return.
 *   3. On network failure, fall back to the stale cache (better than crashing).
 *
 * `refresh()` always hits the network and is what background sync calls.
 */
@Singleton
class StoryRepository @Inject constructor(
    private val api: AlignedApi,
    private val db: AlignedDb
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    // ─── Feed (always observe cache; trigger network on refresh) ─────────────

    fun observe(): Flow<List<StoryDto>> =
        db.stories().observeAll().map { rows ->
            rows.map { json.decodeFromString<StoryDto>(it.payloadJson) }
        }

    suspend fun refresh(): SyncDiff {
        val resp = api.news()
        val stories = resp.stories
        val now = System.currentTimeMillis()
        val rows = stories.map { s ->
            val raw = json.encodeToString(StoryDto.serializer(), s)
            CachedStory(
                id = s.id, category = s.category, headline = s.headline,
                summary = s.summary, createdAt = s.createdAt, updatedAt = s.updatedAt,
                tweetCount = s.tweetCount, totalEngagement = s.totalEngagement,
                payloadJson = raw, payloadHash = raw.hashCode().toString(),
                fetchedAt = now
            )
        }
        val existingIds = stories.mapNotNull { db.stories().get(it.id)?.id }.toSet()
        val newIds = stories.map { it.id }.toSet() - existingIds
        db.stories().upsert(rows)
        // Don't prune by ID — keep older stories that fell out of /api/news so
        // bookmarks still resolve. Only prune by age (TTL).
        db.stories().prune(now - STORY_TTL_MS)
        db.storyDetail().prune(now - DETAIL_TTL_MS)
        db.syncLog().insert(SyncLogRow(
            tier = "hot", ok = true,
            message = "${stories.size} stories, ${newIds.size} new",
            tsEpochMs = now
        ))
        return SyncDiff(totalStories = stories.size, newStoryIds = newIds.toList())
    }

    // ─── Story detail with 7-day TTL ────────────────────────────────────────

    suspend fun story(id: String): StoryDto {
        val now = System.currentTimeMillis()
        val cached = db.storyDetail().get(id)
        if (cached != null && now - cached.fetchedAt < DETAIL_TTL_MS) {
            return json.decodeFromString(StoryDto.serializer(), cached.payloadJson)
        }
        return runCatching { api.story(id) }
            .onSuccess { fetched ->
                val raw = json.encodeToString(StoryDto.serializer(), fetched)
                db.storyDetail().upsert(
                    CachedStoryDetail(
                        id = id, payloadJson = raw,
                        payloadHash = raw.hashCode().toString(),
                        fetchedAt = now
                    )
                )
            }
            .recover { e ->
                // Fall back to stale cache if present, else rethrow
                cached?.let { return@recover json.decodeFromString(StoryDto.serializer(), it.payloadJson) }
                throw e
            }
            .getOrThrow()
    }

    /** Returns the cached story summary (from /api/news cache) without a network call. */
    suspend fun storyOrCached(id: String): StoryDto? {
        db.storyDetail().get(id)?.let {
            return json.decodeFromString(StoryDto.serializer(), it.payloadJson)
        }
        db.stories().get(id)?.let {
            return json.decodeFromString(StoryDto.serializer(), it.payloadJson)
        }
        return null
    }

    /**
     * Find any cached story whose tweet list contains a tweet whose URL ends in
     * (or contains) the given tweet ID. Used by the Summarize-a-link feature to
     * resolve a pasted URL to the cluster the backend has already grouped for us.
     */
    suspend fun findStoryByTweetId(tweetId: String): StoryDto? {
        if (tweetId.isBlank()) return null
        val token = "/status/$tweetId"
        val all = db.stories().observeAll().first()
        for (row in all) {
            val story = runCatching { json.decodeFromString<StoryDto>(row.payloadJson) }.getOrNull() ?: continue
            if (story.tweets.any { it.url.contains(token) || it.id == tweetId }) return story
        }
        return null
    }

    /** Summarize an arbitrary tweet list via /api/summarize. */
    suspend fun summarize(tweets: List<ai.aligned.net.dto.TweetInput>) = api.summarize(tweets)

    // ─── Warm-tier blob cache (lists / map / events / categories / history) ─

    suspend fun lists(forceRefresh: Boolean = false): ListsDto =
        warmBlob("lists", forceRefresh, WARM_TTL_MS, ListsDto.serializer()) { api.lists() }

    suspend fun map(forceRefresh: Boolean = false): MapDto =
        warmBlob("map", forceRefresh, WARM_TTL_MS, MapDto.serializer()) { api.map() }

    suspend fun events(forceRefresh: Boolean = false): EventsDto =
        warmBlob("events", forceRefresh, WARM_TTL_MS, EventsDto.serializer()) { api.events() }

    suspend fun categories(forceRefresh: Boolean = false): CategoriesDto =
        warmBlob("categories", forceRefresh, COLD_TTL_MS, CategoriesDto.serializer()) { api.categories() }

    suspend fun history(forceRefresh: Boolean = false): List<HistoryEntryDto> =
        warmBlob(
            kind = "history",
            forceRefresh = forceRefresh,
            ttl = COLD_TTL_MS,
            serializer = ListSerializer(HistoryEntryDto.serializer())
        ) { api.history() }

    private suspend fun <T> warmBlob(
        kind: String,
        forceRefresh: Boolean,
        ttl: Long,
        serializer: kotlinx.serialization.KSerializer<T>,
        fetch: suspend () -> T
    ): T {
        val now = System.currentTimeMillis()
        val cached = db.blobs().get(kind)
        if (!forceRefresh && cached != null && now - cached.fetchedAt < ttl) {
            return json.decodeFromString(serializer, cached.payloadJson)
        }
        return runCatching { fetch() }
            .onSuccess { value ->
                val raw = json.encodeToString(serializer, value)
                db.blobs().upsert(
                    CachedBlob(
                        kind = kind, payloadJson = raw,
                        payloadHash = raw.hashCode().toString(),
                        fetchedAt = now
                    )
                )
            }
            .recover { e ->
                cached?.let { return@recover json.decodeFromString(serializer, it.payloadJson) }
                throw e
            }
            .getOrThrow()
    }

    // ─── Bookmarks ──────────────────────────────────────────────────────────

    fun observeBookmarks(): Flow<List<StoryDto>> =
        db.bookmarks().observeAll().map { rows ->
            rows.mapNotNull { storyOrCached(it.storyId) }
        }

    fun observeIsBookmarked(id: String): Flow<Boolean> = db.bookmarks().observeIsBookmarked(id)

    suspend fun toggleBookmark(id: String): Boolean {
        val now = System.currentTimeMillis()
        return if (db.bookmarks().isBookmarked(id)) {
            db.bookmarks().remove(id); false
        } else {
            db.bookmarks().add(BookmarkRow(storyId = id, savedAt = now)); true
        }
    }

    // ─── Topic pins ─────────────────────────────────────────────────────────

    fun observePinnedTopics(): Flow<List<String>> =
        db.topicPins().observeAll().map { rows -> rows.map { it.categoryId } }

    suspend fun pinnedTopics(): List<String> = db.topicPins().all()

    suspend fun togglePinnedTopic(categoryId: String): Boolean {
        val now = System.currentTimeMillis()
        return if (db.topicPins().isPinned(categoryId)) {
            db.topicPins().remove(categoryId); false
        } else {
            db.topicPins().add(TopicPinRow(categoryId = categoryId, pinnedAt = now)); true
        }
    }
}

data class SyncDiff(val totalStories: Int, val newStoryIds: List<String>)
