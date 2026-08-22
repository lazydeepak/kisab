package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * M11: activity association for trade-domain flows (product sales and supply
 * purchases) and the extended activity breakdown.
 *
 * The breakdown must partition the farm's monetary facts exactly once: cash
 * figures (income/expense/balance) sum to [FarmTotals], and trade figures
 * (gross sales/purchases, payments received/made) sum to the trade-domain
 * totals — never combined, preserving the M5-05 cash-vs-trade separation.
 */
class TradeActivityTest {
    private lateinit var service: FarmSliceService
    private lateinit var farm: FarmState

    @Before
    fun setUp() {
        service = FarmSliceService(InMemoryFarmStore())
        farm = service.createFarm(
            "Mixed Farm",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)
        )
        service.addParty(
            farm.id,
            PartyDraft(name = "Ram", role = PartyRole.CUSTOMER)
        )
        service.addParty(
            farm.id,
            PartyDraft(name = "Sita", role = PartyRole.SUPPLIER)
        )
        service.addProduct(farm.id, "Eggs", ProductUnit.PIECE)
        service.addSupply(farm.id, "Layer feed", ProductUnit.KILOGRAM)
    }

    private fun ram(): Party = service.parties(farm.id).first { it.name == "Ram" }
    private fun sita(): Party = service.parties(farm.id).first { it.name == "Sita" }
    private fun eggs(): FarmProduct = service.products(farm.id).first { it.name == "Eggs" }
    private fun feed(): FarmSupply = service.supplies(farm.id).first { it.name == "Layer feed" }

    @Test
    fun productSaleAssociatesActivityAndSurvivesPersistence() {
        val trade = service.addProductSale(
            farm.id, ram().id, eggs().id,
            quantity = BigDecimal("10"), rateMinor = 500,
            initialPaymentMinor = 5000, occurredAt = "2024-06-01T12:00:00Z",
            activity = FarmActivityType.POULTRY
        )
        assertEquals(TradeType.SALE, trade.type)
        assertEquals(FarmActivityType.POULTRY, trade.activity)
        assertEquals(5000L, trade.totalMinor)

        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(FarmActivityType.POULTRY, decoded.trades.single { it.id == trade.id }.activity)
    }

    @Test
    fun supplyPurchaseAssociatesActivity() {
        val trade = service.addSupplierPurchase(
            farm.id, sita().id, feed().id,
            quantity = BigDecimal("10"), unit = ProductUnit.KILOGRAM,
            amountMinor = 3000, initialPaymentMinor = 1000,
            occurredAt = "2024-06-02T12:00:00Z", description = "Feed",
            activity = FarmActivityType.CROPS
        )
        assertEquals(TradeType.PURCHASE, trade.type)
        assertEquals(FarmActivityType.CROPS, trade.activity)
    }

    @Test
    fun tradeWithoutActivityIsGeneral() {
        val trade = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE, partyId = ram().id, totalMinor = 4000,
                description = "Cash sale", occurredAt = "2024-06-03T12:00:00Z"
            ),
            initialSettlementMinor = 4000
        )
        assertNull(trade.activity)
        val general = service.farmActivityBreakdown(farm.id).first { it.activity == null }
        assertEquals(4000L, general.grossSalesMinor)
        assertEquals(4000L, general.paymentsReceivedMinor)
    }

    @Test
    fun editTradeChangesActivityAndPreservesSettlements() {
        val trade = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE, partyId = ram().id, totalMinor = 4000,
                description = "Sale", occurredAt = "2024-06-04T12:00:00Z"
            ),
            initialSettlementMinor = 4000
        )
        service.updateTrade(
            farm.id, trade.id,
            TradeDraft(
                type = TradeType.SALE, partyId = ram().id, totalMinor = 4000,
                description = "Sale", occurredAt = "2024-06-04T12:00:00Z",
                activity = FarmActivityType.POULTRY
            )
        )
        val updated = service.trade(farm.id, trade.id)!!
        assertEquals(FarmActivityType.POULTRY, updated.activity)
        assertEquals(1, service.settlementsForTrade(farm.id, trade.id).size)
        val poultry = service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.POULTRY }
        assertEquals(4000L, poultry.grossSalesMinor)
        assertEquals(4000L, poultry.paymentsReceivedMinor)
    }

    @Test
    fun settlementsAttributeToTheSettledTradeActivity() {
        val trade = service.addProductSale(
            farm.id, ram().id, eggs().id,
            quantity = BigDecimal("10"), rateMinor = 500,
            initialPaymentMinor = 2000, occurredAt = "2024-06-05T12:00:00Z",
            activity = FarmActivityType.POULTRY
        )
        service.recordCustomerPayment(farm.id, ram().id, 1000, "2024-06-20T12:00:00Z")

        val poultry = service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.POULTRY }
        assertEquals(5000L, poultry.grossSalesMinor)
        assertEquals(3000L, poultry.paymentsReceivedMinor)
        val updatedFarm = service.loadFarm(farm.id)!!
        assertEquals(2000L, updatedFarm.settlements.outstandingMinorFor(trade))
    }

    @Test
    fun disablingActivityKeepsTradeHistoryAndBucket() {
        service.addSupplierPurchase(
            farm.id, sita().id, feed().id,
            quantity = BigDecimal("10"), unit = ProductUnit.KILOGRAM,
            amountMinor = 3000, initialPaymentMinor = 1000,
            occurredAt = "2024-06-06T12:00:00Z", description = "Feed",
            activity = FarmActivityType.CROPS
        )

        service.disableFarmActivity(farm.id, FarmActivityType.CROPS)

        val updated = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.POULTRY), updated.activities)
        assertEquals(listOf(FarmActivityType.CROPS), updated.disabledActivities)
        val crops = service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.CROPS }
        assertEquals(3000L, crops.grossPurchasesMinor)
        assertEquals(1000L, crops.paymentsMadeMinor)
    }

    @Test
    fun disabledActivityIsNotSelectableForNewTrades() {
        val choices = FarmActivityCatalog.activityChoices(
            activities = setOf(FarmActivityType.POULTRY),
            currentActivity = null
        )
        assertTrue(FarmActivityType.CROPS !in choices.mapNotNull { it })
        assertTrue(FarmActivityType.POULTRY in choices.mapNotNull { it })
        assertEquals(listOf<FarmActivityType?>(null, FarmActivityType.POULTRY), choices)
    }

    @Test
    fun editingKeepsDisabledCurrentActivitySelection() {
        val choices = FarmActivityCatalog.activityChoices(
            activities = setOf(FarmActivityType.POULTRY),
            currentActivity = FarmActivityType.CROPS
        )
        assertEquals(
            listOf<FarmActivityType?>(null, FarmActivityType.POULTRY, FarmActivityType.CROPS),
            choices
        )
    }

    @Test
    fun mixedScenarioBreakdownReconcilesWithCanonicalTotals() {
        // Cash: one tagged expense and one general income.
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE, category = TransactionCategory.FEED,
                amountMinor = 1000, description = "Poultry feed",
                occurredAt = "2024-07-01T12:00:00Z",
                activity = FarmActivityType.POULTRY
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME, category = TransactionCategory.SERVICES,
                amountMinor = 2000, description = "General income",
                occurredAt = "2024-07-02T12:00:00Z"
            )
        )

        // Trade: partial poultry sale, then a later customer settlement.
        service.addProductSale(
            farm.id, ram().id, eggs().id,
            quantity = BigDecimal("10"), rateMinor = 500,
            initialPaymentMinor = 2000, occurredAt = "2024-07-03T12:00:00Z",
            activity = FarmActivityType.POULTRY
        )
        service.recordCustomerPayment(farm.id, ram().id, 1000, "2024-07-10T12:00:00Z")

        // Trade: fully paid general sale (cash sale with a party).
        service.addProductSale(
            farm.id, ram().id, eggs().id,
            quantity = BigDecimal("5"), rateMinor = 500,
            initialPaymentMinor = 2500, occurredAt = "2024-07-04T12:00:00Z"
        )

        // Trade: partial crops purchase.
        service.addSupplierPurchase(
            farm.id, sita().id, feed().id,
            quantity = BigDecimal("10"), unit = ProductUnit.KILOGRAM,
            amountMinor = 3000, initialPaymentMinor = 1000,
            occurredAt = "2024-07-05T12:00:00Z", description = "Feed",
            activity = FarmActivityType.CROPS
        )

        // Trade: fully paid general purchase.
        service.addSupplierPurchase(
            farm.id, sita().id, feed().id,
            quantity = BigDecimal("4"), unit = ProductUnit.KILOGRAM,
            amountMinor = 1200, initialPaymentMinor = 1200,
            occurredAt = "2024-07-06T12:00:00Z", description = "Feed"
        )

        // Disable CROPS after its purchase: bucket must survive.
        service.disableFarmActivity(farm.id, FarmActivityType.CROPS)

        val breakdown = service.farmActivityBreakdown(farm.id)
        val farmTotals = FarmTotals.of(service.loadFarm(farm.id)!!.transactions)
        val farmTrades = service.loadFarm(farm.id)!!.trades
        val farmSettlements = service.loadFarm(farm.id)!!.settlements

        assertEquals(
            farmTotals.incomeMinor,
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.incomeMinor) }
        )
        assertEquals(
            farmTotals.expensesMinor,
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.expenseMinor) }
        )
        assertEquals(
            farmTotals.balanceMinor,
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.balanceMinor) }
        )
        assertEquals(
            farmTrades.filter { it.type == TradeType.SALE }.sumOf { it.totalMinor },
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.grossSalesMinor) }
        )
        assertEquals(
            farmTrades.filter { it.type == TradeType.PURCHASE }.sumOf { it.totalMinor },
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.grossPurchasesMinor) }
        )
        assertEquals(
            farmSettlements.filter { s -> farmTrades.first { it.id == s.tradeId }.type == TradeType.SALE }.sumOf { it.amountMinor },
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.paymentsReceivedMinor) }
        )
        assertEquals(
            farmSettlements.filter { s -> farmTrades.first { it.id == s.tradeId }.type == TradeType.PURCHASE }.sumOf { it.amountMinor },
            breakdown.fold(0L) { acc, b -> Math.addExact(acc, b.paymentsMadeMinor) }
        )

        // Bucket-level expectations.
        val poultry = breakdown.first { it.activity == FarmActivityType.POULTRY }
        assertEquals(0L, poultry.incomeMinor)
        assertEquals(1000L, poultry.expenseMinor)
        assertEquals(-1000L, poultry.balanceMinor)
        assertEquals(5000L, poultry.grossSalesMinor)
        assertEquals(0L, poultry.grossPurchasesMinor)
        assertEquals(3000L, poultry.paymentsReceivedMinor)
        assertEquals(0L, poultry.paymentsMadeMinor)

        val crops = breakdown.first { it.activity == FarmActivityType.CROPS }
        assertEquals(3000L, crops.grossPurchasesMinor)
        assertEquals(1000L, crops.paymentsMadeMinor)
        assertEquals(0L, crops.grossSalesMinor)

        val general = breakdown.first { it.activity == null }
        assertEquals(2000L, general.incomeMinor)
        assertEquals(0L, general.expenseMinor)
        assertEquals(2500L, general.grossSalesMinor)
        assertEquals(1200L, general.grossPurchasesMinor)
        assertEquals(2500L, general.paymentsReceivedMinor)
        assertEquals(1200L, general.paymentsMadeMinor)

        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.POULTRY, null), breakdown.map { it.activity })
    }

    @Test
    fun supplierPaymentAttributesToPurchasedTradeActivity() {
        service.addSupplierPurchase(
            farm.id, sita().id, feed().id,
            quantity = BigDecimal("10"), unit = ProductUnit.KILOGRAM,
            amountMinor = 3000, initialPaymentMinor = 1000,
            occurredAt = "2024-07-15T12:00:00Z", description = "Feed",
            activity = FarmActivityType.POULTRY
        )
        service.recordSupplierPayment(farm.id, sita().id, 500, "2024-07-25T12:00:00Z")

        val poultry = service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.POULTRY }
        assertEquals(3000L, poultry.grossPurchasesMinor)
        assertEquals(1500L, poultry.paymentsMadeMinor)
    }
}