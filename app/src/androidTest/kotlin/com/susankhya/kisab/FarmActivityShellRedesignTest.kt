package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
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
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.domain.TradeDraft
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import java.time.OffsetDateTime
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Focused UX-01 tests verifying the new application shell:
 * - 5-item bottom navigation: Today | Khata | + Record | Farm Work | More
 * - Record is an action opening a 6-verb sheet, never a destination or selected item
 * - Record verbs route into existing operational workflows
 * - Top-level Farm Work and More destinations
 * - Back-stack hierarchy and state preservation across recreation
 */
@RunWith(AndroidJUnit4::class)
class FarmActivityShellRedesignTest {

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

    private fun seedFullFarm(name: String) {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm(name, currencyCode = "NPR")
        service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        service.addSupply(farm.id, "Feed", ProductUnit.KILOGRAM)
        val customer = service.addParty(farm.id, PartyDraft(name = "Customer One", role = PartyRole.CUSTOMER, contact = "9800000001"))
        val supplier = service.addParty(farm.id, PartyDraft(name = "Supplier One", role = PartyRole.SUPPLIER, contact = "9800000002"))
        service.addTrade(farm.id, TradeDraft(type = TradeType.SALE, partyId = customer.id, totalMinor = 5000, description = "Milk sale", occurredAt = OffsetDateTime.now().toString()))
        service.addTrade(farm.id, TradeDraft(type = TradeType.PURCHASE, partyId = supplier.id, totalMinor = 3000, description = "Feed purchase", occurredAt = OffsetDateTime.now().toString()))
    }

    @Test
    fun shellPresentsFiveBottomNavItemsWithTodaySelectedByDefault() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navTodayItem)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navKhataItem)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navRecordItem)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navFarmWorkItem)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navMoreItem)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.navTodayItem)).check(matches(isSelected()))
            onView(withId(R.id.navKhataItem)).check(matches(not(isSelected())))
            onView(withId(R.id.navRecordItem)).check(matches(not(isSelected())))
            onView(withId(R.id.navFarmWorkItem)).check(matches(not(isSelected())))
            onView(withId(R.id.navMoreItem)).check(matches(not(isSelected())))

            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recordButtonOpensActionSheetAndIsNeverSelectedDestination() {
        seedFullFarm("Record Farm")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())
            onView(withId(R.id.navKhataItem)).check(matches(isSelected()))

            // Record button click opens sheet dialog
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withText(R.string.record_sheet_title)).inRoot(isDialog()).check(matches(isDisplayed()))

            // Dismissing returns cleanly to Khata, Record was never selected
            onView(withId(R.id.recordSheetCancelButton)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navKhataItem)).check(matches(isSelected()))
            onView(withId(R.id.navRecordItem)).check(matches(not(isSelected())))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recordSheetVerbsRouteToExistingWorkflows() {
        seedFullFarm("Workflows Farm")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // 1. Sell / बेचेँ
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetSellButton)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.quick_sale_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            // 2. Received money / पैसा पाएँ
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetReceivedMoneyButton)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.received_money_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            // 3. Bought / किनेँ
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetBoughtButton)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.supply_purchase_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            // 4. Used / प्रयोग गरेँ
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetUsedButton)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.supply_usage_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            // 5. Paid money / पैसा तिरेँ
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetPaidMoneyButton)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.supplier_payment_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            // 6. Production / उत्पादन
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetProductionButton)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.production_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun farmWorkDestinationExposesProductionAndSupplies() {
        seedFullFarm("Farm Work Test")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navFarmWorkItem)).perform(click())
            onView(withId(R.id.farmWorkScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.shellTitle)).check(matches(withText(R.string.nav_farm_work)))

            // Production actions
            onView(withId(R.id.farmWorkProductionButton)).perform(click())
            onView(withText(R.string.production_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.farmWorkAllocationButton)).perform(click())
            onView(withText(R.string.production_allocate_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            // Supplies actions
            onView(withId(R.id.farmWorkBoughtButton)).perform(click())
            onView(withText(R.string.supply_purchase_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.farmWorkUsedButton)).perform(click())
            onView(withText(R.string.supply_usage_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.farmWorkRemainingButton)).perform(click())
            onView(withText(R.string.supply_stock_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun moreDestinationRoutesToManagementAndTools() {
        seedFullFarm("More Test Farm")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.shellTitle)).check(matches(withText(R.string.nav_more)))

            // Hisab tools
            onView(withId(R.id.moreHisabButton)).perform(click())
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Farms list
            onView(withId(R.id.moreFarmsButton)).perform(click())
            onView(withId(R.id.farmsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Settings
            onView(withId(R.id.moreSettingsButton)).perform(click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // About
            onView(withId(R.id.moreAboutButton)).perform(click())
            onView(withText(R.string.dialog_about_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backNavigationRestoresTodayFromPrimaryTabs() {
        seedFullFarm("Back Test Farm")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.navFarmWorkItem)).perform(click())
            onView(withId(R.id.farmWorkScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun newDestinationsSurviveRecreation() {
        seedFullFarm("Recreation Farm")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navFarmWorkItem)).perform(click())
            onView(withId(R.id.navFarmWorkItem)).check(matches(isSelected()))

            scenario.recreate()

            onView(withId(R.id.farmWorkScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navFarmWorkItem)).check(matches(isSelected()))
            onView(withId(R.id.navTodayItem)).check(matches(not(isSelected())))

            onView(withId(R.id.navMoreItem)).perform(click())
            scenario.recreate()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.navMoreItem)).check(matches(isSelected()))
        } finally {
            scenario.close()
        }
    }
}
