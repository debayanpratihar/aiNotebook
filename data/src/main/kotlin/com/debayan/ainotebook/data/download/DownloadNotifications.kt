package com.debayan.ainotebook.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Builds and posts the progress notification for a model download. Uses the platform
 * [Notification.Builder] (minSdk 26, so notification channels are always available) to avoid an
 * extra dependency. Progress updates via [notify] are best-effort.
 */
class DownloadNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun notificationId(modelId: String): Int = modelId.hashCode()

    fun buildNotification(displayName: String, percent: Int, indeterminate: Boolean, verifying: Boolean): Notification {
        ensureChannel()
        return Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(displayName)
            .setContentText(if (verifying) "Verifying…" else "Downloading — $percent%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, indeterminate)
            .build()
    }

    /** Best-effort progress update; ignored if notifications are disabled/not permitted. */
    fun notify(modelId: String, notification: Notification) {
        runCatching { manager.notify(notificationId(modelId), notification) }
    }

    private fun ensureChannel() {
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "model_downloads"
    }
}
