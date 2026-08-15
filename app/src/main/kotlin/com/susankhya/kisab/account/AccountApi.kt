package com.susankhya.kisab.account

/**
 * Client contract for exchanging a provider credential for a Kisab online
 * account identity and session material. Implementations talk to the Kisab
 * backend (or a test double). No farm sync.
 */
interface AccountApi {

    /**
     * Establishes or resumes a Kisab online account for the authenticated person.
     *
     * @throws AccountApiException on transport, credential, or server failure.
     *         Implementations must not log [EstablishAccountRequest.credential].
     */
    suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse
}

/**
 * @param localUserId optional client context for the backend; not the account id.
 */
data class EstablishAccountRequest(
    val credential: ProviderCredential,
    val localUserId: String? = null
)

/**
 * Minimal successful backend payload required to persist session + AccountLink.
 * No premium/profile/storage fields.
 */
data class EstablishAccountResponse(
    /** Kisab-owned online account id, e.g. `account-<server-id>`. */
    val accountId: String,
    val sessionId: String,
    val accessToken: String,
    val refreshToken: String? = null
) {
    init {
        require(accountId.isNotBlank()) { "accountId is required" }
        require(sessionId.isNotBlank()) { "sessionId is required" }
        require(accessToken.isNotBlank()) { "accessToken is required" }
    }
}

enum class AccountApiFailureKind {
    TRANSPORT,
    INVALID_CREDENTIAL,
    SERVER_REJECTED,
    INVALID_RESPONSE
}

/**
 * Backend/transport failure. [message] must never contain tokens or assertions.
 */
class AccountApiException(
    val kind: AccountApiFailureKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
