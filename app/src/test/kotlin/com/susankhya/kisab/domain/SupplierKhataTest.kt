package com.susankhya.kisab.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplierKhataTest {
    @Test
    fun partialSupplierPurchaseCreatesTradeStockAndPayableExactlyOnce() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val supplier = service.addParty(farm.id, PartyDraft("Krishna Feed", PartyRole.SUPPLIER))
        val feed = service.addSupply(farm.id, "Feed", ProductUnit.BAG)
        val trade = service.addSupplierPurchase(farm.id, supplier.id, feed.id, BigDecimal("20"), ProductUnit.BAG, 40_000, 15_000, "2026-08-16T08:00:00Z", "Feed")
        val loaded = service.loadFarm(farm.id)!!
        assertEquals(1, loaded.trades.count { it.id == trade.id })
        assertEquals(0, loaded.transactions.size)
        assertEquals(BigDecimal("20"), service.supplyAvailable(farm.id, feed.id))
        assertEquals(25_000, loaded.partyLedger(supplier.id).summary.toPayMinor)
    }

    @Test
    fun supplierPaymentAllocatesOldestFirstAndDoesNotChangeStock() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val supplier = service.addParty(farm.id, PartyDraft("Krishna Feed", PartyRole.SUPPLIER))
        val feed = service.addSupply(farm.id, "Feed", ProductUnit.BAG)
        service.addSupplierPurchase(farm.id, supplier.id, feed.id, BigDecimal("5"), ProductUnit.BAG, 10_000, null, "2026-08-15T08:00:00Z", "Old")
        service.addSupplierPurchase(farm.id, supplier.id, feed.id, BigDecimal("5"), ProductUnit.BAG, 20_000, null, "2026-08-16T08:00:00Z", "New")
        val payments = service.recordSupplierPayment(farm.id, supplier.id, 15_000, "2026-08-16T12:00:00Z")
        assertEquals(10_000, payments[0].amountMinor)
        assertEquals(5_000, payments[1].amountMinor)
        assertEquals(15_000, service.loadFarm(farm.id)!!.partyLedger(supplier.id).summary.toPayMinor)
        assertEquals(BigDecimal("10"), service.supplyAvailable(farm.id, feed.id))
    }

    @Test
    fun overpaymentIsAtomicAndLegacyExpenseRemainsExpenseBacked() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val supplier = service.addParty(farm.id, PartyDraft("Krishna Feed", PartyRole.SUPPLIER))
        val feed = service.addSupply(farm.id, "Feed", ProductUnit.BAG)
        service.addSupplierPurchase(farm.id, supplier.id, feed.id, BigDecimal("20"), ProductUnit.BAG, 40_000, null, "2026-08-16T08:00:00Z", "Feed")
        val before = com.susankhya.kisab.persistence.FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        assertTrue(runCatching { service.recordSupplierPayment(farm.id, supplier.id, 40_001, "2026-08-16T12:00:00Z") }.isFailure)
        assertEquals(before, com.susankhya.kisab.persistence.FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        val old = service.addSupplyPurchase(farm.id, feed.id, BigDecimal("2"), ProductUnit.BAG, 4_000, TransactionCategory.SUPPLIES, "2026-08-16T13:00:00Z", "Legacy")
        val loaded = service.loadFarm(farm.id)!!
        assertEquals(old.id, loaded.supplyPurchaseDetails.first { it.transactionId != null }.transactionId)
        assertEquals(1, loaded.transactions.count { it.id == old.id })
    }
}
