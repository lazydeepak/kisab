package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmState
import java.time.OffsetDateTime

/**
 * Pre-confirmation import intent derived from backup farm id vs local store.
 * Storage semantics are unchanged: same id → update that farm only; new id → add.
 */
enum class FarmImportMode {
    /** Backup farm id already exists locally — replace that farm's contents only. */
    UPDATE_EXISTING,

    /** Backup farm id is not on this device — add as another farm. */
    ADD_NEW
}

/**
 * High-level, farmer-facing differences between backup and the matching local farm.
 * Empty for [FarmImportMode.ADD_NEW].
 */
data class FarmImportDiffHints(
    val nameChanged: Boolean,
    val currencyChanged: Boolean,
    val localRecordCount: Int,
    val backupRecordCount: Int
) {
    val recordCountChanged: Boolean get() = localRecordCount != backupRecordCount
}

/**
 * Everything the confirmation dialog needs before a write.
 * Does not include raw farm UUIDs for display.
 */
data class FarmImportPreview(
    val mode: FarmImportMode,
    val backupFarmName: String,
    val backupCurrencyCode: String,
    val backupEntryCount: Int,
    val backupTransactionCount: Int,
    val backupExportedAt: OffsetDateTime?,
    /** Local farm name when updating and names differ; null if same or add-new. */
    val localFarmNameIfDifferent: String?,
    val diffHints: FarmImportDiffHints?
) {
    val isUpdate: Boolean get() = mode == FarmImportMode.UPDATE_EXISTING
}

object FarmImportPreviewFactory {

    fun recordCount(farm: FarmState): Int =
        farm.entries.size + farm.transactions.size + farm.parties.size +
            farm.trades.size + farm.settlements.size

    /**
     * @param backupFarm decoded farm from the backup envelope
     * @param localFarm existing farm with the same id, or null if none
     * @param exportedAt envelope export timestamp when present
     */
    fun build(
        backupFarm: FarmState,
        localFarm: FarmState?,
        exportedAt: OffsetDateTime?
    ): FarmImportPreview {
        if (localFarm == null) {
            return FarmImportPreview(
                mode = FarmImportMode.ADD_NEW,
                backupFarmName = backupFarm.name,
                backupCurrencyCode = backupFarm.currencyCode,
                backupEntryCount = backupFarm.entries.size,
                backupTransactionCount = backupFarm.transactions.size,
                backupExportedAt = exportedAt,
                localFarmNameIfDifferent = null,
                diffHints = null
            )
        }
        val nameChanged = localFarm.name != backupFarm.name
        return FarmImportPreview(
            mode = FarmImportMode.UPDATE_EXISTING,
            backupFarmName = backupFarm.name,
            backupCurrencyCode = backupFarm.currencyCode,
            backupEntryCount = backupFarm.entries.size,
            backupTransactionCount = backupFarm.transactions.size,
            backupExportedAt = exportedAt,
            localFarmNameIfDifferent = if (nameChanged) localFarm.name else null,
            diffHints = FarmImportDiffHints(
                nameChanged = nameChanged,
                currencyChanged = localFarm.currencyCode != backupFarm.currencyCode,
                localRecordCount = recordCount(localFarm),
                backupRecordCount = recordCount(backupFarm)
            )
        )
    }
}
