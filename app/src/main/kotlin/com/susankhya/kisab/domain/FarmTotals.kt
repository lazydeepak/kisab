package com.susankhya.kisab.domain

/**
 * Pure overview totals derived from the persisted transaction list.
 *
 * Nothing here is stored: the domain persists only individual transactions,
 * and the UI derives income, expenses and balance on every render. Arithmetic
 * uses exact `Long` minor-unit math and throws [ArithmeticException] on
 * overflow instead of silently wrapping.
 */
data class FarmTotals(
    val incomeMinor: Long,
    val expensesMinor: Long,
    val balanceMinor: Long
) {
    companion object {
        fun of(transactions: List<FarmTransaction>): FarmTotals {
            var income = 0L
            var expenses = 0L
            for (transaction in transactions) {
                val amountMinor = transaction.amountMinor
                if (transaction.type == TransactionType.INCOME) {
                    income = Math.addExact(income, amountMinor)
                } else {
                    expenses = Math.addExact(expenses, amountMinor)
                }
            }
            val balance = Math.subtractExact(income, expenses)
            return FarmTotals(income, expenses, balance)
        }
    }
}
