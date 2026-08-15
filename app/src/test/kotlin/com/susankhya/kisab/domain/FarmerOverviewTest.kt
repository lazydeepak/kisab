package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmerOverviewTest {
    private val zone = ZoneId.of("Asia/Kathmandu")
    private val now = OffsetDateTime.parse("2026-08-16T10:00:00Z")

    @Test
    fun dailyOverviewSeparatesSalesReceivedExpensesProductionAndReceivable() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        service.addProductionRecord(farm.id, ProductionRecordDraft(milk.id, BigDecimal("69"), ProductUnit.LITRE, "2026-08-16T02:00:00Z"), zone)
        val ram = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        val sale = service.addProductSale(farm.id, ram.id, milk.id, BigDecimal("57"), 9_000, 380_000, "2026-08-16T08:00:00Z")
        service.addSettlement(farm.id, SettlementDraft(sale.id, 100_000, "2026-08-16T12:00:00Z"))
        service.createTransaction(farm.id, FarmTransactionDraft(TransactionType.EXPENSE, TransactionCategory.SUPPLIES, 120_000, "Feed", "2026-08-16T10:00:00Z"))
        service.addProductionAllocation(farm.id, ProductionAllocationDraft(milk.id, BigDecimal("2"), ProductUnit.LITRE, "2026-08-16T11:00:00Z", ProductionAllocationType.HOME_USE), zone)

        val overview = service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily
        assertEquals(513_000L, overview.salesMinor)
        assertEquals(480_000L, overview.moneyReceivedMinor)
        assertEquals(120_000L, overview.expensesMinor)
        assertEquals(33_000L, overview.currentReceivableMinor)
        assertEquals(133_000L, overview.creditSalesMinor)
        assertEquals(BigDecimal("10"), overview.production.single().unexplained)
    }

    @Test
    fun monthlyUsesLocalMonthAndExcludesPreviousMonth() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        service.createTransaction(farm.id, FarmTransactionDraft(TransactionType.EXPENSE, TransactionCategory.SUPPLIES, 100, "August", "2026-08-01T00:00:00Z"))
        service.createTransaction(farm.id, FarmTransactionDraft(TransactionType.EXPENSE, TransactionCategory.SUPPLIES, 200, "July", "2026-07-31T17:00:00Z"))
        service.addProductionRecord(farm.id, ProductionRecordDraft(milk.id, BigDecimal("10"), ProductUnit.LITRE, "2026-08-16T02:00:00Z"), zone)

        val month = service.loadFarm(farm.id)!!.farmerOverview(now, zone).monthly
        assertEquals(100L, month.expensesMinor)
        assertEquals(BigDecimal("10"), month.production.single().quantity)
    }

    @Test
    fun overviewDoesNotPersistSummaryAndSupportsGenericEggsAndSupplies() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val eggs = service.addProduct(farm.id, "Eggs", ProductUnit.PIECE)
        service.addProductionRecord(farm.id, ProductionRecordDraft(eggs.id, BigDecimal("620"), ProductUnit.PIECE, "2026-08-16T04:00:00Z"), zone)
        val supply = service.addSupply(farm.id, "Feed", ProductUnit.BAG)
        service.addSupplyPurchase(farm.id, supply.id, BigDecimal("12"), ProductUnit.BAG, 100, TransactionCategory.SUPPLIES, "2026-08-16T05:00:00Z", "Feed")
        val before = com.susankhya.kisab.persistence.FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val overview = service.loadFarm(farm.id)!!.farmerOverview(now, zone)
        assertEquals(BigDecimal("620"), overview.daily.production.single().quantity)
        assertEquals(BigDecimal("12"), overview.daily.supplies.single().quantity)
        assertTrue(before == com.susankhya.kisab.persistence.FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
    }
}
