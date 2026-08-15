package com.susankhya.kisab.ui

/**
 * App-owned decision state for the guarded Reset Farm Data flow.
 *
 * The activity drives stage transitions from dialog actions and the Storage
 * Access Framework export result; this object owns the gating rules so they
 * can be tested independently of Android dialog/SAF plumbing.
 *
 * Reset only executes after every gate passes:
 * 1. the user continues past the warning,
 * 2. a backup path reports success (or the user acknowledges an existing
 *    backup), and
 * 3. the user types the reset keyword exactly (case-insensitive, trimmed).
 */
class ResetFarmFlow(private val executeReset: () -> Unit) {

    enum class Stage { NONE, WARNING, BACKUP_GATE, TYPED }

    var stage: Stage = Stage.NONE
        private set

    /** Enters the flow from the Danger Zone action. */
    fun begin() {
        stage = Stage.WARNING
    }

    /** Cancel at any stage: nothing is mutated. */
    fun cancel() {
        stage = Stage.NONE
    }

    /** User confirmed the warning; the backup gate opens next. */
    fun proceedFromWarning() {
        if (stage == Stage.WARNING) stage = Stage.BACKUP_GATE
    }

    /** The existing export/backup flow reported success. */
    fun onBackupSucceeded() {
        if (stage == Stage.BACKUP_GATE) stage = Stage.TYPED
    }

    /** Export was cancelled or failed: reset cannot proceed from this gate. */
    fun onBackupCancelledOrFailed() {
        // Deliberately stays on BACKUP_GATE: the user may retry the backup
        // path or acknowledge an existing backup on a fresh pass.
    }

    /** User explicitly acknowledges an existing external backup. */
    fun acknowledgeExistingBackup() {
        if (stage == Stage.BACKUP_GATE) stage = Stage.TYPED
    }

    /** Enables the final Reset button: stage is typed-confirmation and text matches. */
    fun canType(text: String): Boolean =
        stage == Stage.TYPED && matchesResetKeyword(text)

    /**
     * Attempts to execute the reset. Returns true only when every gate has
     * passed and the typed text matches; the reset callback then runs once.
     */
    fun confirm(text: String): Boolean {
        if (!canType(text)) return false
        stage = Stage.NONE
        executeReset()
        return true
    }

    companion object {
        const val RESET_KEYWORD = "RESET"

        fun matchesResetKeyword(text: String): Boolean =
            text.trim().equals(RESET_KEYWORD, ignoreCase = true)
    }
}
