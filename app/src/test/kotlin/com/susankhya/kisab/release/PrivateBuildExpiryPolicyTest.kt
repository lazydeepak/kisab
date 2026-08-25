package com.susankhya.kisab.release

import com.susankhya.kisab.ui.Clock
import com.susankhya.kisab.ui.PrivateBuildExpiryPresentation
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateBuildExpiryPolicyTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val expiresAt = 2_000_000_000_000L

    @Test
    fun moreThanFourteenDaysIsValid() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - 15 * day)
        assertEquals(PrivateBuildAccessStage.VALID, snap.stage)
        assertEquals(15L, snap.daysRemaining)
        assertTrue(snap.mutationsAllowed)
        assertTrue(snap.backupAllowed)
        assertTrue(snap.importAllowed)
    }

    @Test
    fun exactlyFourteenDaysIsWarning() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - 14 * day)
        assertEquals(PrivateBuildAccessStage.WARNING, snap.stage)
        assertEquals(14L, snap.daysRemaining)
        assertTrue(snap.mutationsAllowed)
    }

    @Test
    fun exactlyThreeDaysIsCritical() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - 3 * day)
        assertEquals(PrivateBuildAccessStage.CRITICAL, snap.stage)
        assertEquals(3L, snap.daysRemaining)
        assertTrue(snap.mutationsAllowed)
    }

    @Test
    fun oneFullDayRemainingIsCritical() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - day)
        assertEquals(PrivateBuildAccessStage.CRITICAL, snap.stage)
        assertEquals(1L, snap.daysRemaining)
        assertTrue(snap.mutationsAllowed)
    }

    @Test
    fun lessThanOneDayRemainingStillCriticalNotExpired() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - 1)
        assertEquals(PrivateBuildAccessStage.CRITICAL, snap.stage)
        assertEquals(0L, snap.daysRemaining)
        assertTrue(snap.mutationsAllowed)
        assertTrue(snap.stage != PrivateBuildAccessStage.EXPIRED)
    }

    @Test
    fun atExpiryIsExpired() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt)
        assertEquals(PrivateBuildAccessStage.EXPIRED, snap.stage)
        assertFalse(snap.mutationsAllowed)
        assertFalse(snap.importAllowed)
        assertTrue(snap.backupAllowed)
        assertTrue(snap.viewAllowed)
    }

    @Test
    fun afterExpiryIsExpired() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt + day)
        assertEquals(PrivateBuildAccessStage.EXPIRED, snap.stage)
        assertFalse(snap.mutationsAllowed)
        assertTrue(snap.backupAllowed)
    }

    @Test
    fun disabledNeverExpires() {
        val snap = PrivateBuildExpiryPolicy.evaluate(false, expiresAt, expiresAt + 1000 * day)
        assertEquals(PrivateBuildAccessStage.VALID, snap.stage)
        assertTrue(snap.mutationsAllowed)
        assertFalse(snap.enabled)
    }

    @Test
    fun warningUiOncePerDayHint() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - 10 * day)
        val first = PrivateBuildExpiryPresentation.uiHints(snap, warningAlreadyShownToday = false)
        assertTrue(first.shouldShowStartupDialog)
        val second = PrivateBuildExpiryPresentation.uiHints(snap, warningAlreadyShownToday = true)
        assertFalse(second.shouldShowStartupDialog)
        assertFalse(first.showPersistentBanner)
    }

    @Test
    fun expiredShowsPersistentBanner() {
        val snap = PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt)
        val hints = PrivateBuildExpiryPresentation.uiHints(snap, false)
        assertTrue(hints.showPersistentBanner)
        assertEquals(PrivateBuildExpiryPresentation.MessageKind.EXPIRED_BANNER, hints.kind)
    }

    @Test
    fun clockRollbackUsesGreatestObservedFloor() {
        val store = InMemoryClockStore()
        val clock = MutableClock(expiresAt - 20 * day)
        val gate = PrivateBuildExpiryGate(
            enabled = true,
            expiresAtEpochMillis = expiresAt,
            deviceClock = clock,
            clockStore = store
        )
        assertEquals(PrivateBuildAccessStage.VALID, gate.snapshot().stage)
        // Advance near expiry warning window
        clock.now = expiresAt - 5 * day
        assertEquals(PrivateBuildAccessStage.WARNING, gate.snapshot().stage)
        // Roll device clock far backward
        clock.now = expiresAt - 100 * day
        val afterRollback = gate.snapshot()
        assertEquals(PrivateBuildAccessStage.WARNING, afterRollback.stage)
        assertTrue(afterRollback.evaluationEpochMillis >= expiresAt - 5 * day)
    }

    @Test
    fun forwardTimeUpdatesFloor() {
        val store = InMemoryClockStore()
        val clock = MutableClock(1_000L)
        val gate = PrivateBuildExpiryGate(true, expiresAt, clock, store)
        gate.snapshot()
        assertEquals(1_000L, store.greatestObservedEpochMillis())
        clock.now = 2_000L
        gate.snapshot()
        assertEquals(2_000L, store.greatestObservedEpochMillis())
    }

    private class MutableClock(var now: Long) : Clock {
        override fun nowMillis(): Long = now
    }

    private class InMemoryClockStore : PrivateBuildClockStore {
        private var greatest: Long? = null
        override fun greatestObservedEpochMillis(): Long? = greatest
        override fun recordObservedEpochMillis(epochMillis: Long) {
            val g = greatest
            if (g == null || epochMillis > g) greatest = epochMillis
        }
    }
}
