package com.susankhya.kisab.domain

/**
 * App-local persistence for [LocalUser] and which farms that user owns.
 *
 * Kept outside [FarmStore] / farm backup so a restored farm file does not
 * import or overwrite account identity, and so Reset/Delete farm never
 * regenerates the user.
 */
interface LocalUserStore {
    fun loadUser(): LocalUser?

    fun saveUser(user: LocalUser)

    /** Farm IDs currently associated with [userId] (empty if none). */
    fun ownedFarmIds(userId: String): Set<String>

    fun addOwnedFarm(userId: String, farmId: String)

    fun removeOwnedFarm(userId: String, farmId: String)
}
