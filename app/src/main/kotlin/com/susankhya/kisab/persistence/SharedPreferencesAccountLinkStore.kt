package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.domain.AccountLink
import com.susankhya.kisab.domain.AccountLinkStore

/**
 * SharedPreferences-backed [AccountLinkStore].
 *
 * Prefs file is separate from farm store, local user, and keystore session.
 * Only stores non-secret link metadata (account id + linked-at). Never tokens.
 */
class SharedPreferencesAccountLinkStore(context: Context) : AccountLinkStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(localUserId: String): AccountLink? {
        if (localUserId.isBlank()) return null
        val status = prefs.getString(statusKey(localUserId), null) ?: return null
        return when (status) {
            STATUS_UNLINKED -> AccountLink.Unlinked(localUserId)
            STATUS_LINKED -> {
                val accountId = prefs.getString(accountIdKey(localUserId), null)
                    ?.takeIf { it.isNotBlank() }
                    ?: return AccountLink.Unlinked(localUserId)
                val linkedAt = prefs.getLong(linkedAtKey(localUserId), MISSING)
                if (linkedAt == MISSING) return AccountLink.Unlinked(localUserId)
                AccountLink.Linked(
                    localUserId = localUserId,
                    accountId = accountId,
                    linkedAtMillis = linkedAt
                )
            }
            else -> null
        }
    }

    override fun save(link: AccountLink) {
        val editor = prefs.edit()
        when (link) {
            is AccountLink.Unlinked -> {
                editor.putString(statusKey(link.localUserId), STATUS_UNLINKED)
                    .remove(accountIdKey(link.localUserId))
                    .remove(linkedAtKey(link.localUserId))
            }
            is AccountLink.Linked -> {
                editor.putString(statusKey(link.localUserId), STATUS_LINKED)
                    .putString(accountIdKey(link.localUserId), link.accountId)
                    .putLong(linkedAtKey(link.localUserId), link.linkedAtMillis)
            }
        }
        editor.commit()
    }

    override fun clear(localUserId: String) {
        prefs.edit()
            .remove(statusKey(localUserId))
            .remove(accountIdKey(localUserId))
            .remove(linkedAtKey(localUserId))
            .commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_account_link"
        private const val STATUS_UNLINKED = "unlinked"
        private const val STATUS_LINKED = "linked"
        private const val MISSING = Long.MIN_VALUE

        private fun statusKey(userId: String) = "status_$userId"
        private fun accountIdKey(userId: String) = "account_id_$userId"
        private fun linkedAtKey(userId: String) = "linked_at_$userId"
    }
}
