package com.susankhya.kisab

import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.domain.FarmPlanningCalculator
import com.susankhya.kisab.ui.FarmOrdering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the explicit spinner category lists shown for income and expense,
 * and that the lists are constrained by transaction type with unchanged
 * semantic ordering.
 */
class FarmOrderingTest {

    @Test
    fun incomeShowsSalesServicesOtherIncome() {
        assertEquals(
            listOf(
                TransactionCategory.SALES,
                TransactionCategory.SERVICES,
                TransactionCategory.OTHER_INCOME
            ),
            FarmOrdering.categoriesFor(TransactionType.INCOME)
        )
    }

    @Test
    fun expenseShowsFeedSuppliesLaborTransportOtherExpense() {
        assertEquals(
            listOf(
                TransactionCategory.FEED,
                TransactionCategory.SUPPLIES,
                TransactionCategory.LABOR,
                TransactionCategory.TRANSPORT,
                TransactionCategory.OTHER_EXPENSE
            ),
            FarmOrdering.categoriesFor(TransactionType.EXPENSE)
        )
    }

    @Test
    fun categoriesAreConstrainedByTheirTransactionType() {
        for (category in FarmOrdering.categoriesFor(TransactionType.INCOME)) {
            assertEquals(TransactionType.INCOME, category.type)
        }
        for (category in FarmOrdering.categoriesFor(TransactionType.EXPENSE)) {
            assertEquals(TransactionType.EXPENSE, category.type)
        }
    }

    @Test
    fun everyCategoryAppearsUnderItsOwnType() {
        for (category in TransactionCategory.values()) {
            assertTrue(
                "Category ${category.name} missing under ${category.type}",
                FarmOrdering.categoriesFor(category.type).contains(category)
            )
        }
    }

    @Test
    fun everyEntryKindIsPresentOnce() {
        assertEquals(listOf(com.susankhya.kisab.domain.FarmEntryKind.LIVESTOCK, com.susankhya.kisab.domain.FarmEntryKind.CROP), FarmOrdering.entryKinds)
    }

    @Test
    fun everyTransactionTypeIsPresentOnce() {
        assertEquals(listOf(TransactionType.INCOME, TransactionType.EXPENSE), FarmOrdering.transactionTypes)
    }

    @Test
    fun everyFinancialPeriodPresetIsPresentOnce() {
        assertEquals(
            listOf(
                com.susankhya.kisab.domain.FinancialPeriodPreset.THIS_MONTH,
                com.susankhya.kisab.domain.FinancialPeriodPreset.LAST_30_DAYS,
                com.susankhya.kisab.domain.FinancialPeriodPreset.ALL_TIME
            ),
            FarmOrdering.financialPeriodPresets
        )
    }

    @Test
    fun everyFarmPlanningCalculatorIsPresentOnceInProductOrder() {
        assertEquals(
            listOf(
                FarmPlanningCalculator.SEED,
                FarmPlanningCalculator.FERTILIZER,
                FarmPlanningCalculator.FEED,
                FarmPlanningCalculator.MILK,
                FarmPlanningCalculator.CROP_YIELD
            ),
            FarmOrdering.farmPlanningCalculators
        )
    }
}
