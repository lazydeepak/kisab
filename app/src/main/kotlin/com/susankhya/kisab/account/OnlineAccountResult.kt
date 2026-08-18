package com.susankhya.kisab.account

import com.susankhya.kisab.domain.AccountLink

/**
 * Explicit outcome of [OnlineAccountService.establish] for future UI.
 * Never wraps raw network exceptions with secrets.
 */
sealed class OnlineAccountResult {

    data class Success(
        val accountId: String,
        val link: AccountLink.Linked
    ) : OnlineAccountResult()

    data class Failure(
        val reason: OnlineAccountFailureReason,
        /** Safe, non-secret detail for logs/tests — never tokens or assertions. */
        val detail: String? = null
    ) : OnlineAccountResult()
}

enum class OnlineAccountFailureReason {
    TRANSPORT,
    INVALID_CREDENTIAL,
    SERVER_REJECTED,
    INVALID_RESPONSE,
    SESSION_PERSISTENCE_FAILED,
    ACCOUNT_LINK_CONFLICT,
    ACCOUNT_LINK_PERSISTENCE_FAILED
}
