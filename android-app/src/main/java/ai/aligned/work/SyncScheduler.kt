package ai.aligned.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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

        // Warm + cold scaffolds — real workers land in M5
        // Left intentionally empty so we don't burn user battery before they're useful.
    }
}
