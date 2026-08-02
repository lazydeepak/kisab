package com.susankhya.kisab

import android.content.Context
import android.widget.Button
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
                currency = "USD",
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
                currency = "USD",
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
        assertEquals("USD", summary.currencyCode)
    }

    @Test
    fun createsFarmAndRecreatesActivityWithoutLosingState() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)

        onView(withId(R.id.farmNameInput)).perform(typeText("Demo Farm"), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Demo Farm"))))

        onView(withId(R.id.entryLabelInput)).perform(typeText("Goat"), closeSoftKeyboard())
        onView(withId(R.id.entryQuantityInput)).perform(typeText("3"), closeSoftKeyboard())
        onView(withId(R.id.addEntryButton)).perform(click())

        onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText("Egg sale"), closeSoftKeyboard())
        onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText("8000"), closeSoftKeyboard())
        onView(withId(R.id.transactionCurrencyInput)).perform(scrollTo(), replaceText("USD"), closeSoftKeyboard())
        onView(withId(R.id.transactionOccurredAtInput)).perform(scrollTo(), replaceText("2024-01-02T12:00:00Z"), closeSoftKeyboard())
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.recreate()

        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Entry count: 1"))))
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Transaction count: 1"))))
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Balance: 8000 USD"))))
    }
}
