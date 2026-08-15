package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetFarmFlowTest {

    private fun trackingFlow(): Pair<ResetFarmFlow, MutableList<Int>> {
        val executions = mutableListOf<Int>()
        val flow = ResetFarmFlow { executions.add(1) }
        return flow to executions
    }

    @Test
    fun `cancel at warning performs no reset`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        assertEquals(ResetFarmFlow.Stage.WARNING, flow.stage)
        flow.cancel()
        assertEquals(ResetFarmFlow.Stage.NONE, flow.stage)
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `reset cannot execute before gates are passed`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        assertFalse(flow.confirm("RESET"))
        assertEquals(ResetFarmFlow.Stage.WARNING, flow.stage)
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `export cancelled blocks typed confirmation`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        assertEquals(ResetFarmFlow.Stage.BACKUP_GATE, flow.stage)
        flow.onBackupCancelledOrFailed()
        assertFalse(flow.canType("RESET"))
        assertFalse(flow.confirm("RESET"))
        assertEquals(ResetFarmFlow.Stage.BACKUP_GATE, flow.stage)
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `export failure blocks typed confirmation`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.onBackupCancelledOrFailed()
        assertFalse(flow.canType("RESET"))
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `successful backup unlocks typed confirmation`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.onBackupSucceeded()
        assertEquals(ResetFarmFlow.Stage.TYPED, flow.stage)
        assertTrue(flow.canType("RESET"))
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `existing backup unlocks typed confirmation`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.acknowledgeExistingBackup()
        assertEquals(ResetFarmFlow.Stage.TYPED, flow.stage)
        assertTrue(flow.canType("RESET"))
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `incorrect text keeps reset disabled and does not execute`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.acknowledgeExistingBackup()
        assertFalse(flow.canType("RESETX"))
        assertFalse(flow.canType(""))
        assertFalse(flow.confirm("RESETX"))
        assertEquals(ResetFarmFlow.Stage.TYPED, flow.stage)
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `reset keyword matching is case-insensitive and trimmed`() {
        assertTrue(ResetFarmFlow.matchesResetKeyword("RESET"))
        assertTrue(ResetFarmFlow.matchesResetKeyword("reset"))
        assertTrue(ResetFarmFlow.matchesResetKeyword("ReSeT"))
        assertTrue(ResetFarmFlow.matchesResetKeyword("  RESET  "))
        assertFalse(ResetFarmFlow.matchesResetKeyword("RESETT"))
        assertFalse(ResetFarmFlow.matchesResetKeyword(""))
    }

    @Test
    fun `cancel at final confirmation performs no reset`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.onBackupSucceeded()
        flow.cancel()
        assertEquals(ResetFarmFlow.Stage.NONE, flow.stage)
        assertFalse(flow.confirm("RESET"))
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `final confirmation executes reset exactly once`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.acknowledgeExistingBackup()
        assertTrue(flow.confirm("reset"))
        assertEquals(1, executions.size)
        assertEquals(ResetFarmFlow.Stage.NONE, flow.stage)
    }

    @Test
    fun `confirm after execution does not run reset again`() {
        val (flow, executions) = trackingFlow()
        flow.begin()
        flow.proceedFromWarning()
        flow.onBackupSucceeded()
        assertTrue(flow.confirm("RESET"))
        assertFalse(flow.confirm("RESET"))
        assertEquals(1, executions.size)
    }
}
