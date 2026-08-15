package com.susankhya.kisab.domain

/** In-memory [AccountLinkStore] for unit tests. */
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
