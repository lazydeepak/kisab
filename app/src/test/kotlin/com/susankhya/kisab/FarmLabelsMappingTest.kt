package com.susankhya.kisab

import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmPlanningCalculator
import com.susankhya.kisab.domain.ArithmeticOperation
import com.susankhya.kisab.domain.LandUnit
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.ui.FarmLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies every domain enum value has a distinct UI string-resource mapping.
 * The mappings in FarmLabels are exhaustive `when` expressions, so a future
 * enum value without a mapping also fails compilation.
 */
class FarmLabelsMappingTest {

    @Test
    fun everyFarmEntryKindHasADistinctMapping() {
        val values = FarmEntryKind.values()
        val resourceIds = values.map { FarmLabels.entryKindRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }

    @Test
    fun everyTransactionTypeHasADistinctMapping() {
        val values = TransactionType.values()
        val resourceIds = values.map { FarmLabels.transactionTypeRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }

    @Test
    fun everyTransactionCategoryHasADistinctMapping() {
        val values = TransactionCategory.values()
        val resourceIds = values.map { FarmLabels.transactionCategoryRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }

    @Test
    fun everyFinancialPeriodPresetHasADistinctMapping() {
        val values = com.susankhya.kisab.domain.FinancialPeriodPreset.values()
        val resourceIds = values.map { FarmLabels.financialPeriodPresetRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }

    @Test
    fun everyArithmeticOperationHasADistinctMapping() {
        val values = ArithmeticOperation.values()
        val resourceIds = values.map { FarmLabels.arithmeticOperationRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }

    @Test
    fun everyLandUnitHasADistinctMapping() {
        val values = LandUnit.values()
        val resourceIds = values.map { FarmLabels.landUnitRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }

    @Test
    fun everyFarmPlanningCalculatorHasADistinctMapping() {
        val values = FarmPlanningCalculator.values()
        val resourceIds = values.map { FarmLabels.farmPlanningCalculatorRes(it) }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(values.size, resourceIds.toSet().size)
    }
}
