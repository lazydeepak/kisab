package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.LocalUserService
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.domain.ProductionRecordDraft
import com.susankhya.kisab.domain.ProductionSession
import com.susankhya.kisab.domain.TradeDraft
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.persistence.SharedPreferencesLocalUserStore
import com.susankhya.kisab.ui.FarmActivity
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Focused tests verifying the UX-02 Today dashboard redesign:
 * - App bar farm identity and quick switcher
 * - Date header and Month overview
 * - Today's activity hero (production, sales, received money, expenses, credit sales)
 * - Money needing attention: distinct directional लिन बाँकी / तिर्न बाँकी with Khata shortcuts
 * - Farm status: production output + supplies remaining with Farm Work shortcut
 * - Recent activity stream
 * - Intentional empty vs populated states
 * - Destination navigation and recreation resilience
 */
@RunWith(AndroidJUnit4::class)
class FarmTodayDashboardRedesignTest {

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

    private fun createFarm(name: String) {
        onView(withId(R.id.farmNameInput)).perform(typeText(name), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())
    }

    @Test
    fun emptyFarmRendersIntentionalEmptyStatesWithoutCardSoup() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Clean Farm")

            // Default destination is Today
            onView(withId(R.id.navTodayItem)).check(matches(isSelected()))
            onView(withId(R.id.shellTitle)).check(matches(withText("Clean Farm")))
            onView(withId(R.id.shellFarmSwitchIcon)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Today Hero empty state visible
            onView(withId(R.id.todayEmptyStateText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayProductionContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.todayCreditSalesContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            // Money attention: all settled text visible, directional cards hidden
            onView(withId(R.id.todayKhataSettledText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayReceivableContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.todayPayableContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            // Farm status: no status text visible
            onView(withId(R.id.todayNoFarmStatusText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayProductionStatusText)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.todaySuppliesStatusText)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun populatedFarmRendersTodayHeroAndDirectionalKhataCards() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Populated Farm", currencyCode = "NPR")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        val feed = service.addSupply(farm.id, "Feed", ProductUnit.BAG)

        val customer = service.addParty(farm.id, PartyDraft("Ram Customer", PartyRole.CUSTOMER, "9800000001"))
        val supplier = service.addParty(farm.id, PartyDraft("Hari Supplier", PartyRole.SUPPLIER, "9800000002"))

        val now = OffsetDateTime.now(ZoneId.systemDefault())

        // 1. Record production: 50 L
        service.addProductionRecord(
            farm.id,
            ProductionRecordDraft(
                productId = milk.id,
                quantity = BigDecimal("50"),
                unit = ProductUnit.LITRE,
                occurredAt = now.toString(),
                session = ProductionSession.MORNING
            ),
            ZoneId.systemDefault()
        )

        // 2. Record sale: 40 L for NPR 4000 (rate 100), 3000 paid, 1000 credit
        service.addProductSale(
            farm.id,
            partyId = customer.id,
            productId = milk.id,
            quantity = BigDecimal("40"),
            rateMinor = 10000,
            initialPaymentMinor = 300000,
            occurredAt = now.toString()
        )

        // 3. Record supply purchase: 10 bags for NPR 5000, unpaid (so toPay = 5000)
        service.addTrade(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = supplier.id,
                totalMinor = 500000,
                description = "Feed purchase",
                occurredAt = now.toString()
            )
        )
        service.addSupplyPurchase(
            farm.id,
            supplyId = feed.id,
            quantity = BigDecimal("10"),
            unit = ProductUnit.BAG,
            amountMinor = 500000,
            category = TransactionCategory.FEED,
            occurredAt = now.toString(),
            description = "Feed purchase"
        )

        // 4. Record a farm expense: 500
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.OTHER_EXPENSE,
                amountMinor = 50000,
                description = "Vet visit",
                occurredAt = now.toString()
            )
        )

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // Production headline is visible
            onView(withId(R.id.todayProductionContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayProductionHeadlineText)).check(matches(withText(containsString("Milk"))))

            // Money metrics populated
            onView(withId(R.id.todaySalesValueText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayReceivedValueText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayExpensesValueText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Money attention: Directional cards for both Receivable (NPR 1000) and Payable (NPR 5000)
            onView(withId(R.id.todayKhataSettledText)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.todayReceivableContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayPayableContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Farm Status Card
            onView(withId(R.id.todayProductionStatusText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todaySuppliesStatusText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todaySuppliesStatusText)).check(matches(withText(containsString("Feed"))))

            // Recent activity contains our vet transaction
            onView(withText(containsString("Vet visit"))).perform(scrollTo()).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun moneyAttentionButtonsRouteDirectlyToKhata() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Khata Route Farm", currencyCode = "NPR")
        val customer = service.addParty(farm.id, PartyDraft("Buyer", PartyRole.CUSTOMER, "9800000001"))
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)

        service.addProductSale(
            farm.id,
            partyId = customer.id,
            productId = milk.id,
            quantity = BigDecimal("10"),
            rateMinor = 10000,
            initialPaymentMinor = 0,
            occurredAt = OffsetDateTime.now().toString()
        )

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.todayViewReceivablesButton)).perform(click())

            // Successfully routed to Khata destination
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navKhataItem)).check(matches(isSelected()))
            onView(withId(R.id.navTodayItem)).check(matches(not(isSelected())))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun farmStatusButtonRoutesDirectlyToFarmWork() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Status Route Farm", currencyCode = "NPR")
        service.addProduct(farm.id, "Milk", ProductUnit.LITRE)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.todayViewFarmWorkButton)).perform(click())

            // Successfully routed to Farm Work destination
            onView(withId(R.id.farmWorkScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navFarmWorkItem)).check(matches(isSelected()))
            onView(withId(R.id.navTodayItem)).check(matches(not(isSelected())))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun appBarFarmSwitcherSwitchesActiveFarm() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val userService = LocalUserService(SharedPreferencesLocalUserStore(context))
        val farm1 = service.createFarm("Alpha Dairy", currencyCode = "NPR")
        val farm2 = service.createFarm("Beta Poultry", currencyCode = "USD")
        userService.associateFarm(farm1.id)
        userService.associateFarm(farm2.id)
        service.setCurrentFarmId(farm1.id)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.shellTitle)).check(matches(withText("Alpha Dairy")))

            // Tap farm switcher in app bar
            onView(withId(R.id.shellFarmSwitchIcon)).perform(click())
            onView(withText(R.string.farm_switcher_dialog_title)).inRoot(isDialog()).check(matches(isDisplayed()))

            // Select Beta Poultry (position 1)
            onData(anything()).inRoot(isDialog()).atPosition(1).perform(click())

            // Active farm switched immediately on Today
            onView(withId(R.id.shellTitle)).check(matches(withText("Beta Poultry")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun monthOverviewButtonOpensMonthlySummaryDialog() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Month Test Farm")

            onView(withId(R.id.farmerOverviewMonthButton)).perform(click())
            onView(withText(R.string.farmer_overview_month_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun todayDashboardSurvivesRecreation() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Persist Farm", currencyCode = "NPR")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)

        service.addProductionRecord(
            farm.id,
            ProductionRecordDraft(
                productId = milk.id,
                quantity = BigDecimal("25"),
                unit = ProductUnit.LITRE,
                occurredAt = OffsetDateTime.now().toString(),
                session = ProductionSession.EVENING
            ),
            ZoneId.systemDefault()
        )

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.todayProductionHeadlineText)).check(matches(withText(containsString("Milk"))))

            scenario.recreate()

            onView(withId(R.id.todayProductionHeadlineText)).check(matches(withText(containsString("Milk"))))
            onView(withId(R.id.shellTitle)).check(matches(withText("Persist Farm")))
            onView(withId(R.id.navTodayItem)).check(matches(isSelected()))
        } finally {
            scenario.close()
        }
    }
}
