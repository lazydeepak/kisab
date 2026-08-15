package com.susankhya.kisab.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.susankhya.kisab.R
import com.susankhya.kisab.ui.FarmActivity

/**
 * Single place that posts system notifications. Does not mutate farms or accounts.
 */
class NotificationCoordinator(
    private val context: Context,
    private val preferences: NotificationPreferences,
    private val permissionCheck: () -> Boolean = { NotificationPermission.isGranted(context) }
) {

    /**
     * @return true if a notification was posted
     */
    fun present(message: IncomingPushMessage): Boolean {
        if (!permissionCheck()) return false
        val category = message.type.toCategory()
        if (category != null && !preferences.isCategoryEnabled(category)) return false

        NotificationChannels.ensureCreated(context)
        val channelId = NotificationChannels.channelIdFor(message.type)
        val notificationId = notificationIdFor(message)
        val contentIntent = contentPendingIntent(message.type.deepLink())

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_kisab_logo)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            // Permission revoked between check and post.
            false
        }
    }

    private fun notificationIdFor(message: IncomingPushMessage): Int {
        val key = message.collapseKey ?: message.type.name
        return key.hashCode()
    }

    private fun contentPendingIntent(deepLink: NotificationDeepLink): PendingIntent {
        val intent = Intent(context, FarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_DEEP_LINK, deepLink.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(context, deepLink.ordinal, intent, flags)
    }

    companion object {
        const val EXTRA_NOTIFICATION_DEEP_LINK = "kisab_notification_deep_link"
    }
}
