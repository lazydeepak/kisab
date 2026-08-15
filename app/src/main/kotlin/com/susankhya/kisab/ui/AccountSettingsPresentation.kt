package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.AccountLink

/**
 * Farmer-facing Account section presentation derived from [AccountLink] (and
 * optional session presence). Does not expose tokens, LocalUser ids, or raw
 * account ids in normal UI copy.
 */
enum class AccountConnectionStatus {
    /** LocalUser present, no Kisab online account link. */
    LOCAL_ONLY,

    /** [AccountLink.Linked] — independent of whether a secure session exists. */
    CONNECTED
}

data class AccountSettingsUiState(
    val status: AccountConnectionStatus,
    /**
     * When linked and [hasActiveSession] is known false, secondary “sign-in
     * required” may be shown. Null session knowledge → never show (avoid guessing).
     */
    val showSignInRequired: Boolean
) {
    val isConnected: Boolean get() = status == AccountConnectionStatus.CONNECTED
}

object AccountSettingsPresentation {

    /**
     * @param link current account link for the LocalUser (unlinked or linked)
     * @param hasActiveSession true/false if session was read successfully; null if unknown
     */
    fun uiState(
        link: AccountLink,
        hasActiveSession: Boolean? = null
    ): AccountSettingsUiState {
        return when (link) {
            is AccountLink.Unlinked -> AccountSettingsUiState(
                status = AccountConnectionStatus.LOCAL_ONLY,
                showSignInRequired = false
            )
            is AccountLink.Linked -> AccountSettingsUiState(
                status = AccountConnectionStatus.CONNECTED,
                showSignInRequired = hasActiveSession == false
            )
        }
    }

    /**
     * Support-friendly short form of a Kisab account id for rare display needs.
     * Not used in the default Settings Account section.
     */
    fun shortenedAccountId(accountId: String): String {
        val trimmed = accountId.trim()
        if (trimmed.length <= 12) return trimmed
        return trimmed.take(8) + "…" + trimmed.takeLast(4)
    }
}
