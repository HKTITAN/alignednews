package ai.aligned.work

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.aligned.MainActivity
import ai.aligned.R
import ai.aligned.net.AlignedApi
import kotlinx.coroutines.*

/**
 * Foreground service that polls /api/research?id=… until status == "complete".
 * Wired in fully at M9. This stub satisfies the manifest declaration.
 */
class ResearchPollService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val api = AlignedApi()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val researchId = intent?.getStringExtra(EXTRA_ID) ?: run { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIF_ID, buildNotification("Researching…"))

        scope.launch {
            while (isActive) {
                val r = runCatching { api.research(researchId) }.getOrNull()
                if (r != null && r.status == "complete") {
                    // M6 will dispatch a "research ready" rich notification.
                    stopSelf()
                    break
                }
                delay(5_000)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, "research")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ALIGNED")
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        const val EXTRA_ID = "research_id"
        const val NOTIF_ID = 4242
    }
}
