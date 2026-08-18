package com.susankhya.kisab.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.susankhya.kisab.R

object NotificationChannels {
    const val UPDATES_ID = "kisab_updates"
    const val REMINDERS_ID = "kisab_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val updates = NotificationChannel(
            UPDATES_ID,
            context.getString(R.string.notification_channel_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_updates_description)
        }
        val reminders = NotificationChannel(
            REMINDERS_ID,
            context.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
        }
        manager.createNotificationChannel(updates)
        manager.createNotificationChannel(reminders)
    }

    fun channelIdFor(type: NotificationType): String = when (type) {
        NotificationType.APP_UPDATE -> UPDATES_ID
        NotificationType.BACKUP_REMINDER -> REMINDERS_ID
        NotificationType.GENERAL_OPERATIONAL_NOTICE -> UPDATES_ID
    }
}
