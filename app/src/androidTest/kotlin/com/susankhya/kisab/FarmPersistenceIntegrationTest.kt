package com.susankhya.kisab

import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmPersistenceIntegrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun persistsFarmAcrossRecreatedStoreAndActivity() {
        val firstStore = SharedPreferencesFarmStore(context)
        val firstService = FarmSliceService(firstStore)
        val farm = firstService.createFarm("Demo Farm")
        firstService.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 2))
        firstService.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 1500,
                description = "Feed",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        firstService.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 3000,
                description = "Sale",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        val recreatedStore = SharedPreferencesFarmStore(context)
        val recreatedService = FarmSliceService(recreatedStore)
        val reloaded = recreatedService.loadFarm(farm.id)
        val summary = recreatedService.summary(farm.id)

        assertEquals("Demo Farm", reloaded?.name)
        assertEquals(1, reloaded?.entries?.size)
        assertEquals(2, reloaded?.transactions?.size)
        assertEquals(1500, summary.balanceMinor)
        assertEquals("NPR", summary.currencyCode)
    }

    @Test
    fun createsFarmAndRecreatesActivityWithoutLosingState() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)

        onView(withId(R.id.farmNameInput)).perform(typeText("Demo Farm"), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())
        onView(withId(R.id.farmNameText)).check(matches(withText("Demo Farm")))

        onView(withId(R.id.farmToolsToggleButton)).perform(scrollTo(), click())
        onView(withId(R.id.entryLabelInput)).perform(typeText("Goat"), closeSoftKeyboard())
        onView(withId(R.id.entryQuantityInput)).perform(typeText("3"), closeSoftKeyboard())
        onView(withId(R.id.addEntryButton)).perform(scrollTo(), click())

        onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
        onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText("8000"), closeSoftKeyboard())
        onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText("Egg sale"), closeSoftKeyboard())
        PickerTestHelpers.pickDateTime(2024, 0, 2, 12, 0)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.recreate()

        var entryCount: String? = null
        var balance: String? = null
        scenario.onActivity { activity ->
            entryCount = activity.formatCount(1)
            balance = activity.formatMoney("NPR", 800000L)
        }
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Entry count: $entryCount"))))
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Balance: $balance"))))
    }

    @Test
    fun transactionsRenderNewestFirstInHistory() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)

        onView(withId(R.id.farmNameInput)).perform(typeText("Order Farm"), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())

        addTransaction(scenario, description = "Old transaction", amount = "1000", year = 2024, month = 1, day = 1)
        addTransaction(scenario, description = "New transaction", amount = "2000", year = 2024, month = 1, day = 2)

        scenario.recreate()

        scenario.onActivity { activity ->
            val container = activity.findViewById<LinearLayout>(R.id.recentTransactionsContainer)
            val first = container.getChildAt(0) as TextView
            val second = container.getChildAt(1) as TextView
            assertTrue(
                "Expected newest transaction first, but was:\n${first.text}\n---\n${second.text}",
                first.text.contains("New transaction") && second.text.contains("Old transaction")
            )
        }
    }

    private fun addTransaction(
        scenario: ActivityScenario<FarmActivity>,
        description: String,
        amount: String,
        year: Int,
        month: Int,
        day: Int
    ) {
        onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
        onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
        onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText(description), closeSoftKeyboard())
        PickerTestHelpers.pickDateTime(year, month - 1, day, 12, 0)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
