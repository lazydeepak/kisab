package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.TradeDraft
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class FarmKhataRedesignTest {

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

    private fun setupPopulatedFarm(): Pair<FarmSliceService, String> {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Khata Test Farm", currencyCode = "NPR")

        val customer = service.addParty(farm.id, PartyDraft("Ram Customer", PartyRole.CUSTOMER, "9841000001"))
        val supplier = service.addParty(farm.id, PartyDraft("Hari Supplier", PartyRole.SUPPLIER, "9841000002"))
        val settledCustomer = service.addParty(farm.id, PartyDraft("Sita Settled", PartyRole.CUSTOMER, "9841000003"))

        val nowIso = OffsetDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // Customer receivable: sale 1500, paid 500 => toReceive = 1000
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = customer.id,
                totalMinor = 150000L,
                description = "Milk 15L",
                occurredAt = nowIso
            ),
            initialSettlementMinor = 50000L
        )

        // Supplier payable: purchase 2000, unpaid => toPay = 2000
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = supplier.id,
                totalMinor = 200000L,
                description = "Feed 1 Bag",
                occurredAt = nowIso
            ),
            initialSettlementMinor = null
        )

        // Settled customer: sale 500, paid 500 => 0 outstanding
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = settledCustomer.id,
                totalMinor = 50000L,
                description = "Yogurt 2kg",
                occurredAt = nowIso
            ),
            initialSettlementMinor = 50000L
        )

        service.setCurrentFarmId(farm.id)
        return service to farm.id
    }

    @Test
    fun testKhataOverview_displaysDistinctDirectionalTotalsAndAllParties() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // Navigate to Khata
            onView(withId(R.id.navKhataItem)).perform(click())

            // Distinct totals
            onView(allOf(withId(R.id.toReceiveText), isDisplayed())).check(matches(withText(containsString("1,000"))))
            onView(allOf(withId(R.id.toPayText), isDisplayed())).check(matches(withText(containsString("2,000"))))

            // Default filter is All
            onView(withId(R.id.khataFilterAllRadio)).check(matches(isDisplayed()))
            onView(withId(R.id.khataFilterToReceiveRadio)).check(matches(isDisplayed()))
            onView(withId(R.id.khataFilterToPayRadio)).check(matches(isDisplayed()))

            // All parties are shown in list
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Sita Settled"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testKhataFilters_filtersByDirectionCorrectly() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())

            // Select To Receive
            onView(withId(R.id.khataFilterToReceiveRadio)).perform(click())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .check(doesNotExist())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Sita Settled"))))
                .check(doesNotExist())

            // Select To Pay
            onView(withId(R.id.khataFilterToPayRadio)).perform(click())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .check(doesNotExist())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Sita Settled"))))
                .check(doesNotExist())

            // Select All again
            onView(withId(R.id.khataFilterAllRadio)).perform(click())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Sita Settled"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testKhataSearch_filtersPartiesByName() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())

            onView(withId(R.id.khataSearchInput)).perform(replaceText("Hari"))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .check(doesNotExist())

            // Clear search
            onView(withId(R.id.khataSearchInput)).perform(replaceText(""))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testPartyDetail_customerReceivable_showsDirectionalHeadlineAndContextualReceiveButton() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())

            // Click Ram Customer row
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo(), click())

            // Detail header & headline
            onView(withId(R.id.partyKhataTitle)).check(matches(withText("Ram Customer")))
            onView(withId(R.id.partyKhataHeadlineText)).check(matches(withText(containsString("1,000"))))

            // Contextual receive action button visible
            onView(withId(R.id.khataContextualReceiveButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.khataContextualPayButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            // Timeline entries visible in khataEntriesContainer
            onView(allOf(isDescendantOfA(withId(R.id.khataEntriesContainer)), withText(containsString("1,500"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Tap Done to close detail
            onView(withId(R.id.closeKhataButton)).perform(scrollTo(), click())

            // Back on Khata overview
            onView(withId(R.id.khataOverviewContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testPartyDetail_supplierPayable_showsDirectionalHeadlineAndContextualPayButton() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())

            // Click Hari Supplier row
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .perform(scrollTo(), click())

            // Detail header & headline
            onView(withId(R.id.partyKhataTitle)).check(matches(withText("Hari Supplier")))
            onView(withId(R.id.partyKhataHeadlineText)).check(matches(withText(containsString("2,000"))))

            // Contextual pay action button visible
            onView(withId(R.id.khataContextualPayButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.khataContextualReceiveButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            // Close detail
            onView(withId(R.id.closeKhataButton)).perform(scrollTo(), click())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testTodayToKhataContinuity_receivablesButtonNavigatesWithToReceiveFilter() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // On Today screen, tap View Receivables button
            onView(withId(R.id.todayViewReceivablesButton)).perform(scrollTo(), click())

            // Arrives on Khata with To Receive radio checked
            onView(withId(R.id.khataOverviewContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .check(doesNotExist())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testTodayToKhataContinuity_payablesButtonNavigatesWithToPayFilter() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // On Today screen, tap View Payables button
            onView(withId(R.id.todayViewPayablesButton)).perform(scrollTo(), click())

            // Arrives on Khata with To Pay radio checked
            onView(withId(R.id.khataOverviewContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .check(doesNotExist())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testEmptyKhataFarm_showsIntentionalEmptyState() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Empty Khata Farm", currencyCode = "NPR")
        service.setCurrentFarmId(farm.id)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())
            onView(withId(R.id.partiesEmptyText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(withId(R.id.toReceiveText), isDisplayed())).check(matches(withText(containsString("0"))))
            onView(allOf(withId(R.id.toPayText), isDisplayed())).check(matches(withText(containsString("0"))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testKhataRecreationResilience_survivesActivityRecreate() {
        setupPopulatedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())

            // Select To Receive
            onView(withId(R.id.khataFilterToReceiveRadio)).perform(click())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // Recreate activity
            scenario.recreate()

            // Filter state preserved across recreation
            onView(withId(R.id.khataOverviewContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Customer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Hari Supplier"))))
                .check(doesNotExist())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testEmptyKhataFarm_emptyStateHasVisibleAddPartyCta() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Empty Khata Farm", currencyCode = "NPR")
        service.setCurrentFarmId(farm.id)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navKhataItem)).perform(click())

            // Intentional empty state: the message must be visible.
            onView(allOf(withId(R.id.partiesEmptyText), isDisplayed()))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(withId(R.id.partiesEmptyText), isDisplayed()))
                .check(matches(withText(containsString("No parties yet"))))

            // Regression guard: the Add party CTA must remain visible and
            // labeled in the empty state. The khata overview chrome hides the
            // party list and empty-state views when a party khata is open, so
            // the CTA must be kept in sync with the overview chrome visibility.
            onView(allOf(withId(R.id.addPartyButton), isDisplayed()))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(withId(R.id.addPartyButton), isDisplayed()))
                .check(matches(withText(containsString("Add party"))))

            // The party khata action buttons (New Sale / New Purchase) belong
            // to a selected party, so they must be gone when no party exists.
            onView(allOf(withId(R.id.khataNewSaleButton), isDisplayed()))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(allOf(withId(R.id.khataNewPurchaseButton), isDisplayed()))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        } finally {
            scenario.close()
        }
    }
}
