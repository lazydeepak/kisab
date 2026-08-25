package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteFarmFlowTest {

    @Test
    fun recentBackupSkipsGateToTyped() {
        val flow = DeleteFarmFlow {}
        flow.begin()
        flow.proceedFromWarning(recentBackup = true)
        assertEquals(DeleteFarmFlow.Stage.TYPED, flow.stage)
    }

    @Test
    fun noRecentOpensGateThenAcknowledge() {
        val flow = DeleteFarmFlow {}
        flow.begin()
        flow.proceedFromWarning(recentBackup = false)
        assertEquals(DeleteFarmFlow.Stage.BACKUP_GATE, flow.stage)
        flow.acknowledgeExistingBackup()
        assertEquals(DeleteFarmFlow.Stage.TYPED, flow.stage)
    }

    @Test
    fun backupSuccessAdvancesFromGate() {
        val flow = DeleteFarmFlow {}
        flow.begin()
        flow.proceedFromWarning(false)
        flow.onBackupSucceeded()
        assertEquals(DeleteFarmFlow.Stage.TYPED, flow.stage)
    }

    @Test
    fun cancelResetsStage() {
        val flow = DeleteFarmFlow {}
        flow.begin()
        flow.cancel()
        assertEquals(DeleteFarmFlow.Stage.NONE, flow.stage)
    }

    @Test
    fun keywordIsCaseInsensitiveAndTrimmed() {
        assertTrue(DeleteFarmFlow.matchesDeleteKeyword("delete"))
        assertTrue(DeleteFarmFlow.matchesDeleteKeyword(" DELETE "))
        assertFalse(DeleteFarmFlow.matchesDeleteKeyword("DEL"))
        assertFalse(DeleteFarmFlow.matchesDeleteKeyword("farm name"))
    }

    @Test
    fun confirmExecutesOnce() {
        var count = 0
        val flow = DeleteFarmFlow { count++ }
        flow.begin()
        flow.proceedFromWarning(true)
        assertTrue(flow.confirm("delete"))
        assertFalse(flow.confirm("delete"))
        assertEquals(1, count)
    }
}
