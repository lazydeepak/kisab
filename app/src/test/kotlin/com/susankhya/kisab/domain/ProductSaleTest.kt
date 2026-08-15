package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSaleTest {

    @Test
    fun decimalQuantityAndRateDeriveExactMinorTotal() {
        val detail = ProductSaleDetail(
            tradeId = "trade-1",
            productId = "product-milk",
            quantity = BigDecimal("2.5"),
            unit = ProductUnit.LITRE,
            rateMinor = 9_000
        )

        assertEquals(22_500, detail.totalMinor())
    }

    @Test
    fun productSaleCreatesTradeDetailAndOpeningSettlementAtomically() {
        val service = FarmSliceService()
        val farm = service.createFarm("Test farm")
        val customer = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        val product = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)

        val trade = service.addProductSale(
            farmId = farm.id,
            partyId = customer.id,
            productId = product.id,
            quantity = BigDecimal("2"),
            rateMinor = 9_000,
            initialPaymentMinor = 100,
            occurredAt = "2026-08-16T08:00:00Z"
        )
        val persisted = service.loadFarm(farm.id)!!

        assertEquals(18_000, trade.totalMinor)
        assertEquals(1, persisted.productSaleDetails.size)
        assertEquals(trade.id, persisted.productSaleDetails.single().tradeId)
        assertEquals(100, persisted.settlements.single().amountMinor)
        assertEquals(17_900, service.partyLedgerSummary(farm.id, customer.id).toReceiveMinor)
    }

    @Test
    fun customerPaymentAllocatesOldestSalesFirstAndExactFullPaymentSettlesAll() {
        val service = FarmSliceService()
        val farm = service.createFarm("Test farm")
        val customer = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        val product = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        val first = service.addProductSale(
            farm.id, customer.id, product.id, BigDecimal("2"), 9_000, null, "2026-08-10T08:00:00Z"
        )
        val second = service.addProductSale(
            farm.id, customer.id, product.id, BigDecimal("3"), 9_000, null, "2026-08-11T08:00:00Z"
        )

        val allocations = service.recordCustomerPayment(
            farm.id, customer.id, 27_000, "2026-08-16T18:00:00Z"
        )

        assertEquals(listOf(first.id, second.id), allocations.map { it.tradeId })
        assertEquals(18_000, allocations[0].amountMinor)
        assertEquals(9_000, allocations[1].amountMinor)
        assertEquals(18_000, service.partyLedgerSummary(farm.id, customer.id).toReceiveMinor)
    }

    @Test
    fun customerPaymentSplitsAcrossOldestOutstandingSalesWithStableTieBreak() {
        val service = FarmSliceService()
        val farm = service.createFarm("Test farm")
        val customer = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        val product = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        val laterId = service.addProductSale(
            farm.id, customer.id, product.id, BigDecimal("1"), 9_000, null, "2026-08-10T08:00:00Z"
        )
        val earlierId = service.addProductSale(
            farm.id, customer.id, product.id, BigDecimal("1"), 9_000, null, "2026-08-10T08:00:00Z"
        )

        val allocations = service.recordCustomerPayment(
            farm.id, customer.id, 10_000, "2026-08-16T18:00:00Z"
        )

        val expectedOrder = listOf(earlierId.id, laterId.id).sorted()
        assertEquals(expectedOrder, allocations.map { it.tradeId })
        assertEquals(9_000, allocations.first().amountMinor)
        assertEquals(1_000, allocations.last().amountMinor)
        assertEquals(8_000, service.partyLedgerSummary(farm.id, customer.id).toReceiveMinor)
    }

    @Test
    fun overpaymentAndNoOutstandingPaymentDoNotMutateFarm() {
        val service = FarmSliceService()
        val farm = service.createFarm("Test farm")
        val customer = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        val product = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        service.addProductSale(
            farm.id, customer.id, product.id, BigDecimal("1"), 9_000, null, "2026-08-10T08:00:00Z"
        )
        val before = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)

        val overpayment = runCatching {
            service.recordCustomerPayment(farm.id, customer.id, 9_001, "2026-08-16T18:00:00Z")
        }.exceptionOrNull()
        assertTrue(overpayment is IllegalArgumentException)
        assertEquals(before, FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))

        service.recordCustomerPayment(farm.id, customer.id, 9_000, "2026-08-16T18:00:00Z")
        val noOutstanding = runCatching {
            service.recordCustomerPayment(farm.id, customer.id, 1, "2026-08-16T19:00:00Z")
        }.exceptionOrNull()
        assertTrue(noOutstanding is IllegalArgumentException)
    }

    @Test
    fun productSaleRoundTripsAndProductsAreFarmScoped() {
        val service = FarmSliceService()
        val farmA = service.createFarm("Farm A")
        val product = service.addProduct(farmA.id, "Milk", ProductUnit.LITRE)
        val customer = service.addParty(farmA.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        service.addProductSale(
            farmA.id, customer.id, product.id, BigDecimal("0.75"), 9_000, null, "2026-08-16T08:00:00Z"
        )
        val farmB = service.createFarm("Farm B")

        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farmA.id)!!))
        assertEquals("0.75", decoded.productSaleDetails.single().quantity.toPlainString())
        assertEquals(1, decoded.products.size)
        assertTrue(service.products(farmB.id).isEmpty())
    }

    @Test
    fun existingV6FarmDecodesWithEmptyProductCollections() {
        val legacy = "6\u001Ffarm-1\u001FLegacy\u001F\u001FNPR\u001F\u001F\u001F\u001F"

        val decoded = FarmPersistenceCodec.decode(legacy)

        assertTrue(decoded.products.isEmpty())
        assertTrue(decoded.productSaleDetails.isEmpty())
    }
}
