package com.example.mediabrowser.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mediabrowser.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "downloads_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.download_notification_channel_desc)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun notificationIdFor(downloadId: Long): Int = NOTIFICATION_ID_BASE + downloadId.toInt()

    fun showProgress(downloadId: Long, fileName: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.download_in_progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notify(downloadId, notification)
    }

    fun showCompleted(downloadId: Long, fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.download_complete))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        notify(downloadId, notification)
    }

    fun showFailed(downloadId: Long, fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.download_failed))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        notify(downloadId, notification)
    }

    private fun notify(downloadId: Long, notification: android.app.Notification) {
        runCatching {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(notificationIdFor(downloadId), notification)
        }
    }
}
