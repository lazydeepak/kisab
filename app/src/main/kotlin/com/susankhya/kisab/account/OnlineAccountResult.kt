package com.susankhya.kisab.account

import com.susankhya.kisab.domain.AccountLink

/**
 * Explicit outcome of [OnlineAccountService.establish] for future UI.
 * Wraps only safe, non-secret details; never exposes tokens or assertions.
 *
 * [Success] means the account was established, session persisted,
 * and AccountLink recorded. [Failure] provides a structured reason
 * and an optional safe detail string for logging/tests.
 */
sealed class OnlineAccountResult {

    /**
     * Account establishment succeeded. [accountId] is the Kisab-owned
     * online account id; [link] reflects the persisted link state.
     */
    data class Success(
        val accountId: String,
        val link: AccountLink.Linked
    ) : OnlineAccountResult()

    /**
     * Account establishment failed. [reason] categorizes the failure;
     * [detail] is a safe, non-secret string for logs/tests only.
     */
    data class Failure(
        val reason: OnlineAccountFailureReason,
        /** Safe, non-secret detail for logs/tests — never tokens or assertions. */
        val detail: String? = null
    ) : OnlineAccountResult()
}

/**
 * Structured failure categories matching [AccountApiFailureKind]
 * plus local persistence failures (session/link write).
 *
 * - [TRANSPORT] — network or unexpected transport error
 * - [INVALID_CREDENTIAL] — provider assertion rejected by backend
 * - [SERVER_REJECTED] — backend accepted transport but rejected request
 * - [INVALID_RESPONSE] — backend response missing required fields
 * - [SESSION_PERSISTENCE_FAILED] — secure session write failed after successful API call
 * - [ACCOUNT_LINK_CONFLICT] — LocalUser already linked to a different account
 * - [ACCOUNT_LINK_PERSISTENCE_FAILED] — AccountLink write failed; session was rolled back
 */
enum class OnlineAccountFailureReason {
    TRANSPORT,
    INVALID_CREDENTIAL,
    SERVER_REJECTED,
    INVALID_RESPONSE,
    SESSION_PERSISTENCE_FAILED,
    ACCOUNT_LINK_CONFLICT,
    ACCOUNT_LINK_PERSISTENCE_FAILED
}
