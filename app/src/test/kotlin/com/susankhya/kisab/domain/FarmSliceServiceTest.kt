package com.susankhya.kisab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FarmSliceServiceTest {
    private val service = FarmSliceService()

    @Test
    fun createsFarmAddsEntryRecordsTransactionAndSummarizesBalance() {
        val farm = service.createFarm("Demo Farm")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 3))
        service.recordTransaction(farm.id, FarmTransaction("Feed purchase", -50))
        service.recordTransaction(farm.id, FarmTransaction("Egg sale", 80))

        val summary = service.summary(farm.id)

        assertEquals(1, summary.entryCount)
        assertEquals(2, summary.transactionCount)
        assertEquals(30, summary.balance)
    }

    @Test
    fun createFarmRequiresAName() {
        try {
            service.createFarm("   ")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Farm name is required", exception.message)
        }
    }
}
