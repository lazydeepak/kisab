package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFreshnessCheckerTest {

    private class FakeBackupFreshnessStore : BackupFreshnessStore {
        private val records = mutableMapOf<String, Long>()
        override fun lastSuccessfulBackupAt(farmId: String): Long? = records[farmId]
        override fun recordSuccessfulBackup(farmId: String, atMillis: Long) {
            records[farmId] = atMillis
        }
    }

    private class FakeClock(var nowMillis: Long) : Clock {
        override fun nowMillis(): Long = nowMillis
    }

    private val farmA = "farm-aaaa"
    private val farmB = "farm-bbbb"

    @Test
    fun `successful backup records the timestamp for the current farm`() {
        val store = FakeBackupFreshnessStore()
        val clock = FakeClock(1_000_000_000L)
        store.recordSuccessfulBackup(farmA, clock.nowMillis())
        assertEquals(clock.nowMillis(), store.lastSuccessfulBackupAt(farmA))
        assertNull(store.lastSuccessfulBackupAt(farmB))
    }

    @Test
    fun `recent backup for one farm does not count for a different farm`() {
        val store = FakeBackupFreshnessStore()
        val clock = FakeClock(1_000_000_000L)
        store.recordSuccessfulBackup(farmA, clock.nowMillis() - BackupFreshness.RECENT_WINDOW_MILLIS + 1)
        val checker = BackupFreshnessChecker(store, clock)
        assertTrue(checker.isRecent(farmA))
        assertFalse(checker.isRecent(farmB))
    }

    @Test
    fun `no metadata for a farm means not recent`() {
        val checker = BackupFreshnessChecker(FakeBackupFreshnessStore(), FakeClock(1_000_000_000L))
        assertFalse(checker.isRecent(farmA))
    }

    @Test
    fun `stale backup for a farm is not recent`() {
        val store = FakeBackupFreshnessStore()
        val clock = FakeClock(1_000_000_000L)
        store.recordSuccessfulBackup(farmA, clock.nowMillis() - BackupFreshness.RECENT_WINDOW_MILLIS - 1)
        assertFalse(BackupFreshnessChecker(store, clock).isRecent(farmA))
    }

    @Test
    fun `cancelled or failed backup does not update metadata`() {
        val store = FakeBackupFreshnessStore()
        val clock = FakeClock(1_000_000_000L)
        store.recordSuccessfulBackup(farmA, 500_000_000L)
        val flow = ResetFarmFlow { }
        flow.begin()
        flow.proceedFromWarning(recentBackup = false)
        flow.onBackupCancelledOrFailed()
        assertEquals(500_000_000L, store.lastSuccessfulBackupAt(farmA))
    }

    @Test
    fun `manual existing backup acknowledgement does not record a Kisab backup`() {
        val store = FakeBackupFreshnessStore()
        val clock = FakeClock(1_000_000_000L)
        val flow = ResetFarmFlow { }
        flow.begin()
        flow.proceedFromWarning(recentBackup = false)
        flow.acknowledgeExistingBackup()
        assertNull(store.lastSuccessfulBackupAt(farmA))
    }
}
