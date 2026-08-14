package com.susankhya.kisab

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.FinancialPeriodPreset
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.TradeDraft
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import java.time.OffsetDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RC-01 on-device verification of the two release gates that had no dedicated
 * device tests: the M5-05 Farm Financial Overview UI and the M6 Party Hisab UI.
 *
 * Covers period-preset switching (which re-renders all four overview sections
 * and the Hisab reconciliation), the empty states, the selection-survives-
 * recreation restore paths, and the overflow/error path is intentionally left
 * to the existing unit coverage (it only renders a toast).
 */
@RunWith(AndroidJUnit4::class)
class FarmOverviewAndHisabDeviceTest {

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

    @Test
    fun overviewPeriodSwitchReRendersCashAndTradeSections() {
        seedOverviewFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabKitabItem)).perform(click())

            // THIS_MONTH: income is in-window, the 60-day-old expense is not.
            overviewCashIncome(scenario, formattedFor(scenario, 100000))
            overviewCashExpense(scenario, formattedFor(scenario, 0))
            overviewCashNet(scenario, formattedFor(scenario, 100000))
            assertOverviewEmptyState(scenario, R.id.overviewCashEmptyText, View.GONE)

            // Trade + position render in THIS_MONTH too.
            assertViewTextContains(scenario, R.id.overviewSalesText, formattedFor(scenario, 20000))
            assertViewTextContains(scenario, R.id.overviewPurchasesText, formattedFor(scenario, 10000))
            assertOverviewEmptyState(scenario, R.id.overviewTradeEmptyText, View.GONE)
            assertOverviewEmptyState(scenario, R.id.overviewPositionEmptyText, View.GONE)

            // LAST_30_DAYS: the 60-day-old expense is still excluded.
            selectOverviewPeriod(scenario, FinancialPeriodPreset.LAST_30_DAYS)
            overviewCashExpense(scenario, formattedFor(scenario, 0))

            // ALL_TIME: the 60-day-old expense is now included.
            selectOverviewPeriod(scenario, FinancialPeriodPreset.ALL_TIME)
            overviewCashExpense(scenario, formattedFor(scenario, 50000))
            overviewCashNet(scenario, formattedFor(scenario, 50000))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun overviewShowsEmptyStatesForFarmWithoutFacts() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Empty Overview Farm")
            onView(withId(R.id.navHisabKitabItem)).perform(click())
            assertOverviewEmptyState(scenario, R.id.overviewCashEmptyText, View.VISIBLE)
            assertOverviewEmptyState(scenario, R.id.overviewTradeEmptyText, View.VISIBLE)
            assertOverviewEmptyState(scenario, R.id.overviewPositionEmptyText, View.VISIBLE)
            assertOverviewEmptyState(scenario, R.id.overviewTrendEmptyText, View.VISIBLE)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun overviewPeriodSelectionSurvivesRecreation() {
        seedOverviewFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabKitabItem)).perform(click())
            selectOverviewPeriod(scenario, FinancialPeriodPreset.ALL_TIME)
            overviewCashExpense(scenario, formattedFor(scenario, 50000))

            scenario.recreate()

            onView(withId(R.id.navHisabKitabItem)).perform(click())
            overviewCashExpense(scenario, formattedFor(scenario, 50000))
            scenario.onActivity { activity ->
                val spinner = activity.findViewById<android.widget.Spinner>(R.id.overviewPeriodSpinner)
                assertEquals(
                    activity.getString(R.string.period_all_time),
                    spinner.selectedItem as String
                )
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun hisabPartyAndPeriodSwitchingReRendersReconciliation() {
        seedOverviewFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())

            // First party is Alpha Buyer (CUSTOMER): its SALE trade renders.
            assertHisabRole(scenario, roleLabel(com.susankhya.kisab.domain.PartyRole.CUSTOMER))
            assertHisabSales(scenario, formattedFor(scenario, 20000))
            assertHisabToReceive(scenario, formattedFor(scenario, 20000))
            assertHisabEmptyState(scenario, R.id.hisabActivityEmptyText, View.GONE)

            // Switch to Beta Seller (SUPPLIER): its PURCHASE trade renders.
            scenario.onActivity { activity ->
                val spinner = activity.findViewById<android.widget.Spinner>(R.id.hisabPartySpinner)
                val betaIndex = (0 until spinner.count).indexOfFirst {
                    (spinner.getItemAtPosition(it) as String).contains("Beta")
                }
                assertTrue("Beta Seller must be in party choices", betaIndex >= 0)
                spinner.setSelection(betaIndex)
            }
            waitForIdle()
            assertHisabRole(scenario, roleLabel(com.susankhya.kisab.domain.PartyRole.SUPPLIER))
            assertHisabPurchases(scenario, formattedFor(scenario, 10000))
            assertHisabToPay(scenario, formattedFor(scenario, 10000))

            // Period switch re-renders the reconciliation for the supplier.
            selectHisabPeriod(scenario, FinancialPeriodPreset.ALL_TIME)
            assertHisabPurchases(scenario, formattedFor(scenario, 10000))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun hisabSelectionSurvivesRecreation() {
        seedOverviewFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())
            scenario.onActivity { activity ->
                val spinner = activity.findViewById<android.widget.Spinner>(R.id.hisabPartySpinner)
                val betaIndex = (0 until spinner.count).indexOfFirst {
                    (spinner.getItemAtPosition(it) as String).contains("Beta")
                }
                spinner.setSelection(betaIndex)
            }
            selectHisabPeriod(scenario, FinancialPeriodPreset.ALL_TIME)
            waitForIdle()
            assertHisabRole(scenario, roleLabel(com.susankhya.kisab.domain.PartyRole.SUPPLIER))

            scenario.recreate()

            onView(withId(R.id.navHisabItem)).perform(click())
            assertHisabRole(scenario, roleLabel(com.susankhya.kisab.domain.PartyRole.SUPPLIER))
            assertHisabPurchases(scenario, formattedFor(scenario, 10000))
        } finally {
            scenario.close()
        }
    }

    // --- helpers -------------------------------------------------------------

    private fun createFarm(name: String) {
        onView(withId(R.id.farmNameInput)).perform(typeText(name), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())
    }

    private fun waitForIdle() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun selectOverviewPeriod(scenario: ActivityScenario<FarmActivity>, preset: FinancialPeriodPreset) {
        scenario.onActivity { activity ->
            val spinner = activity.findViewById<android.widget.Spinner>(R.id.overviewPeriodSpinner)
            spinner.setSelection(preset.ordinal)
        }
        waitForIdle()
    }

    private fun selectHisabPeriod(scenario: ActivityScenario<FarmActivity>, preset: FinancialPeriodPreset) {
        scenario.onActivity { activity ->
            val spinner = activity.findViewById<android.widget.Spinner>(R.id.hisabPeriodSpinner)
            spinner.setSelection(preset.ordinal)
        }
        waitForIdle()
    }

    private fun formattedFor(scenario: ActivityScenario<FarmActivity>, amountMinor: Long): String {
        var result = ""
        scenario.onActivity { activity -> result = activity.formatMoney("NPR", amountMinor) }
        return result
    }

    private fun roleLabel(role: com.susankhya.kisab.domain.PartyRole): String =
        context.getString(com.susankhya.kisab.ui.FarmLabels.partyRoleRes(role))

    private fun overviewCashIncome(scenario: ActivityScenario<FarmActivity>, expected: String) {
        scenario.onActivity { activity ->
            val line = activity.findViewById<android.widget.TextView>(R.id.overviewCashIncomeText)
            assertTrue("income line [${line.text}] should contain [$expected]", line.text.contains(expected))
        }
    }

    private fun overviewCashExpense(scenario: ActivityScenario<FarmActivity>, expected: String) {
        scenario.onActivity { activity ->
            val line = activity.findViewById<android.widget.TextView>(R.id.overviewCashExpenseText)
            assertTrue("expense line [${line.text}] should contain [$expected]", line.text.contains(expected))
        }
    }

    private fun overviewCashNet(scenario: ActivityScenario<FarmActivity>, expected: String) {
        scenario.onActivity { activity ->
            val line = activity.findViewById<android.widget.TextView>(R.id.overviewCashNetText)
            assertTrue("net line [${line.text}] should contain [$expected]", line.text.contains(expected))
        }
    }

    private fun assertOverviewEmptyState(scenario: ActivityScenario<FarmActivity>, viewId: Int, expected: Int) {
        scenario.onActivity { activity ->
            assertEquals(expected, activity.findViewById<View>(viewId).visibility)
        }
    }

    private fun assertHisabRole(scenario: ActivityScenario<FarmActivity>, expected: String) {
        scenario.onActivity { activity ->
            val line = activity.findViewById<android.widget.TextView>(R.id.hisabPartyRoleText)
            assertTrue("hisab role [${line.text}] should contain [$expected]", line.text.contains(expected))
        }
    }

    private fun assertHisabSales(scenario: ActivityScenario<FarmActivity>, expected: String) {
        assertViewTextContains(scenario, R.id.hisabSalesText, expected)
    }

    private fun assertHisabPurchases(scenario: ActivityScenario<FarmActivity>, expected: String) {
        assertViewTextContains(scenario, R.id.hisabPurchasesText, expected)
    }

    private fun assertHisabToReceive(scenario: ActivityScenario<FarmActivity>, expected: String) {
        assertViewTextContains(scenario, R.id.hisabToReceiveText, expected)
    }

    private fun assertHisabToPay(scenario: ActivityScenario<FarmActivity>, expected: String) {
        assertViewTextContains(scenario, R.id.hisabToPayText, expected)
    }

    private fun assertHisabEmptyState(scenario: ActivityScenario<FarmActivity>, viewId: Int, expected: Int) {
        scenario.onActivity { activity ->
            assertEquals(expected, activity.findViewById<View>(viewId).visibility)
        }
    }

    private fun assertViewTextContains(scenario: ActivityScenario<FarmActivity>, viewId: Int, expected: String) {
        scenario.onActivity { activity ->
            val line = activity.findViewById<android.widget.TextView>(viewId)
            assertTrue("view $viewId [${line.text}] should contain [$expected]", line.text.contains(expected))
        }
    }

    private fun seedOverviewFarm() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Overview Farm", currencyCode = "NPR")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 100000,
                description = "Current milk sale",
                occurredAt = OffsetDateTime.now().toString()
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 50000,
                description = "Old feed purchase",
                occurredAt = OffsetDateTime.now().minusDays(60).toString()
            )
        )
        val buyer = service.addParty(
            farm.id,
            PartyDraft(name = "Alpha Buyer", role = PartyRole.CUSTOMER, contact = "9800000001")
        )
        val seller = service.addParty(
            farm.id,
            PartyDraft(name = "Beta Seller", role = PartyRole.SUPPLIER, contact = "9800000002")
        )
        service.addTrade(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = buyer.id,
                totalMinor = 20000,
                description = "Produce sale to buyer",
                occurredAt = OffsetDateTime.now().toString()
            )
        )
        service.addTrade(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = seller.id,
                totalMinor = 10000,
                description = "Inputs from seller",
                occurredAt = OffsetDateTime.now().toString()
            )
        )
    }
}
