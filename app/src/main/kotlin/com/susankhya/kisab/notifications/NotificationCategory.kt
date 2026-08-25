package com.susankhya.kisab.notifications

/** User-toggleable categories (separate from OS notification permission). */
enum class NotificationCategory {
    APP_UPDATES,
    BACKUP_REMINDERS
}

fun NotificationType.toCategory(): NotificationCategory? = when (this) {
    NotificationType.APP_UPDATE -> NotificationCategory.APP_UPDATES
    NotificationType.BACKUP_REMINDER -> NotificationCategory.BACKUP_REMINDERS
    NotificationType.GENERAL_OPERATIONAL_NOTICE -> NotificationCategory.APP_UPDATES
}
