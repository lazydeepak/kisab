package com.susankhya.kisab.account

/**
 * Client-side state machine for the email OTP sign-in flow (ADR-0004).
 * Owns request state and resend cooldown; delegates code semantics
 * (expiry, single-use, attempt lock) to the backend via [AccountApi].
 *
 * The code itself is never retained here — only the [requestId] and the
 * server-gated [EmailOtpFlowState.AwaitingCode.expiresAtEpochMillis]
 * handed back for display. Never used on the main thread; call sites
 * drive it from a coroutine.
 */
class EmailOtpFlow(
    private val api: AccountApi,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val resendCooldownMillis: Long = DEFAULT_RESEND_COOLDOWN_MILLIS
) {

    private var state: EmailOtpFlowState = EmailOtpFlowState.Idle

    /** Current UI-facing state. Read-only for callers. */
    fun currentState(): EmailOtpFlowState = state

    /** True while a code window is open (between request and verify). */
    fun isAwaitingCode(): Boolean = state is EmailOtpFlowState.AwaitingCode

    /** Earliest epoch millis at which resend is allowed, or null when idle. */
    fun resendAvailableAtEpochMillis(): Long? =
        (state as? EmailOtpFlowState.AwaitingCode)?.cooldownUntilEpochMillis

    /**
     * Requests a code for [email]. Returns the new state on success or an
     * error category. Blank emails fail locally without touching the API.
     */
    suspend fun request(email: String): EmailOtpFlowResult {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            return EmailOtpFlowResult.Error(EmailOtpFlowError.INVALID_CODE)
        }
        state = EmailOtpFlowState.Requesting
        return try {
            val sent = api.requestEmailOtp(trimmed)
            state = awaiting(sent, trimmed)
            EmailOtpFlowResult.Success(state)
        } catch (e: AccountApiException) {
            state = EmailOtpFlowState.Idle
            EmailOtpFlowResult.Error(e.kind.toFlowError())
        } catch (_: Exception) {
            state = EmailOtpFlowState.Idle
            EmailOtpFlowResult.Error(EmailOtpFlowError.CONNECTION)
        }
    }

    /**
     * Re-requests a code for the email already in flight after the cooldown
     * elapses. The earlier request is revoked by the backend.
     */
    suspend fun resend(): EmailOtpFlowResult {
        val awaiting = state as? EmailOtpFlowState.AwaitingCode
            ?: return EmailOtpFlowResult.Error(EmailOtpFlowError.UNAVAILABLE)
        if (nowMillis() < awaiting.cooldownUntilEpochMillis) {
            return EmailOtpFlowResult.Error(EmailOtpFlowError.RESEND_NOT_READY)
        }
        state = awaiting
        return try {
            val sent = api.requestEmailOtp(awaiting.email)
            state = awaiting(sent, awaiting.email)
            EmailOtpFlowResult.Success(state)
        } catch (e: AccountApiException) {
            state = EmailOtpFlowState.Idle
            EmailOtpFlowResult.Error(e.kind.toFlowError())
        } catch (_: Exception) {
            state = EmailOtpFlowState.Idle
            EmailOtpFlowResult.Error(EmailOtpFlowError.CONNECTION)
        }
    }

    /**
     * Verifies the typed [code] against the in-flight request. On success the
     * flow reaches [EmailOtpFlowState.Established] carrying the exchange
     * response — it still must be persisted via
     * [OnlineAccountService.persistEstablishment] before the sign-in counts.
     */
    suspend fun verify(code: String): EmailOtpFlowResult {
        val awaiting = state as? EmailOtpFlowState.AwaitingCode
            ?: return EmailOtpFlowResult.Error(EmailOtpFlowError.UNAVAILABLE)
        state = EmailOtpFlowState.Verifying(awaiting)
        return try {
            val response = api.verifyEmailOtpAndEstablish(
                requestId = awaiting.requestId,
                otp = code,
                localUserId = null
            )
            state = EmailOtpFlowState.Established(response)
            EmailOtpFlowResult.Success(state)
        } catch (e: AccountApiException) {
            val error = e.kind.toFlowError()
            // Wrong code / connection problems keep the same request retriable
            // (attempts are still consumed server-side); terminal reasons close it.
            state = if (error.retriableSameRequest) awaiting else EmailOtpFlowState.Idle
            EmailOtpFlowResult.Error(error)
        } catch (_: Exception) {
            state = awaiting
            EmailOtpFlowResult.Error(EmailOtpFlowError.CONNECTION)
        }
    }

    /** Cancels an in-flight request (dialog dismissed). Never sends traffic. */
    fun reset() {
        state = EmailOtpFlowState.Idle
    }

    private fun awaiting(sent: EmailOtpRequested, email: String): EmailOtpFlowState.AwaitingCode =
        EmailOtpFlowState.AwaitingCode(
            requestId = sent.requestId,
            email = email,
            cooldownUntilEpochMillis = nowMillis() + resendCooldownMillis,
            expiresAtEpochMillis = sent.expiresAtEpochMillis
        )

    private fun AccountApiFailureKind.toFlowError(): EmailOtpFlowError = when (this) {
        AccountApiFailureKind.OTP_EXPIRED -> EmailOtpFlowError.OTP_EXPIRED
        AccountApiFailureKind.OTP_RATE_LIMITED -> EmailOtpFlowError.OTP_RATE_LIMITED
        AccountApiFailureKind.INVALID_CREDENTIAL -> EmailOtpFlowError.INVALID_CODE
        AccountApiFailureKind.TRANSPORT -> EmailOtpFlowError.CONNECTION
        AccountApiFailureKind.SERVER_REJECTED,
        AccountApiFailureKind.INVALID_RESPONSE -> EmailOtpFlowError.UNAVAILABLE
    }

    companion object {
        private const val DEFAULT_RESEND_COOLDOWN_MILLIS = 60_000L
    }
}

/** UI-facing state for the email OTP flow. */
sealed class EmailOtpFlowState {
    data object Idle : EmailOtpFlowState()
    data object Requesting : EmailOtpFlowState()
    data class AwaitingCode(
        val requestId: String,
        val email: String,
        /** Earliest epoch millis at which resend is allowed (client-enforced). */
        val cooldownUntilEpochMillis: Long,
        /** Server-gated deadline; after this the code can no longer be verified. */
        val expiresAtEpochMillis: Long
    ) : EmailOtpFlowState()

    data class Verifying(val awaiting: AwaitingCode) : EmailOtpFlowState()
    data class Established(val response: EstablishAccountResponse) : EmailOtpFlowState()
}

/** Result of a [EmailOtpFlow] call — [Success] carries the new state. */
sealed class EmailOtpFlowResult {
    data class Success(val state: EmailOtpFlowState) : EmailOtpFlowResult()
    data class Error(val error: EmailOtpFlowError) : EmailOtpFlowResult()
}

/** UI error categories; map to localized strings, never to raw API messages. */
enum class EmailOtpFlowError {
    /** Code did not match (or the request no longer exists). */
    INVALID_CODE,
    /** Code window closed (10-minute deadline). */
    OTP_EXPIRED,
    /** Attempt limit exhausted — start again. */
    OTP_RATE_LIMITED,
    /** Resend cooldown still active (60 s). */
    RESEND_NOT_READY,
    /** Transport/network problem. */
    CONNECTION,
    /** Service not available (e.g. backend not configured in this build). */
    UNAVAILABLE;

    /** True when the same [EmailOtpFlowState.AwaitingCode] may retry this error. */
    val retriableSameRequest: Boolean
        get() = this == INVALID_CODE || this == CONNECTION
}