package ai.aligned.data

import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.StoryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val api: AlignedApi,
    private val db: AlignedDb
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Cache-first stream of stories. Network refresh is triggered via [refresh]. */
    fun observe(): Flow<List<StoryDto>> =
        db.stories().observeAll().map { rows ->
            rows.map { json.decodeFromString<StoryDto>(it.payloadJson) }
        }

    suspend fun refresh(): SyncDiff {
        val resp = api.news()
        val stories = resp.stories
        val rows = stories.map { s ->
            val raw = json.encodeToString(StoryDto.serializer(), s)
            CachedStory(
                id = s.id, category = s.category, headline = s.headline,
                summary = s.summary, createdAt = s.createdAt, updatedAt = s.updatedAt,
                tweetCount = s.tweetCount, totalEngagement = s.totalEngagement,
                payloadJson = raw, payloadHash = raw.hashCode().toString(),
                fetchedAt = System.currentTimeMillis()
            )
        }
        // Compute diff before upsert
        val existingIds = stories.mapNotNull { db.stories().get(it.id)?.id }.toSet()
        val newIds = stories.map { it.id }.toSet() - existingIds
        db.stories().upsert(rows)
        db.stories().keepOnly(stories.map { it.id })
        db.syncLog().insert(SyncLogRow(
            tier = "hot", ok = true,
            message = "${stories.size} stories, ${newIds.size} new",
            tsEpochMs = System.currentTimeMillis()
        ))
        return SyncDiff(totalStories = stories.size, newStoryIds = newIds.toList())
    }

    suspend fun story(id: String): StoryDto {
        db.storyDetail().get(id)?.let {
            return json.decodeFromString(StoryDto.serializer(), it.payloadJson)
        }
        val fetched = api.story(id)
        val raw = json.encodeToString(StoryDto.serializer(), fetched)
        db.storyDetail().upsert(CachedStoryDetail(
            id = id, payloadJson = raw, payloadHash = raw.hashCode().toString(),
            fetchedAt = System.currentTimeMillis()
        ))
        return fetched
    }
}

data class SyncDiff(val totalStories: Int, val newStoryIds: List<String>)
