package com.susankhya.kisab.notifications

/**
 * Allowed operational push types. Unknown server values must not execute
 * domain actions — see [IncomingPushMessage.parse].
 */
enum class NotificationType {
    APP_UPDATE,
    BACKUP_REMINDER,
    GENERAL_OPERATIONAL_NOTICE;

    companion object {
        fun fromWire(value: String?): NotificationType? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
