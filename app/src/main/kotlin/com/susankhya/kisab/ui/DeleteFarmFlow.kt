package com.susankhya.kisab.ui

/**
 * Guarded Delete Farm flow (mirrors [ResetFarmFlow] gates with keyword DELETE).
 *
 * Delete only executes after:
 * 1. warning continue,
 * 2. recent backup / successful backup path / acknowledge existing backup,
 * 3. typed DELETE (case-insensitive, trimmed).
 */
class DeleteFarmFlow(private val executeDelete: () -> Unit) {

    enum class Stage { NONE, WARNING, BACKUP_GATE, TYPED }

    var stage: Stage = Stage.NONE
        private set

    fun begin() {
        stage = Stage.WARNING
    }

    fun cancel() {
        stage = Stage.NONE
    }

    fun proceedFromWarning(recentBackup: Boolean) {
        if (stage != Stage.WARNING) return
        stage = if (recentBackup) Stage.TYPED else Stage.BACKUP_GATE
    }

    fun onBackupSucceeded() {
        if (stage == Stage.BACKUP_GATE) stage = Stage.TYPED
    }

    fun onBackupCancelledOrFailed() {
        // stays on BACKUP_GATE
    }

    fun acknowledgeExistingBackup() {
        if (stage == Stage.BACKUP_GATE) stage = Stage.TYPED
    }

    fun canType(text: String): Boolean =
        stage == Stage.TYPED && matchesDeleteKeyword(text)

    fun confirm(text: String): Boolean {
        if (!canType(text)) return false
        stage = Stage.NONE
        executeDelete()
        return true
    }

    companion object {
        const val DELETE_KEYWORD = "DELETE"

        fun matchesDeleteKeyword(text: String): Boolean =
            text.trim().equals(DELETE_KEYWORD, ignoreCase = true)
    }
}
