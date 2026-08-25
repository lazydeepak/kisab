package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.ui.BackupFreshnessStore

/**
 * SharedPreferences-backed [BackupFreshnessStore].
 *
 * Follows the app's preference pattern (named prefs file, synchronous commit).
 * One key per farm keeps the metadata scoped by `farmId` so a recent backup
 * for one farm never counts for another.
 */
class SharedPreferencesBackupFreshnessStore(context: Context) : BackupFreshnessStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun lastSuccessfulBackupAt(farmId: String): Long? =
        prefs.getLong(keyFor(farmId), MISSING).takeIf { it != MISSING }

    override fun recordSuccessfulBackup(farmId: String, atMillis: Long) {
        prefs.edit().putLong(keyFor(farmId), atMillis).commit()
    }

    override fun clearFarm(farmId: String) {
        prefs.edit().remove(keyFor(farmId)).commit()
    }

    private fun keyFor(farmId: String): String = KEY_PREFIX + farmId

    companion object {
        private const val PREFS_NAME = "kisab_backup_freshness"
        private const val KEY_PREFIX = "last_successful_backup_"
        private const val MISSING = -1L
    }
}
