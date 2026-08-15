package com.susankhya.kisab.domain

/**
 * Local account-linking contract: query and record a Kisab online account
 * relationship for the offline [LocalUser] without networking.
 *
 * Does not create accounts, authenticate providers, sync farms, or store tokens.
 * Unlink is intentionally not exposed until product policy is defined — clearing
 * a link must not be mistaken for deleting farms or the LocalUser.
 */
class AccountLinkService(
    private val store: AccountLinkStore,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    /** Current link state for [localUserId]; missing persistence = unlinked. */
    fun linkState(localUserId: String): AccountLink {
        require(localUserId.isNotBlank()) { "localUserId is required" }
        return store.load(localUserId) ?: AccountLink.Unlinked(localUserId)
    }

    fun isLinked(localUserId: String): Boolean = linkState(localUserId).isLinked

    /**
     * Records that [localUserId] is linked to Kisab online [accountId].
     *
     * - Same account again → idempotent (keeps original [AccountLink.Linked.linkedAtMillis]).
     * - Different account while already linked → [AccountLinkConflictException]
     *   (no silent switch; merge/transfer is a future sync milestone).
     *
     * Does not mutate LocalUser id, farm ids, or ownership.
     */
    fun linkToAccount(localUserId: String, accountId: String): AccountLink.Linked {
        require(localUserId.isNotBlank()) { "localUserId is required" }
        val normalizedAccount = accountId.trim()
        require(normalizedAccount.isNotBlank()) { "accountId is required" }

        when (val current = linkState(localUserId)) {
            is AccountLink.Linked -> {
                if (current.accountId == normalizedAccount) {
                    return current
                }
                throw AccountLinkConflictException(
                    localUserId = localUserId,
                    existingAccountId = current.accountId,
                    attemptedAccountId = normalizedAccount
                )
            }
            is AccountLink.Unlinked -> {
                val linked = AccountLink.Linked(
                    localUserId = localUserId,
                    accountId = normalizedAccount,
                    linkedAtMillis = clock()
                )
                store.save(linked)
                return linked
            }
        }
    }
}

/**
 * Raised when a LocalUser is already linked to one Kisab account and a different
 * account id is requested. Callers must not overwrite; resolve via future
 * merge/transfer product flow.
 */
class AccountLinkConflictException(
    val localUserId: String,
    val existingAccountId: String,
    val attemptedAccountId: String
) : IllegalStateException(
    "LocalUser $localUserId is already linked to $existingAccountId; " +
        "cannot link to $attemptedAccountId without an explicit transfer decision"
)
