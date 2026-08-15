package com.susankhya.kisab.notifications

/**
 * Installation/device FCM token storage — not LocalUser or account identity.
 * Must never appear in farm backups.
 */
interface PushTokenStore {
    fun latestToken(): String?
    fun saveToken(token: String)
    fun clear()
}
