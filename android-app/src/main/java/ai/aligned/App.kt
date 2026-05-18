package ai.aligned

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import ai.aligned.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        SyncScheduler.ensureScheduled(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            channel("breaking", R.string.notif_channel_breaking, NotificationManager.IMPORTANCE_HIGH),
            channel("topics",   R.string.notif_channel_topics,   NotificationManager.IMPORTANCE_DEFAULT),
            channel("brief",    R.string.notif_channel_brief,    NotificationManager.IMPORTANCE_DEFAULT),
            channel("research", R.string.notif_channel_research, NotificationManager.IMPORTANCE_HIGH),
            channel("chat",     R.string.notif_channel_chat,     NotificationManager.IMPORTANCE_DEFAULT),
            channel("system",   R.string.notif_channel_system,   NotificationManager.IMPORTANCE_LOW)
        ).forEach(nm::createNotificationChannel)
    }

    private fun channel(id: String, nameRes: Int, importance: Int): NotificationChannel =
        NotificationChannel(id, getString(nameRes), importance)
}
