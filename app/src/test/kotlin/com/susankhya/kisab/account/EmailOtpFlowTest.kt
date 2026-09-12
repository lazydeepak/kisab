package com.susankhya.kisab.account

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [EmailOtpFlow] state transitions: request/resend/verify,
 * resend cooldown, error mapping, and the retriable-same-request rule
 * (only terminal errors close the code window).
 */
class EmailOtpFlowTest {

    private var now = 1_000_000L
    private val RESEND_COOLDOWN = 60_000L

    private fun api(): FakeAccountApi = FakeAccountApi(
        handler = { EstablishAccountResponse("account-farmer@example.com", "session-1", "access-1") },
        nowMillis = { now },
        onEmailCode = { "654321" }
    )

    private fun flow(api: AccountApi = api()) =
        EmailOtpFlow(api, nowMillis = { now }, resendCooldownMillis = RESEND_COOLDOWN)

    /** Double whose OTP request path fails with [kind] but otherwise behaves. */
    private class RequestOtpErrorApi(private val kind: AccountApiFailureKind) : AccountApi {
        override suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse =
            throw AccountApiException(kind, "injected ${kind.name}")

        override suspend fun requestEmailOtp(email: String): EmailOtpRequested =
            throw AccountApiException(kind, "injected ${kind.name}")

        override suspend fun verifyEmailOtpAndEstablish(
            requestId: String,
            otp: String,
            localUserId: String?
        ): EstablishAccountResponse =
            throw AccountApiException(kind, "injected ${kind.name}")
    }

    /** Double whose OTP verify path fails with [kind] after a real request succeeded. */
    private class VerifyOtpErrorApi(
        private val delegate: FakeAccountApi,
        private val kind: AccountApiFailureKind
    ) : AccountApi {
        override suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse =
            delegate.establishAccount(request)

        override suspend fun requestEmailOtp(email: String): EmailOtpRequested =
            delegate.requestEmailOtp(email)

        override suspend fun verifyEmailOtpAndEstablish(
            requestId: String,
            otp: String,
            localUserId: String?
        ): EstablishAccountResponse =
            throw AccountApiException(kind, "injected ${kind.name}")
    }

    private fun EmailOtpFlowResult.awaiting(): EmailOtpFlowState.AwaitingCode {
        assertTrue(this is EmailOtpFlowResult.Success)
        val state = (this as EmailOtpFlowResult.Success).state
        assertTrue(state is EmailOtpFlowState.AwaitingCode)
        return state as EmailOtpFlowState.AwaitingCode
    }

    @Test
    fun requestMovesToAwaitingWithCooldownAndServerExpiry() = runTest {
        val f = flow()
        val awaiting = f.request("farmer@example.com").awaiting()
        assertEquals("farmer@example.com", awaiting.email)
        assertEquals(now + RESEND_COOLDOWN, awaiting.cooldownUntilEpochMillis)
        assertEquals(now + 10 * 60 * 1000L, awaiting.expiresAtEpochMillis)
        assertTrue(f.isAwaitingCode())
    }

    @Test
    fun blankEmailFailsLocallyWithoutTouchingApi() = runTest {
        val f = flow()
        val result = f.request("  ")
        assertEquals(EmailOtpFlowResult.Error(EmailOtpFlowError.INVALID_CODE), result)
        assertEquals(EmailOtpFlowState.Idle, f.currentState())
    }

    @Test
    fun resendBeforeCooldownIsRejected() = runTest {
        val f = flow()
        f.request("farmer@example.com")
        now += 10_000L
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.RESEND_NOT_READY),
            f.resend()
        )
    }

    @Test
    fun resendAfterCooldownIssuesNewRequest() = runTest {
        val f = flow()
        val first = f.request("farmer@example.com").awaiting()
        now += RESEND_COOLDOWN + 1
        val second = f.resend().awaiting()
        assertTrue(second.requestId != first.requestId)
        assertEquals(now + RESEND_COOLDOWN, second.cooldownUntilEpochMillis)
    }

    @Test
    fun verifyWithCorrectCodeReachesEstablished() = runTest {
        val f = flow()
        f.request("farmer@example.com")
        val result = f.verify("654321")
        assertTrue(result is EmailOtpFlowResult.Success)
        val state = (result as EmailOtpFlowResult.Success).state
        assertTrue(state is EmailOtpFlowState.Established)
        assertEquals("account-farmer@example.com", (state as EmailOtpFlowState.Established).response.accountId)
        assertTrue(f.currentState() is EmailOtpFlowState.Established)
    }

    @Test
    fun wrongCodeKeepsTheRequestRetriable() = runTest {
        val f = flow()
        val awaiting = f.request("farmer@example.com").awaiting()
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.INVALID_CODE),
            f.verify("000000")
        )
        assertTrue(f.currentState() is EmailOtpFlowState.AwaitingCode)
        // The same request can still verify successfully with the right code.
        val ok = f.verify("654321")
        assertTrue(ok is EmailOtpFlowResult.Success)
    }

    @Test
    fun connectionFailureOnVerifyKeepsRequestRetriable() = runTest {
        val api = VerifyOtpErrorApi(api(), AccountApiFailureKind.TRANSPORT)
        val f = flow(api)
        f.request("farmer@example.com")
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.CONNECTION),
            f.verify("654321")
        )
        assertTrue(f.currentState() is EmailOtpFlowState.AwaitingCode)
    }

    @Test
    fun expiredCodeClosesTheWindow() = runTest {
        val f = flow()
        f.request("farmer@example.com")
        now += 10 * 60 * 1000L + 1
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.OTP_EXPIRED),
            f.verify("654321")
        )
        assertEquals(EmailOtpFlowState.Idle, f.currentState())
    }

    @Test
    fun rateLimitClosesTheWindow() = runTest {
        val f = flow()
        f.request("farmer@example.com")
        repeat(4) { f.verify("000000") }
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.OTP_RATE_LIMITED),
            f.verify("000000")
        )
        assertEquals(EmailOtpFlowState.Idle, f.currentState())
    }

    @Test
    fun requestTransportFailureReportsConnectionAndIdles() = runTest {
        val api = RequestOtpErrorApi(AccountApiFailureKind.TRANSPORT)
        val f = flow(api)
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.CONNECTION),
            f.request("farmer@example.com")
        )
        assertEquals(EmailOtpFlowState.Idle, f.currentState())
    }

    @Test
    fun serverRejectedOnRequestReportsUnavailable() = runTest {
        val api = RequestOtpErrorApi(AccountApiFailureKind.SERVER_REJECTED)
        val f = flow(api)
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.UNAVAILABLE),
            f.request("farmer@example.com")
        )
        assertEquals(EmailOtpFlowState.Idle, f.currentState())
    }

    @Test
    fun verifyWithoutActiveRequestIsUnavailable() = runTest {
        val f = flow()
        assertEquals(
            EmailOtpFlowResult.Error(EmailOtpFlowError.UNAVAILABLE),
            f.verify("654321")
        )
    }

    @Test
    fun resetCancelsInFlightRequest() = runTest {
        val f = flow()
        f.request("farmer@example.com")
        f.reset()
        assertTrue(!f.isAwaitingCode())
        assertEquals(EmailOtpFlowState.Idle, f.currentState())
    }
}