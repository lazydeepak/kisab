package com.susankhya.kisab.ui

/**
 * App-local record of successful export backups, scoped per farm.
 *
 * Used to decide whether the Reset Farm Data flow can skip its backup gate
 * because a recent backup was recorded. The metadata lives outside farm
 * accounting data and only records that an export completed — it does not
 * prove the backup file still physically exists.
 */
interface BackupFreshnessStore {

    /** Epoch millis of the last recorded successful backup for [farmId], or null if none. */
    fun lastSuccessfulBackupAt(farmId: String): Long?

    /** Records a successful backup for [farmId] at [atMillis] (epoch millis). */
    fun recordSuccessfulBackup(farmId: String, atMillis: Long)
}
