package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * M15: generic cash income/expense entries — ordinary money events that are
 * not tied to a Party, Trade, Product, Supply, or Settlement. These tests pin
 * the accounting semantics the new Record-sheet verbs rely on.
 */
class GenericCashEntryTest {

    private lateinit var service: FarmSliceService

    @Before
    fun setUp() {
        service = FarmSliceService(InMemoryFarmStore())
    }

    private fun seedFarm(): FarmState =
        service.createFarm("Cash Farm", currencyCode = "USD")

    private fun genericExpense(
        farmId: String,
        amountMinor: Long = 1500,
        description: String = "Transport",
        activity: FarmActivityType? = null,
        category: TransactionCategory = TransactionCategory.OTHER_EXPENSE
    ): FarmTransaction =
        service.createTransaction(
            farmId,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = category,
                activity = activity,
                amountMinor = amountMinor,
                description = description,
                occurredAt = "2024-03-01T10:00:00Z"
            )
        )

    private fun genericIncome(
        farmId: String,
        amountMinor: Long = 2500,
        description: String = "Manure sale",
        activity: FarmActivityType? = null
    ): FarmTransaction =
        service.createTransaction(
            farmId,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.OTHER_INCOME,
                activity = activity,
                amountMinor = amountMinor,
                description = description,
                occurredAt = "2024-03-02T10:00:00Z"
            )
        )

    @Test
    fun genericExpenseCreatesPlainTransactionWithoutPartyTradeOrSettlement() {
        val farm = seedFarm()
        val tx = genericExpense(farm.id)
        val loaded = service.loadFarm(farm.id)!!
        assertEquals(listOf(tx.id), loaded.transactions.map { it.id })
        assertTrue(loaded.trades.isEmpty())
        assertTrue(loaded.settlements.isEmpty())
        assertTrue(loaded.parties.isEmpty())
        assertEquals(TransactionCategory.OTHER_EXPENSE, loaded.transactions.single().category)
    }

    @Test
    fun genericIncomeRequiresNoCustomerAndNeverCreatesReceivable() {
        val farm = seedFarm()
        genericIncome(farm.id, amountMinor = 2500)
        val loaded = service.loadFarm(farm.id)!!
        assertEquals(2500L, FarmTotals.of(loaded.transactions).incomeMinor)
        assertTrue(loaded.parties.none { it.role == PartyRole.CUSTOMER })
        assertTrue(loaded.trades.isEmpty())
        assertTrue(loaded.settlements.isEmpty())
    }

    @Test
    fun categoryMustMatchTransactionType() {
        val farm = seedFarm()
        try {
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SALES,
                    amountMinor = 100,
                    description = "mismatched",
                    occurredAt = "2024-03-01T10:00:00Z"
                )
            )
            fail("expense with income category must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("invalid for the selected type"))
        }
    }

    @Test
    fun totalsCountGenericEntriesExactlyOnce() {
        val farm = seedFarm()
        genericExpense(farm.id, amountMinor = 1500)
        genericIncome(farm.id, amountMinor = 2500)
        val loaded = service.loadFarm(farm.id)!!
        val totals = FarmTotals.of(loaded.transactions)
        assertEquals(2500L, totals.incomeMinor)
        assertEquals(1500L, totals.expensesMinor)
        assertEquals(1000L, totals.balanceMinor)
    }

    @Test
    fun editAndDeleteFollowOrdinaryTransactionSemantics() {
        val farm = seedFarm()
        val tx = genericExpense(farm.id, amountMinor = 1500)
        service.updateTransaction(
            farm.id,
            tx.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.LABOR,
                amountMinor = 2000,
                description = "Day labour",
                occurredAt = "2024-03-01T10:00:00Z"
            )
        )
        var loaded = service.loadFarm(farm.id)!!
        assertEquals(2000L, FarmTotals.of(loaded.transactions).expensesMinor)
        assertEquals(TransactionCategory.LABOR, loaded.transactions.single().category)

        service.deleteTransaction(farm.id, tx.id)
        loaded = service.loadFarm(farm.id)!!
        assertTrue(loaded.transactions.isEmpty())
        assertEquals(0L, FarmTotals.of(loaded.transactions).expensesMinor)
    }

    @Test
    fun activityAttributionDefaultGeneralExplicitBucketed() {
        val farm = service.createFarm("Attribution Farm", currencyCode = "USD", activities = listOf(FarmActivityType.POULTRY))
        genericExpense(farm.id, amountMinor = 1000, activity = null)
        genericIncome(farm.id, amountMinor = 3000, activity = FarmActivityType.POULTRY)

        val breakdown = service.farmActivityBreakdown(farm.id).associateBy { it.activity }
        val general = breakdown[null]
        val poultry = breakdown[FarmActivityType.POULTRY]
        assertNotNull(general)
        assertNotNull(poultry)
        // Cash columns carry generic transactions exactly once; trade columns stay zero.
        assertEquals(1000L, general!!.expenseMinor)
        assertEquals(0L, general.incomeMinor)
        assertEquals(3000L, poultry!!.incomeMinor)
        assertEquals(0L, poultry.expenseMinor)
        assertEquals(0L, poultry.grossSalesMinor)
        assertEquals(0L, poultry.paymentsReceivedMinor)

        // Breakdown cash columns reconcile with authoritative farm totals.
        val totals = FarmTotals.of(service.loadFarm(farm.id)!!.transactions)
        assertEquals(totals.incomeMinor, breakdown.values.sumOf { it.incomeMinor })
        assertEquals(totals.expensesMinor, breakdown.values.sumOf { it.expenseMinor })
    }

    @Test
    fun dailyOverviewCountsGenericIncomeAsMoneyReceived() {
        val farm = seedFarm()
        genericIncome(farm.id, amountMinor = 2500)
        val loaded = service.loadFarm(farm.id)!!
        val overview = loaded.farmerOverview(
            OffsetDateTime.parse("2024-03-02T12:00:00Z"),
            ZoneOffset.UTC
        ).daily
        // Generic cash income is real money received; hiding it from the
        // farmer's daily view would defeat the M15 entry path.
        assertEquals(2500L, overview.moneyReceivedMinor)
        // It is not a trade sale.
        assertEquals(0L, overview.salesMinor)
    }

    @Test
    fun persistenceRoundTripPreservesGenericEntries() {
        val farm = seedFarm()
        val expense = genericExpense(farm.id, amountMinor = 1200)
        val income = genericIncome(farm.id, amountMinor = 3400, activity = null)
        val encoded = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val decoded = FarmPersistenceCodec.decode(encoded)
        assertEquals(2, decoded.transactions.size)
        val decodedById = decoded.transactions.associateBy { it.id }
        assertEquals(expense.amountMinor, decodedById.getValue(expense.id).amountMinor)
        assertEquals(TransactionCategory.OTHER_EXPENSE, decodedById.getValue(expense.id).category)
        assertNull(decodedById.getValue(expense.id).activity)
        assertEquals(income.amountMinor, decodedById.getValue(income.id).amountMinor)
    }

    @Test
    fun backupRoundTripKeepsGenericEntriesStable() {
        val farm = seedFarm()
        genericExpense(farm.id, amountMinor = 900)
        val before = service.loadFarm(farm.id)!!
        val envelopeBefore = FarmBackupCodec.encode(before)
        val decoded = FarmBackupCodec.decode(envelopeBefore)
        assertEquals(before.transactions, decoded.farm.transactions)
        // Byte-stable re-export of the decoded farm.
        assertEquals(envelopeBefore, FarmBackupCodec.encode(decoded.farm))
    }
}
