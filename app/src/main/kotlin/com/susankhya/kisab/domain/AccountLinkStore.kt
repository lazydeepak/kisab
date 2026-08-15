package com.susankhya.kisab.domain

/**
 * App-local persistence for [AccountLink] only.
 *
 * Outside farm payload, farm backup, and secure session storage. Tokens never
 * belong here — use foundation session storage for credentials later.
 */
interface AccountLinkStore {
    /**
     * Returns the persisted link for [localUserId], or null if this user has
     * never been recorded (treated as unlinked by [AccountLinkService]).
     */
    fun load(localUserId: String): AccountLink?

    fun save(link: AccountLink)

    /** Removes link metadata for [localUserId] (does not touch farms or LocalUser). */
    fun clear(localUserId: String)
}
