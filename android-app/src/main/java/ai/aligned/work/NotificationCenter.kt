package ai.aligned.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ai.aligned.MainActivity
import ai.aligned.R
import ai.aligned.data.AlignedDb
import ai.aligned.data.StoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Channels:
 *  - "topics"   default ON  — user-pinned categories
 *  - "breaking" default OFF — high-engagement first-of-its-kind
 *  - "brief"    default ON  — 08:00 daily summary
 *  - "research" default ON  — research session complete
 *  - "chat"     default OFF
 *  - "system"   low priority
 *
 * "topics" only fires for categories the user has explicitly pinned via the
 * topic-pin store. If the user hasn't pinned anything yet we fall back to firing
 * for the first 3 new stories (so notifications are visible immediately on
 * install, not blocked behind a setup step).
 *
 * Dedupe: by storyId via [NotificationManager] id (hash of the story id).
 */
@Singleton
class NotificationCenter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AlignedDb,
    private val repo: StoryRepository
) {
    private val nm: NotificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    suspend fun onNewStories(newIds: List<String>) {
        val pinned = repo.pinnedTopics().toSet()
        val candidates = if (pinned.isEmpty()) newIds.take(3)
                         else newIds.filter { id -> db.stories().get(id)?.category in pinned }.take(5)
        for (storyId in candidates) {
            val cached = db.stories().get(storyId) ?: continue
            val pi = pendingFor(storyId)
            val n = NotificationCompat.Builder(ctx, "topics")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(cached.headline)
                .setContentText(cached.summary.take(120))
                .setStyle(NotificationCompat.BigTextStyle().bigText(cached.summary))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setGroup("aligned-topics")
                .build()
            nm.notify(abs(storyId.hashCode()), n)
        }
        if (candidates.isNotEmpty()) {
            val summary = NotificationCompat.Builder(ctx, "topics")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ALIGNED")
                .setContentText("${candidates.size} new stories matched your topics")
                .setGroup("aligned-topics")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            nm.notify(0, summary)
        }
    }

    fun onResearchReady(query: String) {
        val pi = PendingIntent.getActivity(
            ctx, RESEARCH_ID,
            Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, "research")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Research ready")
            .setContentText(query)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(RESEARCH_ID, n)
    }

    private fun pendingFor(storyId: String): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            data = android.net.Uri.parse("aligned://story/$storyId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            ctx, abs(storyId.hashCode()), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object { const val RESEARCH_ID = 3131 }
}
