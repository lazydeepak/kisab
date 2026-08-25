package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmProduct
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.domain.ProductionAllocationDraft
import com.susankhya.kisab.domain.ProductionAllocationType
import com.susankhya.kisab.domain.ProductionRecordDraft
import com.susankhya.kisab.domain.ProductionSession
import com.susankhya.kisab.domain.SupplyUsageDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class FarmWorkRedesignTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
    }

    @After
    fun tearDown() {
        SharedPreferencesFarmStore(context).clear()
    }

    private fun setupPopulatedFarm(): Triple<FarmSliceService, String, FarmProduct> {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Farm Work Test Farm", currencyCode = "NPR")

        val milk = service.addProduct(farm.id, "Cow Milk", ProductUnit.LITRE)
        val feed = service.addSupply(farm.id, "Dairy Feed", ProductUnit.BAG)

        val customer = service.addParty(farm.id, PartyDraft("Dairy Customer", PartyRole.CUSTOMER, "9800000001"))
        val now = OffsetDateTime.now(ZoneId.systemDefault())
        val nowIso = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // 1. Morning production 30 L + Evening production 20 L = 50 L total
        service.addProductionRecord(
            farm.id,
            ProductionRecordDraft(milk.id, BigDecimal("30"), ProductUnit.LITRE, nowIso, ProductionSession.MORNING),
            ZoneId.systemDefault()
        )
        service.addProductionRecord(
            farm.id,
            ProductionRecordDraft(milk.id, BigDecimal("20"), ProductUnit.LITRE, nowIso, ProductionSession.EVENING),
            ZoneId.systemDefault()
        )

        // 2. Product sale: 25 L sold
        service.addProductSale(
            farm.id,
            customer.id,
            milk.id,
            BigDecimal("25"),
            10000L,
            250000L,
            nowIso
        )

        // 3. Allocation: 5 L home use + 10 L processing = 15 L allocated => 10 L unexplained
        service.addProductionAllocation(
            farm.id,
            ProductionAllocationDraft(milk.id, BigDecimal("5"), ProductUnit.LITRE, nowIso, ProductionAllocationType.HOME_USE),
            ZoneId.systemDefault()
        )
        service.addProductionAllocation(
            farm.id,
            ProductionAllocationDraft(milk.id, BigDecimal("10"), ProductUnit.LITRE, nowIso, ProductionAllocationType.PROCESSING),
            ZoneId.systemDefault()
        )

        // 4. Supplies: Buy 10 bags feed, use 2 bags => 8 bags remaining
        service.addSupplyPurchase(
            farm.id,
            feed.id,
            BigDecimal("10"),
            ProductUnit.BAG,
            4000000L,
            TransactionCategory.SUPPLIES,
            nowIso,
            "10 bags feed"
        )
        service.addSupplyUsage(
            farm.id,
            SupplyUsageDraft(feed.id, BigDecimal("2"), ProductUnit.BAG, nowIso, "2 bags used")
        )

        service.setCurrentFarmId(farm.id)
        return Triple(service, farm.id, milk)
    }

    @Test
    fun testEmptyFarmWork_showsIntentionalEmptyStates() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Empty Farm", currencyCode = "NPR")
        service.setCurrentFarmId(farm.id)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navFarmWorkItem)).perform(click())

            onView(withId(R.id.farmWorkNoProductsText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.farmWorkNoSuppliesText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.farmWorkRecordProductionButton)).check(matches(isDisplayed()))
            onView(withId(R.id.farmWorkBuySupplyButton)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testPopulatedProduction_showsSessionsEquationAndUnexplained() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navFarmWorkItem)).perform(click())

            // Production section contains Cow Milk card
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("Cow Milk"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Today total headline: 50
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("50"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Morning & Evening session summary
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("30"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("20"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Sold breakdown: 25
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("25"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Unexplained breakdown tile
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString(context.getString(R.string.farm_work_explain_production_action)))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testPopulatedSupplies_showsRemainingAndMovementSummary() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navFarmWorkItem)).perform(click())

            // Supplies section contains Dairy Feed card
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkSuppliesContainer)), withText(containsString("Dairy Feed"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Remaining quantity: 8
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkSuppliesContainer)), withText(containsString("8"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Movement summary
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkSuppliesContainer)), withText(containsString("10"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testTodayToFarmWorkContinuity_navigatesToFarmWork() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // On Today screen, tap View Farm Work button
            onView(withId(R.id.todayViewFarmWorkButton)).perform(scrollTo(), click())

            // Lands on Farm Work with production & supplies
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("Cow Milk"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkSuppliesContainer)), withText(containsString("Dairy Feed"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testFarmWorkRecreationResilience_survivesActivityRecreate() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navFarmWorkItem)).perform(click())

            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("Cow Milk"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Recreate activity
            scenario.recreate()

            // Still on Farm Work and content remains displayed
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("Cow Milk"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkSuppliesContainer)), withText(containsString("Dairy Feed"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}
