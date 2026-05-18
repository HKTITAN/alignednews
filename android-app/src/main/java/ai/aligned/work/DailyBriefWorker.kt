package ai.aligned.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.aligned.MainActivity
import ai.aligned.R
import ai.aligned.data.StoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Fires once per local day around 08:00. Refreshes /api/lists and shows a
 * "today's brief is ready" notification on the "brief" channel.
 */
@HiltWorker
class DailyBriefWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: StoryRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val lists = runCatching { repo.lists(forceRefresh = true) }.getOrElse { return Result.retry() }
        val overview = lists.overview ?: return Result.success()

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pi = PendingIntent.getActivity(
            ctx, BRIEF_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val body = overview.execSummary.take(180).ifBlank { "${lists.groups.size} categories updated" }
        val n = NotificationCompat.Builder(ctx, "brief")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ALIGNED — today's brief")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(BRIEF_ID, n)
        return Result.success()
    }

    companion object { const val BRIEF_ID = 2424 }
}
