package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.notifications.NotificationCategory
import com.susankhya.kisab.notifications.NotificationPreferences

class SharedPreferencesNotificationPreferences(context: Context) : NotificationPreferences {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isCategoryEnabled(category: NotificationCategory): Boolean =
        prefs.getBoolean(key(category), defaultEnabled(category))

    override fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        prefs.edit().putBoolean(key(category), enabled).commit()
    }

    private fun key(category: NotificationCategory): String = "category_${category.name}"

    private fun defaultEnabled(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.APP_UPDATES -> true
        NotificationCategory.BACKUP_REMINDERS -> true
    }

    companion object {
        private const val PREFS_NAME = "kisab_notification_preferences"
    }
}
