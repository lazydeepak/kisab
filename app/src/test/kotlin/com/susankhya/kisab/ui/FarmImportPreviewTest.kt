package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmImportPreviewTest {

    private val exportedAt = OffsetDateTime.parse("2026-08-01T12:00:00Z")

    @Test
    fun newFarmIdIsAddNew() {
        val backup = FarmState(id = "farm-new", name = "Backup Farm", currencyCode = "NPR")
        val preview = FarmImportPreviewFactory.build(backup, localFarm = null, exportedAt = exportedAt)
        assertEquals(FarmImportMode.ADD_NEW, preview.mode)
        assertTrue(preview.isUpdate.not())
        assertEquals("Backup Farm", preview.backupFarmName)
        assertEquals("NPR", preview.backupCurrencyCode)
        assertEquals(exportedAt, preview.backupExportedAt)
        assertNull(preview.localFarmNameIfDifferent)
        assertNull(preview.diffHints)
    }

    @Test
    fun existingFarmIdIsUpdateExisting() {
        val local = FarmState(id = "farm-1", name = "Local Name", currencyCode = "NPR")
        val backup = FarmState(id = "farm-1", name = "Backup Name", currencyCode = "USD")
        val preview = FarmImportPreviewFactory.build(backup, local, exportedAt)
        assertEquals(FarmImportMode.UPDATE_EXISTING, preview.mode)
        assertTrue(preview.isUpdate)
        assertEquals("Backup Name", preview.backupFarmName)
        assertEquals("Local Name", preview.localFarmNameIfDifferent)
        assertTrue(preview.diffHints!!.nameChanged)
        assertTrue(preview.diffHints!!.currencyChanged)
    }

    @Test
    fun sameNameDoesNotSurfaceLocalNameIfDifferent() {
        val local = FarmState(id = "farm-1", name = "Same", currencyCode = "NPR")
        val backup = FarmState(id = "farm-1", name = "Same", currencyCode = "NPR")
        val preview = FarmImportPreviewFactory.build(backup, local, null)
        assertNull(preview.localFarmNameIfDifferent)
        assertFalse(preview.diffHints!!.nameChanged)
        assertFalse(preview.diffHints!!.currencyChanged)
    }

    @Test
    fun recordCountsReflectHighLevelDiff() {
        val local = FarmState(
            id = "farm-1",
            name = "A",
            currencyCode = "NPR",
            entries = mutableListOf(FarmEntry(FarmEntryKind.CROP, "Maize", 1)),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-1",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 100,
                    description = "x",
                    occurredAt = OffsetDateTime.parse("2024-01-01T00:00:00Z")
                )
            )
        )
        val backup = FarmState(id = "farm-1", name = "A", currencyCode = "NPR")
        val preview = FarmImportPreviewFactory.build(backup, local, exportedAt)
        assertEquals(2, preview.diffHints!!.localRecordCount)
        assertEquals(0, preview.diffHints!!.backupRecordCount)
        assertTrue(preview.diffHints!!.recordCountChanged)
    }

    @Test
    fun determinationUsesFarmIdNotName() {
        // Same display name, different ids → still ADD_NEW when local lookup by id is null
        val backup = FarmState(id = "farm-b", name = "Shared Name", currencyCode = "INR")
        val preview = FarmImportPreviewFactory.build(backup, localFarm = null, exportedAt = null)
        assertEquals(FarmImportMode.ADD_NEW, preview.mode)
    }
}
