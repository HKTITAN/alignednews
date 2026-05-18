package ai.aligned.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Local cache. One row per story; full JSON kept as a blob so we can rehydrate
 * the DTO. Avoids per-field schema churn when the backend shape evolves.
 *
 * `payloadHash` lets us skip writes when the upstream bytes haven't changed
 * (which is common — /api/news returns byte-identical content for ~5 min).
 */

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
}

@Dao
interface StoryDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CachedStoryDetail)

    @Query("SELECT * FROM story_detail WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CachedStoryDetail?
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
    entities = [CachedStory::class, CachedStoryDetail::class, SyncLogRow::class],
    version = 1,
    exportSchema = false
)
abstract class AlignedDb : RoomDatabase() {
    abstract fun stories(): StoryDao
    abstract fun storyDetail(): StoryDetailDao
    abstract fun syncLog(): SyncLogDao

    companion object {
        fun open(ctx: Context): AlignedDb =
            Room.databaseBuilder(ctx, AlignedDb::class.java, "aligned.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
