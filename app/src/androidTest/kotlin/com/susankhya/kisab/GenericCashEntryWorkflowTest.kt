package com.susankhya.kisab

import android.content.Context
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M15 device workflow: generic cash income/expense must be reachable from
 * the Record sheet without any Party/Product/Supply context, must not create
 * trades or settlements, and must land in the ordinary cash ledger.
 */
@RunWith(AndroidJUnit4::class)
class GenericCashEntryWorkflowTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun otherExpenseRecordsWithoutSupplierAndNeverCreatesTrade() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(click(), replaceText("CashFarm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetOtherExpenseButton)).perform(click())

            onView(withId(R.id.transactionEditorTitle))
                .check(matches(withText(R.string.transaction_editor_new_other_expense)))
            onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText("150"), closeSoftKeyboard())
            onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText("Transport"), closeSoftKeyboard())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.saveTransactionButton)).perform(scrollTo(), click())

            onView(withId(R.id.recentTransactionsTitle)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val service = FarmSliceService(SharedPreferencesFarmStore(activity.applicationContext))
                val loaded = service.loadFarm(service.currentFarmId()!!)!!
                assertEquals(1, loaded.transactions.size)
                val tx = loaded.transactions.single()
                assertEquals(TransactionType.EXPENSE, tx.type)
                assertEquals(TransactionCategory.OTHER_EXPENSE, tx.category)
                assertEquals(15000L, tx.amountMinor)
                assertTrue(loaded.trades.isEmpty())
                assertTrue(loaded.settlements.isEmpty())
                assertTrue(loaded.parties.isEmpty())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun otherIncomeRecordsWithoutCustomerAndLandsInCashLedger() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(click(), replaceText("IncomeFarm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetOtherIncomeButton)).perform(click())

            onView(withId(R.id.transactionEditorTitle))
                .check(matches(withText(R.string.transaction_editor_new_other_income)))
            onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText("75.5"), closeSoftKeyboard())
            onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText("Manure sale"), closeSoftKeyboard())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.saveTransactionButton)).perform(scrollTo(), click())

            scenario.onActivity { activity ->
                val service = FarmSliceService(SharedPreferencesFarmStore(activity.applicationContext))
                val loaded = service.loadFarm(service.currentFarmId()!!)!!
                assertEquals(1, loaded.transactions.size)
                val tx = loaded.transactions.single()
                assertEquals(TransactionType.INCOME, tx.type)
                assertEquals(TransactionCategory.OTHER_INCOME, tx.category)
                assertEquals(7550L, tx.amountMinor)
                assertTrue(loaded.trades.isEmpty())
                assertTrue(loaded.settlements.isEmpty())
                // No receivable: no party exists to owe anything.
                assertTrue(loaded.parties.isEmpty())
            }
        } finally {
            scenario.close()
        }
    }
}
