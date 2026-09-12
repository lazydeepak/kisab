package com.susankhya.kisab.account

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the in-memory OTP workflow of [FakeAccountApi] against the
 * ADR-0004 lifecycle: email deliverability, single-use, 10-minute expiry,
 * 5-attempt lock, and resend revoking the earlier request.
 */
class FakeAccountApiOtpTest {

    private var now = 1_000_000L
    private val fake = FakeAccountApi(
        handler = { throw AssertionError("no establishAccount expected in this flow") },
        nowMillis = { now },
        onEmailCode = { "654321" }
    )

    private fun assertFailureKind(kind: AccountApiFailureKind, block: suspend () -> Unit) = runTest {
        try {
            block()
            throw AssertionError("expected AccountApiException($kind)")
        } catch (e: AccountApiException) {
            assertEquals(kind, e.kind)
        }
    }

    @Test
    fun requestReturnsRequestIdAndExpiryWithoutLeakingCode() = runTest {
        val requested = fake.requestEmailOtp("farmer@example.com")
        assertTrue(requested.requestId.startsWith("otp-"))
        assertEquals(now + 10 * 60 * 1000L, requested.expiresAtEpochMillis)
        assertEquals("654321", fake.emittedCodes["farmer@example.com"])
        assertFalse(requested.requestId.contains("654321"))
    }

    @Test
    fun blankEmailIsRejected() = assertFailureKind(AccountApiFailureKind.INVALID_CREDENTIAL) {
        fake.requestEmailOtp("   ")
    }

    @Test
    fun wrongCodeFailsUntilCorrectOneSucceeds() = runTest {
        val requested = fake.requestEmailOtp("farmer@example.com")
        assertFailureKind(AccountApiFailureKind.INVALID_CREDENTIAL) {
            fake.verifyEmailOtpAndEstablish(requested.requestId, "000000", "user-1")
        }
        val response = fake.verifyEmailOtpAndEstablish(requested.requestId, "654321", "user-1")
        assertEquals("account-farmer@example.com", response.accountId)
        assertTrue(response.sessionId.startsWith("session-otp-"))
        assertTrue(response.accessToken.startsWith("access-otp-"))
    }

    @Test
    fun codeIsSingleUse() = runTest {
        val requested = fake.requestEmailOtp("farmer@example.com")
        fake.verifyEmailOtpAndEstablish(requested.requestId, "654321", null)
        assertFailureKind(AccountApiFailureKind.INVALID_CREDENTIAL) {
            fake.verifyEmailOtpAndEstablish(requested.requestId, "654321", null)
        }
    }

    @Test
    fun expiredRequestIsRejected() = runTest {
        val requested = fake.requestEmailOtp("farmer@example.com")
        now = requested.expiresAtEpochMillis + 1
        assertFailureKind(AccountApiFailureKind.OTP_EXPIRED) {
            fake.verifyEmailOtpAndEstablish(requested.requestId, "654321", null)
        }
    }

    @Test
    fun fifthWrongAttemptLocksTheRequest() = runTest {
        val requested = fake.requestEmailOtp("farmer@example.com")
        repeat(4) {
            assertFailureKind(AccountApiFailureKind.INVALID_CREDENTIAL) {
                fake.verifyEmailOtpAndEstablish(requested.requestId, "000000", null)
            }
        }
        assertFailureKind(AccountApiFailureKind.OTP_RATE_LIMITED) {
            fake.verifyEmailOtpAndEstablish(requested.requestId, "000000", null)
        }
    }

    @Test
    fun resendForSameEmailRevokesEarlierRequest() = runTest {
        val first = fake.requestEmailOtp("farmer@example.com")
        val second = fake.requestEmailOtp("farmer@example.com")
        assertFalse(first.requestId == second.requestId)
        // Only the newest request is live.
        assertFailureKind(AccountApiFailureKind.INVALID_CREDENTIAL) {
            fake.verifyEmailOtpAndEstablish(first.requestId, "654321", null)
        }
        val response = fake.verifyEmailOtpAndEstablish(second.requestId, "654321", null)
        assertNotNull(response)
    }

    @Test
    fun demoFactoryCompletesFullExchange() = runTest {
        val api = FakeAccountApi.demo(nowMillis = { now })
        val requested = api.requestEmailOtp("demo@example.com")
        val response = api.verifyEmailOtpAndEstablish(requested.requestId, "123456", "user-demo")
        assertEquals("account-demo@example.com", response.accountId)
        assertNotNull(response.accessToken)
    }
}