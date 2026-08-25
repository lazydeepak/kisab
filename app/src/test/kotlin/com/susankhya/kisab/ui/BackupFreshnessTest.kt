package com.susankhya.kisab.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFreshnessTest {

    private val now = 1_000_000_000L

    @Test
    fun `no recorded backup is never recent`() {
        assertFalse(BackupFreshness.isRecentBackup(recordedAtMillis = null, nowMillis = now))
    }

    @Test
    fun `backup under five minutes old is recent`() {
        assertTrue(
            BackupFreshness.isRecentBackup(
                recordedAtMillis = now - BackupFreshness.RECENT_WINDOW_MILLIS + 1,
                nowMillis = now
            )
        )
    }

    @Test
    fun `backup exactly five minutes old is recent`() {
        assertTrue(
            BackupFreshness.isRecentBackup(
                recordedAtMillis = now - BackupFreshness.RECENT_WINDOW_MILLIS,
                nowMillis = now
            )
        )
    }

    @Test
    fun `backup older than five minutes is stale`() {
        assertFalse(
            BackupFreshness.isRecentBackup(
                recordedAtMillis = now - BackupFreshness.RECENT_WINDOW_MILLIS - 1,
                nowMillis = now
            )
        )
    }

    @Test
    fun `just-now backup is recent`() {
        assertTrue(BackupFreshness.isRecentBackup(recordedAtMillis = now, nowMillis = now))
    }
}
