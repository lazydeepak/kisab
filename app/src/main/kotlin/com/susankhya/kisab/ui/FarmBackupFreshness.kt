package com.susankhya.kisab.ui

/** Testable time source so UI code does not bury System.currentTimeMillis() in dialogs. */
fun interface Clock {
    fun nowMillis(): Long
}

/**
 * Recent-backup rule for the Reset Farm Data flow.
 *
 * A recorded backup counts as recent when its completion age is at most
 * [RECENT_WINDOW_MILLIS] (5 minutes). The boundary is deterministic: exactly
 * 5 minutes old is recent; older, or no record at all, is not.
 */
object BackupFreshness {
    const val RECENT_WINDOW_MILLIS = 5L * 60L * 1000L

    fun isRecentBackup(recordedAtMillis: Long?, nowMillis: Long): Boolean {
        if (recordedAtMillis == null) return false
        return nowMillis - recordedAtMillis <= RECENT_WINDOW_MILLIS
    }
}

/**
 * Combines the per-farm [BackupFreshnessStore] with a [Clock] so the reset
 * flow can ask "is there a recent backup for this specific farm?".
 */
class BackupFreshnessChecker(
    private val store: BackupFreshnessStore,
    private val clock: Clock
) {
    fun isRecent(farmId: String): Boolean {
        val recordedAt = store.lastSuccessfulBackupAt(farmId) ?: return false
        return BackupFreshness.isRecentBackup(recordedAt, clock.nowMillis())
    }
}
