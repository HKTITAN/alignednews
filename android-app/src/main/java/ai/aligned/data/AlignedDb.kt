package ai.aligned.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Local cache. Per-concern row with full JSON kept as a blob so we can rehydrate
 * the DTO. Avoids per-field schema churn when the backend shape evolves.
 *
 * `payloadHash` lets us skip writes when the upstream bytes haven't changed
 * (which is common — /api/news returns byte-identical content for ~5 min).
 *
 * Stories are retained for **7 days** (`fetchedAt + 7d`). Older rows are pruned
 * by [StoryDao.prune]. Lists/map/events/categories use **30 minutes** TTL.
 */

const val STORY_TTL_MS: Long          = 7L * 24L * 60L * 60L * 1000L // 7 days
const val DETAIL_TTL_MS: Long         = 7L * 24L * 60L * 60L * 1000L // 7 days
const val WARM_TTL_MS: Long           = 30L * 60L * 1000L            // 30 minutes
const val COLD_TTL_MS: Long           = 6L * 60L * 60L * 1000L       // 6 hours

@Entity(tableName = "stories")
data class CachedStory(
    @PrimaryKey val id: String,
    val category: String,
    val headline: String,
    val summary: String,
    val createdAt: String,
    val updatedAt: String,
    val tweetCount: Int,
    val totalEngagement: Long,
    val payloadJson: String,
    val payloadHash: String,
    val fetchedAt: Long
)

@Entity(tableName = "story_detail")
data class CachedStoryDetail(
    @PrimaryKey val id: String,
    val payloadJson: String,
    val payloadHash: String,
    val fetchedAt: Long
)

@Entity(tableName = "sync_log")
data class SyncLogRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tier: String,
    val ok: Boolean,
    val message: String,
    val tsEpochMs: Long
)

/** Singleton-keyed blobs: one row per kind ("lists", "map", "events", "categories", "history", "settings"). */
@Entity(tableName = "blob_cache")
data class CachedBlob(
    @PrimaryKey val kind: String,
    val payloadJson: String,
    val payloadHash: String,
    val fetchedAt: Long
)

/** Local-only bookmarks (story IDs marked by the user). */
@Entity(tableName = "bookmarks")
data class BookmarkRow(
    @PrimaryKey val storyId: String,
    val savedAt: Long
)

/** Local-only topic pins (category IDs the user has subscribed to). */
@Entity(tableName = "topic_pins")
data class TopicPinRow(
    @PrimaryKey val categoryId: String,
    val pinnedAt: Long
)

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CachedStory>>

    @Query("SELECT * FROM stories WHERE category = :cat ORDER BY createdAt DESC")
    fun observeByCategory(cat: String): Flow<List<CachedStory>>

    @Query("SELECT * FROM stories WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CachedStory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<CachedStory>)

    @Query("SELECT payloadHash FROM stories WHERE id = :id LIMIT 1")
    suspend fun hashOf(id: String): String?

    @Query("DELETE FROM stories WHERE id NOT IN (:keep)")
    suspend fun keepOnly(keep: List<String>)

    /** Prune stories older than `cutoff` (epoch ms). Called from sync workers. */
    @Query("DELETE FROM stories WHERE fetchedAt < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun count(): Int
}

@Dao
interface StoryDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CachedStoryDetail)

    @Query("SELECT * FROM story_detail WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CachedStoryDetail?

    @Query("DELETE FROM story_detail WHERE fetchedAt < :cutoff")
    suspend fun prune(cutoff: Long)
}

@Dao
interface BlobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CachedBlob)

    @Query("SELECT * FROM blob_cache WHERE kind = :kind LIMIT 1")
    suspend fun get(kind: String): CachedBlob?
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<BookmarkRow>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE storyId = :id)")
    fun observeIsBookmarked(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE storyId = :id)")
    suspend fun isBookmarked(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(row: BookmarkRow)

    @Query("DELETE FROM bookmarks WHERE storyId = :id")
    suspend fun remove(id: String)
}

@Dao
interface TopicPinDao {
    @Query("SELECT * FROM topic_pins ORDER BY pinnedAt DESC")
    fun observeAll(): Flow<List<TopicPinRow>>

    @Query("SELECT categoryId FROM topic_pins")
    suspend fun all(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM topic_pins WHERE categoryId = :id)")
    suspend fun isPinned(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(row: TopicPinRow)

    @Query("DELETE FROM topic_pins WHERE categoryId = :id")
    suspend fun remove(id: String)
}

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(row: SyncLogRow)

    @Query("SELECT * FROM sync_log ORDER BY tsEpochMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SyncLogRow>

    @Query("DELETE FROM sync_log WHERE tsEpochMs < :olderThan")
    suspend fun trim(olderThan: Long)
}

@Database(
    entities = [
        CachedStory::class,
        CachedStoryDetail::class,
        SyncLogRow::class,
        CachedBlob::class,
        BookmarkRow::class,
        TopicPinRow::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AlignedDb : RoomDatabase() {
    abstract fun stories(): StoryDao
    abstract fun storyDetail(): StoryDetailDao
    abstract fun syncLog(): SyncLogDao
    abstract fun blobs(): BlobDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun topicPins(): TopicPinDao

    companion object {
        fun open(ctx: Context): AlignedDb =
            Room.databaseBuilder(ctx, AlignedDb::class.java, "aligned.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
