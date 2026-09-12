package com.susankhya.kisab.account

/**
 * Client contract for exchanging a provider credential for a Kisab online
 * account identity and session material. Implementations talk to the Kisab
 * backend (or a test double). No farm sync.
 *
 * ## Email OTP (ADR-0004)
 * The email flow is a two-step exchange:
 * 1. [requestEmailOtp] asks the backend to email a one-time code to [email];
 *    only a [requestId] + server expiry are returned — never the code.
 * 2. [verifyEmailOtpAndEstablish] exchanges a `requestId` + user-supplied
 *    OTP for the same response shape as [establishAccount].
 * Lifecycle is server-gated (10-minute single-use code, 5-attempt lock,
 * resend cooldown) but the client enforces the resend cooldown itself.
 */
interface AccountApi {

    /**
     * Establishes or resumes a Kisab online account for the authenticated person.
     *
     * @throws AccountApiException on transport, credential, or server failure.
     *         Implementations must not log [EstablishAccountRequest.credential].
     */
    suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse

    /**
     * Requests a one-time code be emailed to [email].
     *
     * The returned [EmailOtpRequested] carries a server-gated [EmailOtpRequested.requestId]
     * and the backend's [EmailOtpRequested.expiresAtEpochMillis]. The code itself is never
     * returned. [email] must not be logged; implementations must fail with
     * [AccountApiFailureKind.INVALID_CREDENTIAL] for blank emails.
     */
    suspend fun requestEmailOtp(email: String): EmailOtpRequested

    /**
     * Verifies the emailed OTP for [requestId] and, when valid, establishes the
     * Kisab account + session for [localUserId] in one exchange.
     *
     * @param otp the 6-digit code typed by the person; never stored client-side.
     * @throws AccountApiException with [AccountApiFailureKind.OTP_EXPIRED],
     *         [AccountApiFailureKind.OTP_RATE_LIMITED], or
     *         [AccountApiFailureKind.INVALID_CREDENTIAL] for OTP problems;
     *         transport/server kinds otherwise.
     */
    suspend fun verifyEmailOtpAndEstablish(
        requestId: String,
        otp: String,
        localUserId: String? = null
    ): EstablishAccountResponse
}

/**
 * @param localUserId optional client context for the backend; not the account id.
 */
data class EstablishAccountRequest(
    val credential: ProviderCredential,
    val localUserId: String? = null
)

/**
 * Acknowledgement that an OTP email was sent, with the server-gated request
 * reference and expiry. Does not carry the code.
 */
data class EmailOtpRequested(
    /** Server-gated reference for [AccountApi.verifyEmailOtpAndEstablish]. */
    val requestId: String,
    /** Server deadline in epoch millis; after this the request is unusable. */
    val expiresAtEpochMillis: Long
) {
    init {
        require(requestId.isNotBlank()) { "requestId is required" }
        require(expiresAtEpochMillis > 0L) { "expiresAtEpochMillis must be positive" }
    }
}

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
    INVALID_RESPONSE,
    /** [AccountApi.verifyEmailOtpAndEstablish] — the OTP request expired (ADR-0004). */
    OTP_EXPIRED,
    /** [AccountApi.verifyEmailOtpAndEstablish] — attempt limit exhausted (ADR-0004). */
    OTP_RATE_LIMITED
}

/**
 * Backend/transport failure. [message] must never contain tokens or assertions.
 */
class AccountApiException(
    val kind: AccountApiFailureKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Placeholder [AccountApi] for builds where the online service is not yet
 * configured. Every call fails with [AccountApiFailureKind.SERVER_REJECTED]
 * so the UI can surface a clear “not available yet” state instead of a
 * phantom failure category. Never carries secrets.
 */
object UnavailableAccountApi : AccountApi {
    private const val MESSAGE = "online account service is not configured in this build"

    override suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse {
        throw AccountApiException(AccountApiFailureKind.SERVER_REJECTED, MESSAGE)
    }

    override suspend fun requestEmailOtp(email: String): EmailOtpRequested {
        throw AccountApiException(AccountApiFailureKind.SERVER_REJECTED, MESSAGE)
    }

    override suspend fun verifyEmailOtpAndEstablish(
        requestId: String,
        otp: String,
        localUserId: String?
    ): EstablishAccountResponse {
        throw AccountApiException(AccountApiFailureKind.SERVER_REJECTED, MESSAGE)
    }
}