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

        onView(withId(R.id.farmToolsToggleButton)).perform(scrollTo(), click())
        onView(withId(R.id.entryLabelInput)).perform(scrollTo(), typeText("Goat"), closeSoftKeyboard())
        onView(withId(R.id.entryQuantityInput)).perform(scrollTo(), typeText("3"), closeSoftKeyboard())
        onView(withId(R.id.addEntryButton)).perform(scrollTo(), click())

        // Seed the income record through the domain layer: the create-mode
        // cash editor retired in M7+ no longer exists in the shell.
        scenario.onActivity { activity ->
            val store = SharedPreferencesFarmStore(activity.applicationContext)
            val service = FarmSliceService(store)
            service.createTransaction(
                service.currentFarmId()!!,
                FarmTransactionDraft(
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 800000L,
                    description = "Egg sale",
                    occurredAt = "2024-01-02T12:00:00Z"
                )
            )
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.recreate()

        var entryCount: String? = null
        var balance: String? = null
        scenario.onActivity { activity ->
            val store = SharedPreferencesFarmStore(activity.applicationContext)
            val service = FarmSliceService(store)
            val farm = service.loadFarm(service.currentFarmId()!!)!!
            entryCount = activity.formatCount(farm.entries.size)
            balance = activity.formattedBalance(farm.currencyCode, 800000L)
        }
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Entry count: $entryCount"))))
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Balance: $balance"))))
    }

    @Test
    fun transactionsRenderNewestFirstInHistory() {
        run {
            val store = SharedPreferencesFarmStore(context)
            val service = FarmSliceService(store)
            val farm = service.createFarm("Order Farm")
            listOf(
                "Old transaction" to "2024-01-01T12:00:00Z",
                "New transaction" to "2024-01-02T12:00:00Z"
            ).forEach { (description, occurredAt) ->
                service.createTransaction(
                    farm.id,
                    FarmTransactionDraft(
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.FEED,
                        amountMinor = if (description.startsWith("New")) 2000 else 1000,
                        description = description,
                        occurredAt = occurredAt
                    )
                )
            }
        }
        val scenario = ActivityScenario.launch(FarmActivity::class.java)

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
}
