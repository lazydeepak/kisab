package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M5-05 Farm Financial Overview projection tests. All asserts run against the
 * derived [FarmFinancialOverview] built from authoritative persisted facts —
 * the overview itself is never persisted; it is a pure view recomputed on
 * every call with a stable clock and zone.
 *
 * Period semantics under test: every period is **start-inclusive /
 * end-exclusive** ([FinancialPeriod]). The "current position" is stated as of
 * the period's exclusive end ([FarmFinancialOverview.currentPosition]) and
 * reflects obligations created strictly before the cutoff net of settlements
 * recorded strictly before the cutoff — independent of the period's start.
 */
class FarmFinancialOverviewTest {
    private val service = FarmSliceService(InMemoryFarmStore())
    private val zone: ZoneId = ZoneId.of("Asia/Kathmandu")
    private val now: OffsetDateTime = OffsetDateTime.parse("2024-02-20T12:00:00Z")
    private val fixedExportedAt: OffsetDateTime = OffsetDateTime.parse("2024-02-20T12:00:00Z")

    private fun overview(preset: FinancialPeriodPreset): FarmFinancialOverview =
        service.farmFinancialOverview(service.currentFarmId()!!, preset, now, zone)

    private fun addIncome(occurredAt: OffsetDateTime, amountMinor: Long = 1000) {
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = amountMinor,
                description = "Sale",
                occurredAt = occurredAt.toString()
            )
        )
    }

    private fun addExpense(occurredAt: OffsetDateTime, amountMinor: Long = 1000) {
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = amountMinor,
                description = "Feed",
                occurredAt = occurredAt.toString()
            )
        )
    }

    @Test
    fun emptyFarmShowsZeroTotalsAndEmptyTrend() {
        val farm = service.createFarm("Demo Farm")

        assertEquals(FinancialPeriodPreset.THIS_MONTH, overview(FinancialPeriodPreset.THIS_MONTH).period.preset)
        val cash = overview(FinancialPeriodPreset.ALL_TIME).cashTotals
        assertEquals(0, cash.incomeMinor)
        assertEquals(0, cash.expenseMinor)
        assertEquals(0, cash.netMinor)
        val trade = overview(FinancialPeriodPreset.ALL_TIME).tradeTotals
        assertEquals(0, trade.grossSalesMinor)
        assertEquals(0, trade.grossPurchasesMinor)
        assertEquals(0, trade.paymentsReceivedMinor)
        assertEquals(0, trade.paymentsMadeMinor)
        val position = overview(FinancialPeriodPreset.ALL_TIME).currentPosition
        assertEquals(0, position.receivableMinor)
        assertEquals(0, position.payableMinor)
        assertEquals(0, position.netMinor)
        assertTrue(overview(FinancialPeriodPreset.ALL_TIME).monthlyTrend.isEmpty())
        assertEquals(farm.id, service.currentFarmId())
    }

    @Test
    fun cashActivitySumsIncomeAndExpenseInPeriod() {
        service.createFarm("Demo Farm")
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 10000, description = "Sale", occurredAt = "2024-02-03T12:00:00Z")
        )
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.EXPENSE, category = TransactionCategory.FEED, amountMinor = 4000, description = "Feed", occurredAt = "2024-02-10T12:00:00Z")
        )

        val cash = overview(FinancialPeriodPreset.ALL_TIME).cashTotals
        assertEquals(10000, cash.incomeMinor)
        assertEquals(4000, cash.expenseMinor)
        assertEquals(6000, cash.netMinor)
    }

    @Test
    fun thisMonthExcludesTransactionsOutsideTheMonth() {
        service.createFarm("Demo Farm")
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 5000, description = "Sale", occurredAt = "2024-01-15T12:00:00Z")
        )
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 7000, description = "Sale", occurredAt = "2024-02-05T12:00:00Z")
        )

        val cash = overview(FinancialPeriodPreset.THIS_MONTH).cashTotals
        assertEquals(7000, cash.incomeMinor)
        assertEquals(0, cash.expenseMinor)
    }

    @Test
    fun thisMonthPeriodIsStartInclusiveEndExclusiveInFixedNonUtcZone() {
        service.createFarm("Demo Farm")
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.THIS_MONTH, now, zone)

        addIncome(period.startInclusive, amountMinor = 100)
        addIncome(period.startInclusive.minusNanos(1), amountMinor = 200)
        addIncome(period.endExclusive.minusNanos(1), amountMinor = 400)
        addIncome(period.endExclusive, amountMinor = 800)

        val cash = service.farmFinancialOverview(
            service.currentFarmId()!!, FinancialPeriodPreset.THIS_MONTH, now, zone
        ).cashTotals

        // startInclusive and the nanosecond just before endExclusive are inside;
        // one nanosecond before the start and the exact endExclusive are outside.
        assertEquals(500, cash.incomeMinor)
    }

    @Test
    fun last30DaysHasInclusiveStartAndExclusiveEnd() {
        service.createFarm("Demo Farm")
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)

        // Exactly at the inclusive start (now - 30d) counts.
        addIncome(period.startInclusive, amountMinor = 100)
        // One nanosecond before the start is outside the window.
        addIncome(period.startInclusive.minusNanos(1), amountMinor = 200)
        // One nanosecond before the exclusive end (now) counts.
        addIncome(period.endExclusive.minusNanos(1), amountMinor = 400)
        // Exactly at the exclusive end (now) is outside the window.
        addIncome(period.endExclusive, amountMinor = 800)

        val cash = overview(FinancialPeriodPreset.LAST_30_DAYS).cashTotals
        assertEquals(500, cash.incomeMinor)
    }

    @Test
    fun exactInstantAtLast30DaysBoundariesAreRespected() {
        service.createFarm("Demo Farm")
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)

        addIncome(period.startInclusive, amountMinor = 10)
        addIncome(period.endExclusive.minusNanos(1), amountMinor = 20)
        addIncome(period.endExclusive, amountMinor = 30)

        val cash = overview(FinancialPeriodPreset.LAST_30_DAYS).cashTotals
        assertEquals(30, cash.incomeMinor)
    }

    @Test
    fun tradeActivityCountsSalesPurchasesAndSettlementsInPeriod() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val supplier = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Store", role = PartyRole.SUPPLIER))
        val sale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 20000, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.PURCHASE, partyId = supplier.id, totalMinor = 8000, occurredAt = "2024-02-08T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = sale.id, amountMinor = 5000, occurredAt = "2024-02-12T12:00:00Z")
        )

        val trade = overview(FinancialPeriodPreset.ALL_TIME).tradeTotals
        assertEquals(20000, trade.grossSalesMinor)
        assertEquals(8000, trade.grossPurchasesMinor)
        assertEquals(5000, trade.paymentsReceivedMinor)
        assertEquals(0, trade.paymentsMadeMinor)
    }

    @Test
    fun currentPositionCountsOutstandingOnlyAtEndOfPeriod() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val supplier = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Store", role = PartyRole.SUPPLIER))
        val sale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 20000, occurredAt = "2024-02-05T12:00:00Z")
        )
        val purchase = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.PURCHASE, partyId = supplier.id, totalMinor = 6000, occurredAt = "2024-02-08T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = sale.id, amountMinor = 5000, occurredAt = "2024-02-12T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = purchase.id, amountMinor = 1000, occurredAt = "2024-02-12T12:00:00Z")
        )

        val position = overview(FinancialPeriodPreset.ALL_TIME).currentPosition
        assertEquals(15000, position.receivableMinor)
        assertEquals(5000, position.payableMinor)
        assertEquals(10000, position.netMinor)
    }

    @Test
    fun positionAsOfEndExclusiveCountsPreCutoffTradesAndSettlementsOnly() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)

        // A trade created before the period start still belongs to the as-of
        // position (it existed before the cutoff).
        val oldSale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 10000, occurredAt = period.startInclusive.minusDays(30).toString())
        )
        // A settlement recorded strictly before the cutoff reduces the position.
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = oldSale.id, amountMinor = 2000, occurredAt = period.startInclusive.toString())
        )
        // A settlement recorded exactly at the exclusive end does NOT reduce the position.
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = oldSale.id, amountMinor = 1000, occurredAt = period.endExclusive.toString())
        )
        // A settlement recorded after the cutoff does NOT reduce the position.
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = oldSale.id, amountMinor = 1000, occurredAt = period.endExclusive.plusDays(1).toString())
        )
        // A second trade within the period.
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 5000, occurredAt = period.endExclusive.minusDays(10).toString())
        )

        val overview = overview(FinancialPeriodPreset.LAST_30_DAYS)

        // Receivable = 10000 - 2000 (old trade, only the pre-cutoff settlement) + 5000 (in-period trade) = 13000.
        assertEquals(13000, overview.currentPosition.receivableMinor)
        // Activity totals are period-scoped: only the in-period trade and the in-period settlement.
        assertEquals(5000, overview.tradeTotals.grossSalesMinor)
        assertEquals(2000, overview.tradeTotals.paymentsReceivedMinor)
    }

    @Test
    fun partialAndFullySettledSalesAndPurchasesAcrossMixedParties() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val supplier = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Store", role = PartyRole.SUPPLIER))

        // Partial sale (outstanding 3000) and fully settled sale.
        val partialSale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 10000, occurredAt = "2024-02-01T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = partialSale.id, amountMinor = 7000, occurredAt = "2024-02-05T12:00:00Z")
        )
        val settledSale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 6000, occurredAt = "2024-02-02T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = settledSale.id, amountMinor = 6000, occurredAt = "2024-02-06T12:00:00Z")
        )

        // Partial purchase (outstanding 4000) and fully settled purchase.
        val partialPurchase = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.PURCHASE, partyId = supplier.id, totalMinor = 9000, occurredAt = "2024-02-03T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = partialPurchase.id, amountMinor = 5000, occurredAt = "2024-02-07T12:00:00Z")
        )
        val settledPurchase = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.PURCHASE, partyId = supplier.id, totalMinor = 4000, occurredAt = "2024-02-04T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = settledPurchase.id, amountMinor = 4000, occurredAt = "2024-02-08T12:00:00Z")
        )

        val position = overview(FinancialPeriodPreset.ALL_TIME).currentPosition
        assertEquals(3000, position.receivableMinor)
        assertEquals(4000, position.payableMinor)
        assertEquals(-1000, position.netMinor)

        val trade = overview(FinancialPeriodPreset.ALL_TIME).tradeTotals
        assertEquals(16000, trade.grossSalesMinor)
        assertEquals(13000, trade.grossPurchasesMinor)
        assertEquals(13000, trade.paymentsReceivedMinor)
        assertEquals(9000, trade.paymentsMadeMinor)
    }

    @Test
    fun paidTradeHasNoPositionAndStaysOutOfCash() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val sale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 20000, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = sale.id, amountMinor = 20000, occurredAt = "2024-02-12T12:00:00Z")
        )

        val overview = overview(FinancialPeriodPreset.ALL_TIME)
        assertEquals(0, overview.currentPosition.receivableMinor)
        assertEquals(0, overview.cashTotals.incomeMinor)
        assertEquals(20000, overview.tradeTotals.grossSalesMinor)
        assertEquals(20000, overview.tradeTotals.paymentsReceivedMinor)
    }

    @Test
    fun cashAndPartySaleAreNeverCombined() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 1000, description = "Sale", occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 9000, occurredAt = "2024-02-06T12:00:00Z")
        )

        val overview = overview(FinancialPeriodPreset.ALL_TIME)
        assertEquals(1000, overview.cashTotals.incomeMinor)
        assertEquals(9000, overview.tradeTotals.grossSalesMinor)
    }

    @Test
    fun editingFactsChangesNextOverview() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 20000, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.updateTrade(
            service.currentFarmId()!!,
            trade.id,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 15000, occurredAt = trade.occurredAt.toString())
        )

        val overview = overview(FinancialPeriodPreset.ALL_TIME)
        assertEquals(15000, overview.tradeTotals.grossSalesMinor)
        assertEquals(15000, overview.currentPosition.receivableMinor)
    }

    @Test
    fun monthlyTrendGroupsByMonthAndZeroFillsContinuousRange() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 1000, description = "Sale", occurredAt = "2024-01-02T12:00:00Z")
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 5000, occurredAt = "2024-02-03T12:00:00Z")
        )

        val rows = overview(FinancialPeriodPreset.ALL_TIME).monthlyTrend
        assertEquals(2, rows.size)
        assertEquals(listOf(2024), rows.map { it.year }.distinct())
        assertEquals(1, rows[0].month)
        assertEquals(1000, rows[0].cashIncomeMinor)
        assertEquals(0, rows[1].cashIncomeMinor)
        assertEquals(2, rows[1].month)
        assertEquals(5000, rows[1].salesMinor)
    }

    @Test
    fun monthlyTrendIsDeterministicAcrossCalls() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 1000, description = "Sale", occurredAt = "2024-01-02T12:00:00Z")
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 5000, occurredAt = "2024-03-03T12:00:00Z")
        )

        val first = overview(FinancialPeriodPreset.ALL_TIME).monthlyTrend
        val second = overview(FinancialPeriodPreset.ALL_TIME).monthlyTrend
        assertEquals(first, second)
    }

    @Test
    fun monthlyTrendCapsAtTwelveRowsAndOrdersOldestToNewest() {
        service.createFarm("Demo Farm")
        // Continuous range of more than 12 months (oldest Jan 2022 … newest Feb 2024,
        // both before the stable `now` of 2024-02-20).
        addIncome(OffsetDateTime.parse("2022-01-05T12:00:00Z"), amountMinor = 1000)
        addIncome(OffsetDateTime.parse("2024-02-05T12:00:00Z"), amountMinor = 2000)

        val rows = overview(FinancialPeriodPreset.ALL_TIME).monthlyTrend

        assertEquals(FarmFinancialPeriods.MAX_TREND_ROWS, rows.size)
        // Oldest → newest, ascending (kept window is Mar 2023 … Feb 2024).
        assertEquals(2023, rows.first().year)
        assertEquals(3, rows.first().month)
        assertEquals(2024, rows.last().year)
        assertEquals(2, rows.last().month)
        for (i in 1 until rows.size) {
            val previous = rows[i - 1]
            val current = rows[i]
            assertTrue(
                "Rows must be strictly ascending: ${previous.year}-${previous.month} then ${current.year}-${current.month}",
                current.year > previous.year || (current.year == previous.year && current.month > previous.month)
            )
        }
        // The kept window is the newest 12 continuous months (Mar 2023 … Feb 2024);
        // the Jan 2022 income is outside the cap, so the first kept row is
        // zero-filled and only the newest month carries its 2000.
        assertEquals(0L, rows.first().cashIncomeMinor)
        assertEquals(2000, rows.last().cashIncomeMinor)
    }

    @Test
    fun cashTotalsOverflowThrowsInsteadOfWrapping() {
        service.createFarm("Demo Farm")
        addIncome(OffsetDateTime.parse("2024-02-05T12:00:00Z"), amountMinor = Long.MAX_VALUE)
        addIncome(OffsetDateTime.parse("2024-02-06T12:00:00Z"), amountMinor = Long.MAX_VALUE)

        assertThrows(ArithmeticException::class.java) {
            overview(FinancialPeriodPreset.ALL_TIME)
        }
    }

    @Test
    fun grossTradeTotalsOverflowThrowsInsteadOfWrapping() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.plusDays(1).toString())
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.plusDays(2).toString())
        )

        assertThrows(ArithmeticException::class.java) {
            overview(FinancialPeriodPreset.LAST_30_DAYS)
        }
    }

    @Test
    fun settlementTotalsOverflowThrowsInsteadOfWrapping() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        // Both trades occur BEFORE the period start so the trade-total loop skips
        // them; both settlements fall INSIDE the period so the payment-total loop
        // accumulates to overflow.
        val oldSale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.minusDays(1).toString())
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = oldSale.id, amountMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.plusDays(1).toString())
        )
        val olderSale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.minusDays(2).toString())
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = olderSale.id, amountMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.plusDays(2).toString())
        )

        assertThrows(ArithmeticException::class.java) {
            overview(FinancialPeriodPreset.LAST_30_DAYS)
        }
    }

    @Test
    fun receivablePositionAggregationOverflowThrowsInsteadOfWrapping() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        // Both trades are outside the period (no trade-total overflow) but before
        // the cutoff, so their outstanding amounts accumulate in the position.
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.minusDays(1).toString())
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = period.startInclusive.minusDays(2).toString())
        )

        assertThrows(ArithmeticException::class.java) {
            overview(FinancialPeriodPreset.LAST_30_DAYS)
        }
    }

    @Test
    fun netPositionSubtractionDoesNotOverflowForMaximumNonNegativeInputs() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val supplier = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Store", role = PartyRole.SUPPLIER))
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = Long.MAX_VALUE, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.PURCHASE, partyId = supplier.id, totalMinor = Long.MAX_VALUE, occurredAt = "2024-02-06T12:00:00Z")
        )

        val position = overview(FinancialPeriodPreset.ALL_TIME).currentPosition
        assertEquals(Long.MAX_VALUE, position.receivableMinor)
        assertEquals(Long.MAX_VALUE, position.payableMinor)
        assertEquals(0L, position.netMinor)
    }

    @Test
    fun orphanSettlementIsRejectedByProjectionInsteadOfSilentlyDropped() {
        // The service boundary rejects orphan settlements at addSettlement time,
        // so a directly constructed FarmState is required to exercise the
        // projection's own guard.
        val farm = FarmState(id = "farm-direct", name = "Direct")
        farm.parties.add(Party(id = "party-1", name = "Ram", role = PartyRole.CUSTOMER))
        farm.trades.add(
            Trade(
                id = "trade-1",
                type = TradeType.SALE,
                partyId = "party-1",
                totalMinor = 10000,
                description = "",
                occurredAt = OffsetDateTime.parse("2024-02-05T12:00:00Z")
            )
        )
        farm.settlements.add(
            Settlement(
                id = "settlement-orphan",
                tradeId = "trade-missing",
                amountMinor = 1000,
                occurredAt = OffsetDateTime.parse("2024-02-06T12:00:00Z"),
                note = ""
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            buildFarmFinancialOverview(farm, FinancialPeriodPreset.ALL_TIME, now, zone)
        }
    }

    @Test
    fun overviewDoesNotChangeEncodedFarmState() {
        service.createFarm("Demo Farm")
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 1000, description = "Sale", occurredAt = "2024-02-05T12:00:00Z")
        )

        val before = FarmPersistenceCodec.encode(service.loadFarm(service.currentFarmId()!!)!!)

        service.farmFinancialOverview(service.currentFarmId()!!, FinancialPeriodPreset.ALL_TIME, now, zone)

        val after = FarmPersistenceCodec.encode(service.loadFarm(service.currentFarmId()!!)!!)

        assertEquals(before, after)
    }

    @Test
    fun schemaV6RoundTripsThroughPersistenceAndBackupByteStableAfterProjection() {
        service.createFarm("Demo Farm")
        val customer = service.addParty(service.currentFarmId()!!, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val sale = service.addTrade(
            service.currentFarmId()!!,
            TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 20000, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addSettlement(
            service.currentFarmId()!!,
            SettlementDraft(tradeId = sale.id, amountMinor = 5000, occurredAt = "2024-02-12T12:00:00Z")
        )
        service.createTransaction(
            service.currentFarmId()!!,
            FarmTransactionDraft(type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 3000, description = "Sale", occurredAt = "2024-02-06T12:00:00Z")
        )

        val farm = service.loadFarm(service.currentFarmId()!!)!!
        assertEquals(14, farm.schemaVersion)
        assertEquals(14, FarmState.CURRENT_FARM_SCHEMA_VERSION)

        // Persistence codec: encode → decode → project → re-encode is byte-stable.
        val encodedPersistence = FarmPersistenceCodec.encode(farm)
        val decoded = FarmPersistenceCodec.decode(encodedPersistence)
        assertEquals(14, decoded.schemaVersion)
        buildFarmFinancialOverview(decoded, FinancialPeriodPreset.ALL_TIME, now, zone)
        assertEquals(encodedPersistence, FarmPersistenceCodec.encode(decoded))

        // Backup envelope: encode → decode → project → re-encode is byte-stable.
        val encodedBackup = FarmBackupCodec.encode(farm, exportedAt = fixedExportedAt)
        val envelope = FarmBackupCodec.decode(encodedBackup)
        assertEquals(14, envelope.farm.schemaVersion)
        buildFarmFinancialOverview(envelope.farm, FinancialPeriodPreset.ALL_TIME, now, zone)
        assertEquals(encodedBackup, FarmBackupCodec.encode(envelope.farm, exportedAt = fixedExportedAt))
    }
}
