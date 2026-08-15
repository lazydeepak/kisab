package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmSupplyTest {
    @Test
    fun purchaseAddsOneExpenseAndStockThenUsageDerivesRemaining() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val supply = service.addSupply(farm.id, "Feed", ProductUnit.BAG)

        val transaction = service.addSupplyPurchase(
            farm.id, supply.id, BigDecimal("12"), ProductUnit.BAG, 24_000,
            TransactionCategory.FEED, "2026-08-16T08:00:00Z", "Feed purchase"
        )
        assertEquals(1, service.loadFarm(farm.id)!!.transactions.size)
        assertEquals(transaction.id, service.loadFarm(farm.id)!!.supplyPurchaseDetails.single().transactionId)
        assertEquals(BigDecimal("12"), service.supplyAvailable(farm.id, supply.id))

        service.addSupplyUsage(
            farm.id, SupplyUsageDraft(supply.id, BigDecimal("3"), ProductUnit.BAG, "2026-08-16T18:00:00Z")
        )
        assertEquals(BigDecimal("9"), service.supplyAvailable(farm.id, supply.id))
    }

    @Test
    fun usageAboveAvailableIsRejectedWithoutMutation() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val supply = service.addSupply(farm.id, "Calcium", ProductUnit.BOTTLE)
        service.addSupplyPurchase(
            farm.id, supply.id, BigDecimal("4"), ProductUnit.BOTTLE, 8_000,
            TransactionCategory.SUPPLIES, "2026-08-16T08:00:00Z", "Calcium"
        )
        service.addSupplyUsage(
            farm.id, SupplyUsageDraft(supply.id, BigDecimal("3"), ProductUnit.BOTTLE, "2026-08-16T18:00:00Z")
        )
        val before = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)

        val result = runCatching {
            service.addSupplyUsage(
                farm.id, SupplyUsageDraft(supply.id, BigDecimal("2"), ProductUnit.BOTTLE, "2026-08-16T19:00:00Z")
            )
        }
        assertTrue(result.isFailure)
        assertEquals(before, FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(BigDecimal("1"), service.supplyAvailable(farm.id, supply.id))
    }

    @Test
    fun purchasesRemainFinanciallyExactAndGenericUnitsRoundTrip() {
        val service = FarmSliceService()
        val farmA = service.createFarm("Farm A")
        val supply = service.addSupply(farmA.id, "Medicine", ProductUnit.BOTTLE)
        service.addSupplyPurchase(
            farmA.id, supply.id, BigDecimal("2.5"), ProductUnit.BOTTLE, 12_345,
            TransactionCategory.SUPPLIES, "2026-08-16T08:00:00Z", "Medicine"
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farmA.id)!!))
        val farmB = service.createFarm("Farm B")

        assertEquals(12_345, decoded.transactions.single().amountMinor)
        assertEquals(BigDecimal("2.5"), decoded.supplyPurchaseDetails.single().quantity)
        assertEquals(BigDecimal("2.5"), decoded.supplyQuantityAvailable(supply.id))
        assertTrue(service.supplies(farmB.id).isEmpty())
    }

    @Test
    fun preSupplyFarmLoadsWithEmptySupplyCollections() {
        val legacy = "7\u001Ffarm-1\u001FLegacy\u001F\u001FNPR\u001F\u001F\u001F\u001F\u001F\u001F"
        val decoded = FarmPersistenceCodec.decode(legacy)
        assertTrue(decoded.supplies.isEmpty())
        assertTrue(decoded.supplyPurchaseDetails.isEmpty())
        assertTrue(decoded.supplyUsages.isEmpty())
    }
}
