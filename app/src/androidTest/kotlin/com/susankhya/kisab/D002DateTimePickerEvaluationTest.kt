package com.susankhya.kisab

import android.content.Context
import android.os.Build
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M4-05-D002 on-device evaluation of the past-date editing interaction.
 *
 * D002 is an OBSERVATION (not an assumed defect): the change-date-time flow
 * opens a platform DatePicker followed by a TimePicker, defaulting to the
 * editor's current timestamp, and only commits when the time picker confirms.
 * These tests exercise realistic cases on the physical device — change today's
 * transaction to a previous date, edit an older transaction, change both date
 * and time, cancel midway, and verify the displayed local date/time after
 * saving — in English and Nepali, so the disposition reflects observed
 * behavior rather than expectation.
 */
@RunWith(AndroidJUnit4::class)
class D002DateTimePickerEvaluationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        resetAppLocale()
    }

    @After
    fun tearDown() {
        resetAppLocale()
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun changeTodayTransactionToPreviousDatePersistsAndDisplaysLocally() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("D002 Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())

            var createdIso: String? = null
            scenario.onActivity { activity ->
                val iso = activity.editorOccurredAtIsoForTest()
                createdIso = iso
                val elapsed = Duration.between(OffsetDateTime.parse(iso).toInstant(), Instant.now()).toMillis()
                assertTrue("Expected default-now timestamp, was $iso", elapsed in 0..120_000)
            }

            fillEditor(description = "Today feed", amount = "500")
            clickSave(scenario)

            val createdInstant = createdIso!!.let { OffsetDateTime.parse(it).toInstant().toString() }
            scenario.onActivity { activity ->
                assertEquals(createdInstant, farmFor(activity).transactions.single().occurredAt.toInstant().toString())
            }

            openEditorForTransaction("Today feed")
            PickerTestHelpers.pickDateTime(2024, 0, 5, 9, 30)
            clickSave(scenario)

            scenario.onActivity { activity ->
                val farm = farmFor(activity)
                assertEquals(1, farm.transactions.size)
                val tx = farm.transactions.single()
                assertEquals(expectedInstant(2024, 1, 5, 9, 30), tx.occurredAt.toInstant().toString())
                val row = recentRowText(scenario)
                assertTrue("Recent row must render the changed local date/time", row.contains(activity.displayTransactionTime(tx)))
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun editExistingOlderTransactionChangesBothDateAndTime() {
        seedFarm("D002 Edit", "2024-01-10T12:00:00Z")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("old feed")

            PickerTestHelpers.pickDateTime(2024, 1, 3, 8, 45)
            clickSave(scenario)

            scenario.onActivity { activity ->
                val tx = farmFor(activity).transactions.single()
                assertEquals(expectedInstant(2024, 2, 3, 8, 45), tx.occurredAt.toInstant().toString())
                val row = recentRowText(scenario)
                assertTrue("Recent row must render the edited local date/time", row.contains(activity.displayTransactionTime(tx)))
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun cancelAtDatePickerKeepsDraftAndOriginalTimestamp() {
        seedFarm("D002 Draft", "2024-01-10T12:00:00Z")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("old feed")
            fillEditor(description = "old feed", amount = "150")

            onView(withId(R.id.changeDateTimeButton)).perform(scrollTo(), click())
            Espresso.pressBack()

            var iso: String? = null
            scenario.onActivity { activity -> iso = activity.editorOccurredAtIsoForTest() }
            assertEquals("Cancel must not change the timestamp", "2024-01-10T12:00:00Z", OffsetDateTime.parse(iso).toInstant().toString())
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("150")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun cancelAtTimePickerAfterDateSelectionKeepsDraftAndOriginalTimestamp() {
        seedFarm("D002 Draft2", "2024-01-10T12:00:00Z")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("old feed")
            fillEditor(description = "old feed", amount = "200")

            onView(withId(R.id.changeDateTimeButton)).perform(scrollTo(), click())
            PickerTestHelpers.setDateOnly(2024, 0, 5)
            onView(withId(android.R.id.button1)).perform(click())
            onView(withClassName(equalTo(TimePicker::class.java.name))).check(matches(isDisplayed()))
            Espresso.pressBack()

            var iso: String? = null
            scenario.onActivity { activity -> iso = activity.editorOccurredAtIsoForTest() }
            assertEquals("Cancelling the time picker must not commit the date", "2024-01-10T12:00:00Z", OffsetDateTime.parse(iso).toInstant().toString())
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("200")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun pastDateEditCompletesInNepali() {
        setAppLocale("ne-NP")
        seedFarm("D002 Nepali", "2024-01-10T12:00:00Z")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("old feed")
            onView(withId(R.id.changeDateTimeButton)).check(matches(withText(R.string.change_date_time_action)))

            PickerTestHelpers.pickDateTime(2024, 0, 5, 9, 30)
            clickSave(scenario)

            scenario.onActivity { activity ->
                val tx = farmFor(activity).transactions.single()
                assertEquals(expectedInstant(2024, 1, 5, 9, 30), tx.occurredAt.toInstant().toString())
            }
        } finally {
            scenario.close()
        }
    }

    // --- Helpers -------------------------------------------------------------

    private fun recentRowText(scenario: ActivityScenario<FarmActivity>): String {
        var text = ""
        scenario.onActivity { activity ->
            val container = activity.findViewById<LinearLayout>(R.id.recentTransactionsContainer)
            text = (container.getChildAt(0) as TextView).text.toString()
        }
        return text
    }

    private fun createFarm(name: String) {
        onView(withId(R.id.farmNameInput)).perform(replaceText(name), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())
    }

    private fun fillEditor(description: String, amount: String) {
        onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
        onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText(description), closeSoftKeyboard())
    }

    private fun openEditorForTransaction(description: String) {
        onView(allOf(withId(R.id.recentTransactionRow), withText(containsString(description))))
            .perform(scrollTo(), click())
    }

    private fun clickSave(scenario: ActivityScenario<FarmActivity>) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<android.widget.Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun seedFarm(name: String, occurredAt: String) {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm(name)
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 1000,
                currency = "NPR",
                description = "old feed",
                occurredAt = occurredAt
            )
        )
    }

    private fun expectedInstant(year: Int, month: Int, day: Int, hour: Int, minute: Int): String =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault())
            .toInstant()
            .toString()

    private fun farmFor(activity: android.app.Activity): com.susankhya.kisab.domain.FarmState {
        val store = SharedPreferencesFarmStore(activity.applicationContext)
        val service = FarmSliceService(store)
        return service.loadFarm(service.currentFarmId()!!)!!
    }

    private fun setAppLocale(locale: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.forLanguageTags(locale)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        }
        Espresso.onIdle()
    }

    private fun resetAppLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.getEmptyLocaleList()
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
        Espresso.onIdle()
    }
}