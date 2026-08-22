package com.susankhya.kisab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FarmActivityServiceTest {
    private lateinit var service: FarmSliceService

    @Before
    fun setUp() {
        service = FarmSliceService(InMemoryFarmStore())
    }

    @Test
    fun createFarmOrdersActivitiesByDisplayOrder() {
        val farm = service.createFarm(
            "Mixed",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)
        )
        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.POULTRY), farm.activities)
    }

    @Test
    fun createFarmDeduplicatesAndOrdersActivities() {
        val farm = service.createFarm(
            "Bad",
            activities = listOf(FarmActivityType.CROPS, FarmActivityType.CROPS, FarmActivityType.POULTRY)
        )
        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.POULTRY), farm.activities)
    }

    @Test
    fun setFarmActivitiesDisablesOnlyActivitiesWithHistory() {
        val farm = service.createFarm(
            "Mixed",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 900,
                description = "Goat feed",
                occurredAt = "2024-02-03T12:00:00Z",
                activity = FarmActivityType.GOAT_SHEEP
            )
        )

        service.setFarmActivities(farm.id, setOf(FarmActivityType.POULTRY))

        val updated = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.POULTRY), updated.activities)
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), updated.disabledActivities)
        assertEquals(1, updated.transactions.size)
        assertEquals(FarmActivityType.GOAT_SHEEP, updated.transactions[0].activity)
    }

    @Test
    fun setFarmActivitiesDropsActivitiesWithoutHistory() {
        val farm = service.createFarm(
            "Mixed",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.setFarmActivities(farm.id, setOf(FarmActivityType.GOAT_SHEEP))

        val updated = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), updated.activities)
        assertTrue(updated.disabledActivities.isEmpty())
    }

    @Test
    fun disableFarmActivityThenReEnableKeepsHistoryAndTotals() {
        val farm = service.createFarm(
            "Poultry Farm",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 900,
                description = "Goat feed",
                occurredAt = "2024-02-03T12:00:00Z",
                activity = FarmActivityType.GOAT_SHEEP
            )
        )

        service.disableFarmActivity(farm.id, FarmActivityType.GOAT_SHEEP)
        val disabled = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), disabled.disabledActivities)
        assertEquals(900L, service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.GOAT_SHEEP }.expenseMinor)

        service.reEnableFarmActivity(farm.id, FarmActivityType.GOAT_SHEEP)
        val reEnabled = service.loadFarm(farm.id)!!
        assertEquals(
            listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP),
            reEnabled.activities
        )
        assertTrue(reEnabled.disabledActivities.isEmpty())
        assertEquals(1, reEnabled.transactions.size)
        assertEquals(900L, service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.GOAT_SHEEP }.expenseMinor)
    }

    @Test
    fun farmActivityBreakdownIsStableAcrossDisableReEnable() {
        val farm = service.createFarm(
            "Dairy",
            activities = listOf(FarmActivityType.CATTLE_BUFFALO_DAIRY)
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 10000,
                description = "Milk sale",
                occurredAt = "2024-05-01T12:00:00Z",
                activity = FarmActivityType.CATTLE_BUFFALO_DAIRY
            )
        )

        val before = service.farmActivityBreakdown(farm.id)
        service.disableFarmActivity(farm.id, FarmActivityType.CATTLE_BUFFALO_DAIRY)
        val after = service.farmActivityBreakdown(farm.id)

        assertEquals(before, after)
    }

    @Test
    fun addFarmActivityAddsToRunningSet() {
        val farm = service.createFarm("Farm")
        service.addFarmActivity(farm.id, FarmActivityType.FISHERY)
        assertEquals(listOf(FarmActivityType.FISHERY), service.loadFarm(farm.id)!!.activities)
    }
}