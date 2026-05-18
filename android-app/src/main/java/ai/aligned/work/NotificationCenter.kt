package ai.aligned.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ai.aligned.MainActivity
import ai.aligned.R
import ai.aligned.data.AlignedDb
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
 * v0.1: only "topics" fires; topic match is naive (no user pin store yet).
 * Dedupe: by storyId via a tiny rolling buffer in NotificationManager id space.
 */
@Singleton
class NotificationCenter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AlignedDb
) {
    private val nm: NotificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    suspend fun onNewStories(newIds: List<String>) {
        // Limit fan-out to 3 per cycle.
        val toFire = newIds.take(3)
        for (storyId in toFire) {
            val cached = db.stories().get(storyId) ?: continue
            val intent = Intent(ctx, MainActivity::class.java).apply {
                data = android.net.Uri.parse("aligned://story/$storyId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(
                ctx, abs(storyId.hashCode()), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
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
        if (toFire.isNotEmpty()) {
            // Summary notification ties them together on Android <N where group expansion needs it.
            val summary = NotificationCompat.Builder(ctx, "topics")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ALIGNED")
                .setContentText("${toFire.size} new stories matched your topics")
                .setGroup("aligned-topics")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            nm.notify(0, summary)
        }
    }
}
