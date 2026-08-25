package com.susankhya.kisab.domain

/**
 * Pure helpers for Farm Management list/selection policies (UI-testable).
 */
object FarmManagement {

    /**
     * Farms to show: intersection of persisted [farmIds] and local-user
     * [ownedFarmIds], in store enumeration order. Stale ownership IDs that are
     * not persisted are omitted (never crash).
     */
    fun visibleFarmIds(persistedFarmIds: List<String>, ownedFarmIds: Set<String>): List<String> {
        if (ownedFarmIds.isEmpty()) {
            // Fresh ownership not yet associated: still show persisted farms.
            return persistedFarmIds
        }
        return persistedFarmIds.filter { it in ownedFarmIds }
    }

    /**
     * After deleting [deletedFarmId], which farm should become current?
     * - null if none remain
     * - first remaining id in [remainingFarmIds] order if the deleted farm was active
     * - otherwise keep [previousCurrentId] if it still exists
     */
    fun nextCurrentFarmIdAfterDelete(
        deletedFarmId: String,
        previousCurrentId: String?,
        remainingFarmIds: List<String>
    ): String? {
        if (remainingFarmIds.isEmpty()) return null
        if (previousCurrentId != null &&
            previousCurrentId != deletedFarmId &&
            previousCurrentId in remainingFarmIds
        ) {
            return previousCurrentId
        }
        return remainingFarmIds.first()
    }
}
