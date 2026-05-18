package ai.aligned.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.aligned.data.StoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Hot tier (~5 min cadence). Repository handles network + cache + diff.
 * Diffs that produce new story ids feed into [NotificationCenter] for opt-in
 * topic / breaking notifications. (Topic match logic lands in M6.)
 */
@HiltWorker
class HotSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: StoryRepository,
    private val notifications: NotificationCenter
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result =
        runCatching { repo.refresh() }
            .fold(
                onSuccess = { diff ->
                    if (diff.newStoryIds.isNotEmpty()) {
                        notifications.onNewStories(diff.newStoryIds)
                    }
                    Result.success()
                },
                onFailure = { Result.retry() }
            )
}
