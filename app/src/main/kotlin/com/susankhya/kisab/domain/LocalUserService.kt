package com.susankhya.kisab.domain

import java.util.UUID

/**
 * Ensures a single stable [LocalUser] exists and tracks which farms it owns
 * locally. Does not create farms, touch farm accounting data, or participate
 * in backup encode/decode.
 */
class LocalUserService(
    private val store: LocalUserStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { "user-${UUID.randomUUID()}" }
) {

    /**
     * Returns the existing local user, or creates and persists one on first use.
     * Never regenerates an ID once saved.
     */
    fun ensureLocalUser(): LocalUser {
        store.loadUser()?.let { return it }
        val created = LocalUser(userId = idGenerator(), createdAtMillis = clock())
        store.saveUser(created)
        return created
    }

    fun currentUser(): LocalUser? = store.loadUser()

    /**
     * Records that [farmId] is owned by the current local user. Idempotent.
     * Call after create, import/replace, or first-run migration of an existing farm.
     */
    fun associateFarm(farmId: String) {
        val user = ensureLocalUser()
        store.addOwnedFarm(user.userId, farmId)
    }

    /**
     * Drops ownership of [farmId] after the farm itself has been deleted.
     * Does not delete or regenerate the local user.
     */
    fun disassociateFarm(farmId: String) {
        val user = store.loadUser() ?: return
        store.removeOwnedFarm(user.userId, farmId)
    }

    fun ownedFarmIds(): Set<String> {
        val user = store.loadUser() ?: return emptySet()
        return store.ownedFarmIds(user.userId)
    }

    /**
     * First-run / upgrade migration: ensure a local user exists and, if the
     * device already has a current farm, associate that farm without changing
     * its id or data.
     */
    fun migrateExistingInstall(currentFarmId: String?) {
        ensureLocalUser()
        if (currentFarmId != null) {
            associateFarm(currentFarmId)
        }
    }
}
