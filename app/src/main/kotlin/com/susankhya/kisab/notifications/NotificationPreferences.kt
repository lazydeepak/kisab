package com.susankhya.kisab.notifications

/**
 * App-local category preferences. Orthogonal to OS [POST_NOTIFICATIONS] permission.
 */
interface NotificationPreferences {
    fun isCategoryEnabled(category: NotificationCategory): Boolean
    fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean)
}
