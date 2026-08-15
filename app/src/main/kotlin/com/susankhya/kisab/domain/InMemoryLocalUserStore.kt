package com.susankhya.kisab.domain

/** In-memory [LocalUserStore] for unit tests. */
class InMemoryLocalUserStore : LocalUserStore {
    private var user: LocalUser? = null
    private val ownedByUser = linkedMapOf<String, LinkedHashSet<String>>()

    override fun loadUser(): LocalUser? = user

    override fun saveUser(user: LocalUser) {
        this.user = user
        ownedByUser.putIfAbsent(user.userId, linkedSetOf())
    }

    override fun ownedFarmIds(userId: String): Set<String> =
        ownedByUser[userId]?.toSet() ?: emptySet()

    override fun addOwnedFarm(userId: String, farmId: String) {
        if (user?.userId != userId) return
        ownedByUser.getOrPut(userId) { linkedSetOf() }.add(farmId)
    }

    override fun removeOwnedFarm(userId: String, farmId: String) {
        ownedByUser[userId]?.remove(farmId)
    }
}
