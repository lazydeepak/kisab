package com.susankhya.kisab.domain

/**
 * Relationship between a stable offline [LocalUser] and a Kisab-owned online
 * account. Distinct from auth providers (Google/Apple/email) and from
 * [com.susankhya.kisab.session.KisabSession] credentials.
 *
 * Default is [Unlinked]: full offline use with no network dependency.
 * [Linked] stores only the Kisab `accountId` (never provider UIDs or tokens).
 */
sealed class AccountLink {
    abstract val localUserId: String

    data class Unlinked(override val localUserId: String) : AccountLink()

    data class Linked(
        override val localUserId: String,
        /** Kisab-owned online account id, e.g. `account-<server-stable-id>`. */
        val accountId: String,
        val linkedAtMillis: Long
    ) : AccountLink() {
        init {
            require(accountId.isNotBlank()) { "accountId is required when linked" }
        }
    }

    val isLinked: Boolean get() = this is Linked

    val accountIdOrNull: String?
        get() = (this as? Linked)?.accountId
}
