package com.susankhya.kisab.domain

/**
 * In-memory [AccountLinkStore] for unit tests.
 *
 * Backed by a [linkedMapOf] preserving insertion order. Not thread-safe;
 * intended for single-threaded test use via [runTest].
 */
class InMemoryAccountLinkStore : AccountLinkStore {
    private val byUser = linkedMapOf<String, AccountLink>()

    override fun load(localUserId: String): AccountLink? = byUser[localUserId]

    override fun save(link: AccountLink) {
        byUser[link.localUserId] = link
    }

    override fun clear(localUserId: String) {
        byUser.remove(localUserId)
    }
}
