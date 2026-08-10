package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FarmTotalsTest {

    @Test
    fun emptyFarmTotalsAreZero() {
        val totals = FarmTotals.of(emptyList())
        assertEquals(0L, totals.incomeMinor)
        assertEquals(0L, totals.expensesMinor)
        assertEquals(0L, totals.balanceMinor)
    }

    @Test
    fun incomeOnlyFarmSumsIncome() {
        val totals = FarmTotals.of(
            listOf(
                transaction(TransactionType.INCOME, 1000L),
                transaction(TransactionType.INCOME, 2000L)
            )
        )
        assertEquals(3000L, totals.incomeMinor)
        assertEquals(0L, totals.expensesMinor)
        assertEquals(3000L, totals.balanceMinor)
    }

    @Test
    fun expenseOnlyFarmSumsExpenses() {
        val totals = FarmTotals.of(
            listOf(
                transaction(TransactionType.EXPENSE, 1000L),
                transaction(TransactionType.EXPENSE, 2000L)
            )
        )
        assertEquals(0L, totals.incomeMinor)
        assertEquals(3000L, totals.expensesMinor)
        assertEquals(-3000L, totals.balanceMinor)
    }

    @Test
    fun mixedFarmComputesIncomeExpensesAndBalance() {
        val totals = FarmTotals.of(
            listOf(
                transaction(TransactionType.INCOME, 5000L),
                transaction(TransactionType.EXPENSE, 1500L)
            )
        )
        assertEquals(5000L, totals.incomeMinor)
        assertEquals(1500L, totals.expensesMinor)
        assertEquals(3500L, totals.balanceMinor)
    }

    @Test
    fun negativeBalanceIsExact() {
        val totals = FarmTotals.of(
            listOf(
                transaction(TransactionType.INCOME, 1000L),
                transaction(TransactionType.EXPENSE, 2500L)
            )
        )
        assertEquals(-1500L, totals.balanceMinor)
    }

    @Test
    fun largeValuesDoNotWrapWhenTheyFit() {
        val totals = FarmTotals.of(
            listOf(
                transaction(TransactionType.INCOME, Long.MAX_VALUE),
                transaction(TransactionType.EXPENSE, Long.MAX_VALUE)
            )
        )
        assertEquals(Long.MAX_VALUE, totals.incomeMinor)
        assertEquals(Long.MAX_VALUE, totals.expensesMinor)
        assertEquals(0L, totals.balanceMinor)
    }

    @Test
    fun incomeOverflowThrowsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) {
            FarmTotals.of(
                listOf(
                    transaction(TransactionType.INCOME, Long.MAX_VALUE),
                    transaction(TransactionType.INCOME, Long.MAX_VALUE)
                )
            )
        }
    }

    @Test
    fun expenseOverflowThrowsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) {
            FarmTotals.of(
                listOf(
                    transaction(TransactionType.EXPENSE, Long.MAX_VALUE),
                    transaction(TransactionType.EXPENSE, Long.MAX_VALUE)
                )
            )
        }
    }

    private fun transaction(type: TransactionType, amountMinor: Long): FarmTransaction =
        FarmTransaction(
            id = "tx-1",
            type = type,
            category = if (type == TransactionType.INCOME) TransactionCategory.SALES else TransactionCategory.FEED,
            amountMinor = amountMinor,
            description = "d",
            occurredAt = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        )
}
