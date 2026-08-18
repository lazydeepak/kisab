package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.release.PrivateBuildClockStore

/**
 * Persists the greatest observed wall-clock time for private-build expiry
 * evaluation. Separate from farm data and account identity.
 */
class SharedPreferencesPrivateBuildClockStore(context: Context) : PrivateBuildClockStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun greatestObservedEpochMillis(): Long? {
        val value = prefs.getLong(KEY_GREATEST_OBSERVED, MISSING)
        return value.takeIf { it != MISSING }
    }

    override fun recordObservedEpochMillis(epochMillis: Long) {
        val current = prefs.getLong(KEY_GREATEST_OBSERVED, MISSING)
        if (current == MISSING || epochMillis > current) {
            prefs.edit().putLong(KEY_GREATEST_OBSERVED, epochMillis).commit()
        }
    }

    companion object {
        private const val PREFS_NAME = "kisab_private_build_clock"
        private const val KEY_GREATEST_OBSERVED = "greatest_observed_epoch_millis"
        private const val MISSING = Long.MIN_VALUE
    }
}
