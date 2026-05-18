package ai.aligned.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.aligned.data.StoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Warm tier (~30 min cadence). Refreshes lists/map/events caches so they're
 * fresh when the user opens those tabs. Categories refresh once per 6h via the
 * cold path (handled implicitly by the repo TTL).
 */
@HiltWorker
class WarmSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: StoryRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result =
        runCatching {
            // forceRefresh = true to bypass TTL since this is the scheduled refresh.
            repo.lists(forceRefresh = true)
            repo.map(forceRefresh = true)
            repo.events(forceRefresh = true)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
}
