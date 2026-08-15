package com.susankhya.kisab.notifications

/**
 * Conservative in-app destinations for notification taps.
 * No destructive actions.
 */
enum class NotificationDeepLink {
    /** About / private-build update information surface. */
    UPDATE_INFO,
    /** Settings → Data (backup/export). */
    BACKUP_DATA,
    /** Settings → Notifications. */
    NOTIFICATION_SETTINGS
}

fun NotificationType.deepLink(): NotificationDeepLink = when (this) {
    NotificationType.APP_UPDATE -> NotificationDeepLink.UPDATE_INFO
    NotificationType.BACKUP_REMINDER -> NotificationDeepLink.BACKUP_DATA
    NotificationType.GENERAL_OPERATIONAL_NOTICE -> NotificationDeepLink.NOTIFICATION_SETTINGS
}
