package com.susankhya.kisab.persistence

import android.content.Context

/**
 * Tracks which local calendar day a private-build expiry warning was last shown
 * so WARNING stage does not interrupt every navigation.
 */
class SharedPreferencesPrivateBuildWarningStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastWarningDayKey(): String? = prefs.getString(KEY_LAST_WARNING_DAY, null)

    fun markWarningShown(dayKey: String) {
        prefs.edit().putString(KEY_LAST_WARNING_DAY, dayKey).commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_private_build_warnings"
        private const val KEY_LAST_WARNING_DAY = "last_warning_day_key"
    }
}
