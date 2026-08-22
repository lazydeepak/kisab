package com.susankhya.kisab.domain

import com.susankhya.kisab.ui.FarmOrdering
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmActivityCatalogTest {

    @Test
    fun displayOrderIsCanonicalAndCoversEveryActivity() {
        assertEquals(
            FarmActivityType.values().toList(),
            FarmActivityCatalog.displayOrder.sortedBy { it.ordinal }
        )
        assertEquals(FarmActivityType.values().size, FarmActivityCatalog.displayOrder.size)
    }

    @Test
    fun orderedCategoriesForGeneralFarmMatchesCanonicalCategories() {
        assertEquals(
            FarmOrdering.categoriesFor(TransactionType.EXPENSE),
            FarmActivityCatalog.orderedCategories(emptySet(), TransactionType.EXPENSE)
        )
        assertEquals(
            FarmOrdering.categoriesFor(TransactionType.INCOME),
            FarmActivityCatalog.orderedCategories(emptySet(), TransactionType.INCOME)
        )
    }

    @Test
    fun orderedCategoriesPrioritizeRelevantExpenseCategories() {
        val poultry = setOf(FarmActivityType.POULTRY)
        val ordered = FarmActivityCatalog.orderedCategories(poultry, TransactionType.EXPENSE)
        assertEquals(TransactionCategory.FEED, ordered.first())
        assertEquals(
            FarmOrdering.categoriesFor(TransactionType.EXPENSE).toSet(),
            ordered.toSet()
        )
    }

    @Test
    fun orderedCategoriesNeverNarrowIncomeChoices() {
        val dairy = setOf(FarmActivityType.CATTLE_BUFFALO_DAIRY)
        assertEquals(
            FarmOrdering.categoriesFor(TransactionType.INCOME),
            FarmActivityCatalog.orderedCategories(dairy, TransactionType.INCOME)
        )
    }

    @Test
    fun orderedCalculatorsPrioritizeRelevantAndKeepAll() {
        val crop = setOf(FarmActivityType.CROPS)
        val ordered = FarmActivityCatalog.orderedCalculators(crop)
        assertEquals(FarmPlanningCalculator.SEED, ordered.first())
        assertEquals(
            FarmOrdering.farmPlanningCalculators.toSet(),
            ordered.toSet()
        )
    }

    @Test
    fun orderedCalculatorsForDairyLeadWithFeedAndMilk() {
        val dairy = setOf(FarmActivityType.CATTLE_BUFFALO_DAIRY)
        val ordered = FarmActivityCatalog.orderedCalculators(dairy)
        assertTrue(ordered.indexOf(FarmPlanningCalculator.FEED) < ordered.indexOf(FarmPlanningCalculator.SEED))
        assertTrue(ordered.indexOf(FarmPlanningCalculator.MILK) < ordered.indexOf(FarmPlanningCalculator.SEED))
    }

    @Test
    fun activityChoicesAlwaysOfferGeneralFirst() {
        val choices = FarmActivityCatalog.activityChoices(
            setOf(FarmActivityType.POULTRY, FarmActivityType.CROPS),
            currentActivity = null
        )
        assertEquals(null, choices.first())
        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.POULTRY), choices.drop(1))
    }

    @Test
    fun activityChoicesAppendDisabledCurrentActivityForEditing() {
        val choices = FarmActivityCatalog.activityChoices(
            activities = setOf(FarmActivityType.CROPS),
            currentActivity = FarmActivityType.POULTRY
        )
        assertEquals(FarmActivityType.POULTRY, choices.last())
    }

    @Test
    fun farmActivityBreakdownReconcilesWithFarmTotals() {
        val transactions = listOf(
            transaction(amountMinor = 1000, activity = FarmActivityType.POULTRY),
            transaction(amountMinor = 500, activity = FarmActivityType.POULTRY),
            transaction(amountMinor = 8000, activity = FarmActivityType.CROPS),
            transaction(amountMinor = 2500, activity = null),
            transaction(amountMinor = 1200, activity = FarmActivityType.POULTRY)
        )
        val breakdown = farmActivityBreakdown(transactions)

        val poultry = breakdown.first { it.activity == FarmActivityType.POULTRY }
        assertEquals(2700L, poultry.incomeMinor)
        assertEquals(0L, poultry.expenseMinor)
        assertEquals(2700L, poultry.balanceMinor)

        val general = breakdown.last()
        assertEquals(null, general.activity)
        assertEquals(2500L, general.incomeMinor)

        val totals = FarmTotals.of(transactions)
        assertEquals(totals.incomeMinor, breakdown.sumOf { it.incomeMinor })
        assertEquals(totals.expensesMinor, breakdown.sumOf { it.expenseMinor })
        assertEquals(totals.balanceMinor, breakdown.sumOf { it.balanceMinor })
    }

    @Test
    fun farmActivityBreakdownOrdersKnownActivitiesThenGeneralLast() {
        val transactions = listOf(
            transaction(amountMinor = 1, activity = FarmActivityType.OTHER),
            transaction(amountMinor = 1, activity = FarmActivityType.CROPS),
            transaction(amountMinor = 1, activity = null)
        )
        val names = farmActivityBreakdown(transactions).map { it.activity }
        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.OTHER, null), names)
    }

    @Test
    fun farmActivityBreakdownReturnsEmptyWhenNoTransactions() {
        assertTrue(farmActivityBreakdown(emptyList()).isEmpty())
    }

    private fun transaction(amountMinor: Long, activity: FarmActivityType?): FarmTransaction =
        FarmTransaction(
            id = "tx-${amountMinor}-${activity?.name ?: "general"}",
            type = TransactionType.INCOME,
            category = TransactionCategory.SALES,
            amountMinor = amountMinor,
            description = "record",
            occurredAt = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
            activity = activity
        )
}