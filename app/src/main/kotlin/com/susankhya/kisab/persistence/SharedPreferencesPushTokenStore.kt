package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.notifications.PushTokenStore

/**
 * App-local FCM token only. Outside farm store, local user, account link, and backups.
 */
class SharedPreferencesPushTokenStore(context: Context) : PushTokenStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun latestToken(): String? =
        prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    override fun saveToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_TOKEN, trimmed).commit()
    }

    override fun clear() {
        prefs.edit().remove(KEY_TOKEN).commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_push_token"
        private const val KEY_TOKEN = "fcm_token"
    }
}
