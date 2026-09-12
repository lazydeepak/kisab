package com.susankhya.kisab.account

/**
 * In-memory [AccountApi] for unit tests. Never used for production traffic.
 * Does not log credentials.
 *
 * ## OTP workflow
 * Mails a deterministic 6-digit code (see [onEmailCode]); `requestId` is a
 * `otp-<n>` sequence. Server-gated lifecycle mirroring ADR-0004:
 *  - 10-minute expiry ([otpLifetimeMillis])
 *  - single-use code; re-requesting for the same email revokes the old one
 *  - 5-attempt lock — 4 wrong attempts leave it active, the 5th wrong attempt
 *    invalidates the request, and later calls report [AccountApiFailureKind.OTP_RATE_LIMITED]
 *  - a blank email fails with [AccountApiFailureKind.INVALID_CREDENTIAL]
 */
class FakeAccountApi(
    private val handler: suspend (EstablishAccountRequest) -> EstablishAccountResponse,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val otpLifetimeMillis: Long = 10 * 60 * 1000L,
    private val maxAttempts: Int = OTP_MAX_ATTEMPTS,
    private val onEmailCode: (email: String) -> String = { "123456" }
) : AccountApi {

    var lastRequest: EstablishAccountRequest? = null
        private set

    /** The last emailed code per email address — used by tests to read “the email”. */
    val emittedCodes = mutableMapOf<String, String>()

    private var nextRequestSequence = 1
    private val pending = mutableMapOf<String, OtpRequest>()

    private data class OtpRequest(
        val email: String,
        val code: String,
        val expiresAt: Long,
        var attemptsRemaining: Int
    )

    override suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse {
        lastRequest = request
        return handler(request)
    }

    override suspend fun requestEmailOtp(email: String): EmailOtpRequested {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            throw AccountApiException(AccountApiFailureKind.INVALID_CREDENTIAL, "email is required")
        }
        val code = onEmailCode(trimmed)
        emittedCodes[trimmed] = code
        // Re-requesting for the same email revokes any outstanding request.
        pending.keys.removeAll { pending[it]?.email == trimmed }
        val requestId = "otp-${nextRequestSequence++}"
        val request = OtpRequest(
            email = trimmed,
            code = code,
            expiresAt = nowMillis() + otpLifetimeMillis,
            attemptsRemaining = maxAttempts
        )
        pending[requestId] = request
        return EmailOtpRequested(
            requestId = requestId,
            expiresAtEpochMillis = request.expiresAt
        )
    }

    override suspend fun verifyEmailOtpAndEstablish(
        requestId: String,
        otp: String,
        localUserId: String?
    ): EstablishAccountResponse {
        val request = pending[requestId]
            ?: throw AccountApiException(
                AccountApiFailureKind.INVALID_CREDENTIAL,
                "unknown or already-used OTP request"
            )

        if (nowMillis() > request.expiresAt) {
            pending.remove(requestId)
            throw AccountApiException(AccountApiFailureKind.OTP_EXPIRED, "OTP request expired")
        }
        if (request.attemptsRemaining <= 0) {
            throw AccountApiException(AccountApiFailureKind.OTP_RATE_LIMITED, "OTP attempt limit exhausted")
        }

        if (otp.trim() != request.code) {
            request.attemptsRemaining -= 1
            if (request.attemptsRemaining <= 0) {
                pending.remove(requestId)
                throw AccountApiException(
                    AccountApiFailureKind.OTP_RATE_LIMITED,
                    "OTP attempt limit exhausted"
                )
            }
            throw AccountApiException(
                AccountApiFailureKind.INVALID_CREDENTIAL,
                "incorrect OTP"
            )
        }

        // Single-use: consume before creating the account response.
        pending.remove(requestId)
        return EstablishAccountResponse(
            accountId = "account-${request.email}",
            sessionId = "session-otp-${nextRequestSequence}",
            accessToken = "access-otp-${nextRequestSequence}",
            refreshToken = "refresh-otp-${nextRequestSequence}"
        )
    }

    companion object {
        private const val OTP_MAX_ATTEMPTS = 5

        fun success(
            accountId: String = "account-test-1",
            sessionId: String = "session-test-1",
            accessToken: String = "access-test-1",
            refreshToken: String? = "refresh-test-1"
        ): FakeAccountApi {
            val response = EstablishAccountResponse(
                accountId = accountId,
                sessionId = sessionId,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
            return FakeAccountApi(handler = { response })
        }

        fun failing(kind: AccountApiFailureKind, message: String = kind.name): FakeAccountApi =
            FakeAccountApi(handler = { throw AccountApiException(kind, message) })

        /**
         * Debug-build demo double: email OTP works end-to-end (code `123456`)
         * and any provider credential establishes a deterministic account.
         * Never used in release builds.
         */
        fun demo(nowMillis: () -> Long = { System.currentTimeMillis() }): FakeAccountApi =
            FakeAccountApi(
                handler = {
                    EstablishAccountResponse(
                        accountId = "account-demo",
                        sessionId = "session-demo",
                        accessToken = "access-demo",
                        refreshToken = "refresh-demo"
                    )
                },
                nowMillis = nowMillis
            )
    }
}