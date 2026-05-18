package ai.aligned.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object SyncScheduler {

    /** Call once from App.onCreate after notification channels are made. */
    fun ensureScheduled(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        val netConnected = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Hot — 15 min minimum on Android (OS floor for PeriodicWorkRequest)
        val hot = PeriodicWorkRequestBuilder<HotSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(netConnected)
            .build()
        wm.enqueueUniquePeriodicWork("hot-sync", ExistingPeriodicWorkPolicy.KEEP, hot)

        // Warm — every 30 min, refreshes lists/map/events caches
        val warm = PeriodicWorkRequestBuilder<WarmSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(netConnected)
            .build()
        wm.enqueueUniquePeriodicWork("warm-sync", ExistingPeriodicWorkPolicy.KEEP, warm)

        // Daily brief — fires once every 24h, aimed at 08:00 local.
        // The OS won't honor an exact time but the initial delay aligns the next run.
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis
        val brief = PeriodicWorkRequestBuilder<DailyBriefWorker>(24, TimeUnit.HOURS)
            .setConstraints(netConnected)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork("daily-brief", ExistingPeriodicWorkPolicy.UPDATE, brief)
    }
}
