package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartyTradeSettlementModelTest {

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    // ── PartyRole.compatibleWith ──────────────────────────────────

    @Test
    fun customerCompatibleWithSale() {
        assertTrue(PartyRole.CUSTOMER.compatibleWith(TradeType.SALE))
    }

    @Test
    fun customerNotCompatibleWithPurchase() {
        assertFalse(PartyRole.CUSTOMER.compatibleWith(TradeType.PURCHASE))
    }

    @Test
    fun supplierCompatibleWithPurchase() {
        assertTrue(PartyRole.SUPPLIER.compatibleWith(TradeType.PURCHASE))
    }

    @Test
    fun supplierNotCompatibleWithSale() {
        assertFalse(PartyRole.SUPPLIER.compatibleWith(TradeType.SALE))
    }

    @Test
    fun bothCompatibleWithSaleAndPurchase() {
        assertTrue(PartyRole.BOTH.compatibleWith(TradeType.SALE))
        assertTrue(PartyRole.BOTH.compatibleWith(TradeType.PURCHASE))
    }

    @Test
    fun otherNotCompatibleWithEither() {
        assertFalse(PartyRole.OTHER.compatibleWith(TradeType.SALE))
        assertFalse(PartyRole.OTHER.compatibleWith(TradeType.PURCHASE))
    }

    // ── paymentStatusOf ───────────────────────────────────────────

    @Test
    fun unpaidWhenZero() {
        assertEquals(PaymentStatus.UNPAID, paymentStatusOf(1000, 0))
    }

    @Test
    fun partialWhenLessThanTotal() {
        assertEquals(PaymentStatus.PARTIAL, paymentStatusOf(1000, 500))
    }

    @Test
    fun paidWhenEqualToTotal() {
        assertEquals(PaymentStatus.PAID, paymentStatusOf(1000, 1000))
    }

    @Test
    fun paidWhenExceedsTotal() {
        assertEquals(PaymentStatus.PAID, paymentStatusOf(1000, 1500))
    }

    @Test
    fun unpaidWhenNegative() {
        assertEquals(PaymentStatus.UNPAID, paymentStatusOf(1000, -100))
    }

    // ── Trade.isCashTrade ─────────────────────────────────────────

    @Test
    fun cashTradeWhenNoPartyAndFullySettled() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = null,
            totalMinor = 5000, description = "", occurredAt = now
        )
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 5000, occurredAt = now, note = "")
        )
        assertTrue(trade.isCashTrade(settlements))
    }

    @Test
    fun notCashTradeWhenPartyExists() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        assertFalse(trade.isCashTrade(emptyList()))
    }

    @Test
    fun notCashTradeWhenUnsettled() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = null,
            totalMinor = 5000, description = "", occurredAt = now
        )
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 2000, occurredAt = now, note = "")
        )
        assertFalse(trade.isCashTrade(settlements))
    }

    // ── List<Settlement>.paidMinorFor ─────────────────────────────

    @Test
    fun paidMinorForReturnsZeroWhenNoSettlements() {
        val settlements = emptyList<Settlement>()
        assertEquals(0L, settlements.paidMinorFor("nonexistent"))
    }

    @Test
    fun paidMinorForSumsSettlementsForTrade() {
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 1000, occurredAt = now, note = ""),
            Settlement(id = "s2", tradeId = "t1", amountMinor = 2000, occurredAt = now, note = ""),
            Settlement(id = "s3", tradeId = "t2", amountMinor = 5000, occurredAt = now, note = "")
        )
        assertEquals(3000L, settlements.paidMinorFor("t1"))
    }

    @Test
    fun paidMinorForOnlyCountsMatchingTrade() {
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 1000, occurredAt = now, note = ""),
            Settlement(id = "s2", tradeId = "t2", amountMinor = 2000, occurredAt = now, note = "")
        )
        assertEquals(1000L, settlements.paidMinorFor("t1"))
    }

    // ── List<Settlement>.outstandingMinorFor ──────────────────────

    @Test
    fun outstandingMinorForFullTrade() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        assertEquals(5000L, emptyList<Settlement>().outstandingMinorFor(trade))
    }

    @Test
    fun outstandingMinorForPartialPayment() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 2000, occurredAt = now, note = "")
        )
        assertEquals(3000L, settlements.outstandingMinorFor(trade))
    }

    @Test
    fun outstandingMinorForFullySettled() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 5000, occurredAt = now, note = "")
        )
        assertEquals(0L, settlements.outstandingMinorFor(trade))
    }

    // ── List<Settlement>.paymentSummaryFor ────────────────────────

    @Test
    fun paymentSummaryForUnpaid() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        val summary = emptyList<Settlement>().paymentSummaryFor(trade)
        assertEquals(0L, summary.paidMinor)
        assertEquals(5000L, summary.outstandingMinor)
        assertEquals(PaymentStatus.UNPAID, summary.status)
    }

    @Test
    fun paymentSummaryForPartial() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 2000, occurredAt = now, note = "")
        )
        val summary = settlements.paymentSummaryFor(trade)
        assertEquals(2000L, summary.paidMinor)
        assertEquals(3000L, summary.outstandingMinor)
        assertEquals(PaymentStatus.PARTIAL, summary.status)
    }

    @Test
    fun paymentSummaryForPaid() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5000, description = "", occurredAt = now
        )
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 3000, occurredAt = now, note = ""),
            Settlement(id = "s2", tradeId = "t1", amountMinor = 2000, occurredAt = now, note = "")
        )
        val summary = settlements.paymentSummaryFor(trade)
        assertEquals(5000L, summary.paidMinor)
        assertEquals(0L, summary.outstandingMinor)
        assertEquals(PaymentStatus.PAID, summary.status)
    }

    // ── List<Settlement>.settlementsForTrade ──────────────────────

    @Test
    fun settlementsForTradeReturnsMatchingOnly() {
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 1000, occurredAt = now, note = ""),
            Settlement(id = "s2", tradeId = "t2", amountMinor = 2000, occurredAt = now, note = ""),
            Settlement(id = "s3", tradeId = "t1", amountMinor = 3000, occurredAt = now, note = "")
        )
        val result = settlements.settlementsForTrade("t1")
        assertEquals(2, result.size)
        assertTrue(result.all { it.tradeId == "t1" })
    }

    @Test
    fun settlementsForTradeReturnsEmptyWhenNoneMatch() {
        val settlements = listOf(
            Settlement(id = "s1", tradeId = "t1", amountMinor = 1000, occurredAt = now, note = "")
        )
        assertEquals(0, settlements.settlementsForTrade("nonexistent").size)
    }

    // ── TradeDraft.toTrade ────────────────────────────────────────

    @Test
    fun tradeDraftToTradeNormalizesToUtc() {
        val draft = TradeDraft(
            type = TradeType.SALE,
            partyId = "p1",
            totalMinor = 5000,
            description = "  trimmed  ",
            occurredAt = "2024-06-15T10:30:00+05:45"
        )
        val trade = draft.toTrade("t1")
        assertEquals("t1", trade.id)
        assertEquals(TradeType.SALE, trade.type)
        assertEquals("p1", trade.partyId)
        assertEquals(5000L, trade.totalMinor)
        assertEquals("trimmed", trade.description)
        assertEquals(ZoneOffset.UTC, trade.occurredAt.offset)
    }

    @Test
    fun tradeDraftBlankPartyIdBecomesNull() {
        val draft = TradeDraft(
            type = TradeType.SALE,
            partyId = "   ",
            totalMinor = 5000,
            occurredAt = "2024-01-01T12:00:00Z"
        )
        val trade = draft.toTrade("t1")
        assertEquals(null, trade.partyId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun tradeDraftRejectsInvalidDate() {
        val draft = TradeDraft(
            type = TradeType.SALE,
            partyId = null,
            totalMinor = 5000,
            occurredAt = "not-a-date"
        )
        draft.toTrade("t1")
    }

    // ── SettlementDraft.toSettlement ──────────────────────────────

    @Test
    fun settlementDraftToSettlementNormalizesToUtc() {
        val draft = SettlementDraft(
            tradeId = "t1",
            amountMinor = 3000,
            occurredAt = "2024-06-15T10:30:00+05:45",
            note = "  trimmed  "
        )
        val settlement = draft.toSettlement("s1")
        assertEquals("s1", settlement.id)
        assertEquals("t1", settlement.tradeId)
        assertEquals(3000L, settlement.amountMinor)
        assertEquals("trimmed", settlement.note)
        assertEquals(ZoneOffset.UTC, settlement.occurredAt.offset)
    }

    @Test(expected = IllegalArgumentException::class)
    fun settlementDraftRejectsInvalidDate() {
        val draft = SettlementDraft(
            tradeId = "t1",
            amountMinor = 3000,
            occurredAt = "not-a-date"
        )
        draft.toSettlement("s1")
    }

    // ── FarmTransactionDraft.toTransaction ────────────────────────

    @Test
    fun transactionDraftToTransactionNormalizesToUtc() {
        val draft = FarmTransactionDraft(
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FEED,
            amountMinor = 5000,
            description = "  feed  ",
            occurredAt = "2024-06-15T10:30:00+05:45"
        )
        val tx = draft.toTransaction("tx1")
        assertEquals("tx1", tx.id)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionCategory.FEED, tx.category)
        assertEquals(5000L, tx.amountMinor)
        assertEquals("feed", tx.description)
        assertEquals(ZoneOffset.UTC, tx.occurredAt.offset)
    }

    @Test(expected = IllegalArgumentException::class)
    fun transactionDraftRejectsInvalidDate() {
        val draft = FarmTransactionDraft(
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FEED,
            amountMinor = 5000,
            description = "feed",
            occurredAt = "not-a-date"
        )
        draft.toTransaction("tx1")
    }

    // ── FarmProduct ───────────────────────────────────────────────

    @Test
    fun farmProductUnitLabelReturnsCustomLabelForCustom() {
        val product = FarmProduct(id = "p1", name = "Eggs", defaultUnit = ProductUnit.CUSTOM, customUnitLabel = "Dozen")
        assertEquals("Dozen", product.unitLabel())
    }

    @Test
    fun farmProductUnitLabelReturnsEnumNameForStandard() {
        val product = FarmProduct(id = "p1", name = "Milk", defaultUnit = ProductUnit.LITRE)
        assertEquals("LITRE", product.unitLabel())
    }

    // ── FarmState.hasMonetaryRecords ───────────────────────────────

    @Test
    fun emptyFarmHasNoMonetaryRecords() {
        val farm = FarmState(id = "f1", name = "Test")
        assertFalse(farm.hasMonetaryRecords())
    }

    @Test
    fun farmWithTransactionHasMonetaryRecords() {
        val farm = FarmState(
            id = "f1", name = "Test",
            transactions = mutableListOf(
                FarmTransaction(id = "tx1", type = TransactionType.EXPENSE, category = TransactionCategory.FEED,
                    amountMinor = 1000, description = "feed", occurredAt = now)
            )
        )
        assertTrue(farm.hasMonetaryRecords())
    }

    @Test
    fun farmWithTradeHasMonetaryRecords() {
        val farm = FarmState(
            id = "f1", name = "Test",
            trades = mutableListOf(
                Trade(id = "t1", type = TradeType.SALE, partyId = null, totalMinor = 5000,
                    description = "", occurredAt = now)
            )
        )
        assertTrue(farm.hasMonetaryRecords())
    }

    @Test
    fun farmWithSettlementHasMonetaryRecords() {
        val farm = FarmState(
            id = "f1", name = "Test",
            settlements = mutableListOf(
                Settlement(id = "s1", tradeId = "t1", amountMinor = 5000, occurredAt = now, note = "")
            )
        )
        assertTrue(farm.hasMonetaryRecords())
    }

    // ── FarmState.supplyQuantityAvailable ──────────────────────────

    @Test
    fun supplyQuantityAvailableWithNoRecords() {
        val farm = FarmState(id = "f1", name = "Test")
        assertEquals(BigDecimal.ZERO, farm.supplyQuantityAvailable("nonexistent"))
    }

    @Test
    fun supplyQuantityAvailableComputesNet() {
        val farm = FarmState(
            id = "f1", name = "Test",
            supplyPurchaseDetails = mutableListOf(
                SupplyPurchaseDetail(transactionId = "tx1", supplyId = "s1", quantity = BigDecimal("100"), unit = ProductUnit.KILOGRAM)
            ),
            supplyUsages = mutableListOf(
                SupplyUsage(id = "u1", supplyId = "s1", quantity = BigDecimal("30"), unit = ProductUnit.KILOGRAM, occurredAt = now)
            ),
            transactions = mutableListOf(
                FarmTransaction(id = "tx1", type = TransactionType.EXPENSE, category = TransactionCategory.SUPPLIES,
                    amountMinor = 1000, description = "feed", occurredAt = now)
            )
        )
        assertEquals(BigDecimal("70"), farm.supplyQuantityAvailable("s1"))
    }

    // ── PartyRole enum values ─────────────────────────────────────

    @Test
    fun allPartyRolesExist() {
        assertEquals(4, PartyRole.entries.size)
        assertTrue(PartyRole.entries.containsAll(setOf(
            PartyRole.CUSTOMER, PartyRole.SUPPLIER, PartyRole.BOTH, PartyRole.OTHER
        )))
    }
}
