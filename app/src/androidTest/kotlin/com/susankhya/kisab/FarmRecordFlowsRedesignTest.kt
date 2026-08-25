package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmProduct
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.ProductUnit
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

@RunWith(AndroidJUnit4::class)
class FarmRecordFlowsRedesignTest {

    private lateinit var context: Context
    private lateinit var service: FarmSliceService
    private var testFarmId: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        service = FarmSliceService(SharedPreferencesFarmStore(context))
        val farm = service.createFarm("Record Flow Test Farm", currencyCode = "NPR")
        testFarmId = farm.id
        service.setCurrentFarmId(farm.id)
    }

    @After
    fun tearDown() {
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun testQuickSale_fullCashFlow() {
        val farmId = testFarmId!!
        val milk = service.addProduct(farmId, "Cow Milk", ProductUnit.LITRE)
        val customer = service.addParty(farmId, PartyDraft("Customer Cash", PartyRole.CUSTOMER, "9801111111"))

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetSellButton)).inRoot(isDialog()).perform(click())

            onView(withHint(R.string.quick_sale_quantity)).inRoot(isDialog()).perform(replaceText("10"), closeSoftKeyboard())
            onView(withHint(R.string.quick_sale_rate)).inRoot(isDialog()).perform(replaceText("80"), closeSoftKeyboard())

            onView(withText(R.string.quick_sale_save)).inRoot(isDialog()).perform(click())

            // Sell again dialog shown
            onView(withText(R.string.quick_sale_saved)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())

            // Total sale = 800 recorded
            val farm = service.loadFarm(farmId)!!
            assert(farm.trades.isNotEmpty())
            assert(farm.trades.first().totalMinor == 80000L)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testQuickSale_creditFlowCreatesReceivable() {
        val farmId = testFarmId!!
        val milk = service.addProduct(farmId, "Buffalo Milk", ProductUnit.LITRE)
        val customer = service.addParty(farmId, PartyDraft("Customer Credit", PartyRole.CUSTOMER, "9802222222"))

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetSellButton)).inRoot(isDialog()).perform(click())

            onView(withHint(R.string.quick_sale_quantity)).inRoot(isDialog()).perform(replaceText("5"), closeSoftKeyboard())
            onView(withHint(R.string.quick_sale_rate)).inRoot(isDialog()).perform(replaceText("100"), closeSoftKeyboard())

            // Select Credit
            onView(withText(R.string.quick_sale_credit)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.quick_sale_save)).inRoot(isDialog()).perform(click())

            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())

            // 500 receivable created for Customer Credit
            val summary = service.partyLedgerSummary(farmId, customer.id)
            assert(summary.toReceiveMinor == 50000L)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testReceivedMoney_fullAmountShortcutSavesPayment() {
        val farmId = testFarmId!!
        val milk = service.addProduct(farmId, "Goat Milk", ProductUnit.LITRE)
        val customer = service.addParty(farmId, PartyDraft("Customer Owe", PartyRole.CUSTOMER, "9803333333"))

        // Create initial sale of 1000 credit
        service.addProductSale(farmId, customer.id, milk.id, BigDecimal("10"), 10000L, 0L, "2026-08-18T00:00:00Z")

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetReceivedMoneyButton)).inRoot(isDialog()).perform(click())

            // Tap full amount shortcut
            onView(withText(R.string.received_money_full_amount)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.received_money_save)).inRoot(isDialog()).perform(click())

            // Ledger settled
            val summary = service.partyLedgerSummary(farmId, customer.id)
            assert(summary.toReceiveMinor == 0L)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testSupplyUsage_recordsUsageAndDeductsStock() {
        val farmId = testFarmId!!
        val feed = service.addSupply(farmId, "Poultry Feed", ProductUnit.BAG)
        service.addSupplyPurchase(farmId, feed.id, BigDecimal("20"), ProductUnit.BAG, 10000000L, TransactionCategory.SUPPLIES, "2026-08-18T00:00:00Z", "Purchase")

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetUsedButton)).inRoot(isDialog()).perform(click())

            onView(withHint(R.string.supply_quantity)).inRoot(isDialog()).perform(replaceText("5"), closeSoftKeyboard())
            onView(withText(R.string.action_ok)).inRoot(isDialog()).perform(click())

            // 15 remaining
            val remaining = service.supplyAvailable(farmId, feed.id)
            assert(remaining.compareTo(BigDecimal("15")) == 0)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testProduction_recordsSessionProduction() {
        val farmId = testFarmId!!
        val milk = service.addProduct(farmId, "Morning Cow Milk", ProductUnit.LITRE)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetProductionButton)).inRoot(isDialog()).perform(click())

            onView(withHint(R.string.production_quantity)).inRoot(isDialog()).perform(replaceText("40"), closeSoftKeyboard())
            onView(withText(R.string.production_morning)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.production_save)).inRoot(isDialog()).perform(click())

            val farm = service.loadFarm(farmId)!!
            assert(farm.productionRecords.isNotEmpty())
            assert(farm.productionRecords.first().quantity.compareTo(BigDecimal("40")) == 0)
        } finally {
            scenario.close()
        }
    }
}
