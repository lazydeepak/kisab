package com.susankhya.kisab

import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import java.time.OffsetDateTime
import java.util.Locale
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device verification of M4-02 presentation rules: major-unit money entry
 * and prefill, NPR defaults, currency preservation with mixed-currency
 * rejection, localized amount validation, device-local time rendering, and
 * backup round trips under the new presentation.
 */
@RunWith(AndroidJUnit4::class)
class FarmActivityPresentationTest {

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
    fun majorUnitAmountEntryAndPrefillRoundTrip() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("NPR Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            fillForm(description = "Feed", amount = "123.45", currency = "NPR")
            clickSave(scenario)

            var balance: String? = null
            scenario.onActivity { activity -> balance = activity.formatMoney("NPR", 12345) }
            onView(withId(R.id.summaryText)).check(matches(withText(containsString("Balance: $balance"))))

            selectTransaction("Feed")
            var expectedEdit: String? = null
            scenario.onActivity { activity -> expectedEdit = activity.editFieldAmount("NPR", 12345) }
            onView(withId(R.id.transactionAmountInput)).check(matches(withText(expectedEdit!!)))

            clickSave(scenario)
            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                val farm = service.loadFarm(service.currentFarmId()!!)!!
                assertEquals(1, farm.transactions.size)
                assertEquals(12345, farm.transactions.single().amountMinor)
                assertEquals("NPR", farm.transactions.single().currency)
                assertEquals("2024-01-01T12:00:00Z", farm.transactions.single().occurredAt.toInstant().toString())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nepaliLocaleDefaultsToNprAndFormatsWithLocalizedDigits() {
        setAppLocale(Locale("ne"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("NPR Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            onView(withId(R.id.transactionCurrencyInput)).check(matches(withText("NPR")))

            fillForm(description = "Feed", amount = "१२३.४५", currency = "NPR")
            clickSave(scenario)

            var balance: String? = null
            scenario.onActivity { activity -> balance = activity.formatMoney("NPR", 12345) }
            assertTrue("Expected Nepali digits in balance, was: $balance", balance!!.contains("१२३.४५"))
            onView(withId(R.id.summaryText)).check(matches(withText(containsString(balance!!))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun usdFarmPrefillsUsdAndRejectsMixedCurrency() {
        seedTransaction(amountMinor = 1500, currency = "USD", description = "Feed")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.transactionCurrencyInput)).check(matches(withText("USD")))

            var money: String? = null
            scenario.onActivity { activity -> money = activity.formatMoney("USD", 1500) }
            onView(withId(R.id.transactionsText)).check(matches(withText(containsString(money!!))))

            fillForm(description = "Feed", amount = "10.00", currency = "EUR")
            clickSave(scenario)

            var expectedError: String? = null
            scenario.onActivity { activity -> expectedError = activity.getString(R.string.error_transaction_currency_mismatch, "USD") }
            onView(withId(R.id.validationMessageText)).check(matches(withText(expectedError!!)))

            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                val farm = service.loadFarm(service.currentFarmId()!!)!!
                assertEquals(1, farm.transactions.size)
                assertEquals("USD", farm.transactions.single().currency)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun amountValidationShowsLocalizedErrors() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("Demo Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            fun fillAmountAndSave(amount: String) {
                onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText("Feed"), closeSoftKeyboard())
                onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
                onView(withId(R.id.transactionOccurredAtInput))
                    .perform(scrollTo(), replaceText("2024-01-01T17:45:00+05:45"), closeSoftKeyboard())
                clickSave(scenario)
            }

            fun expectError(errorRes: Int) {
                var expected: String? = null
                scenario.onActivity { activity -> expected = activity.getString(errorRes) }
                onView(withId(R.id.validationMessageText)).check(matches(withText(expected!!)))
            }

            fillAmountAndSave("")
            expectError(R.string.error_transaction_amount_required)

            fillAmountAndSave("0")
            expectError(R.string.error_transaction_amount_positive)

            fillAmountAndSave("-5")
            expectError(R.string.error_transaction_amount_positive)

            fillAmountAndSave("12.345")
            expectError(R.string.error_transaction_amount_too_precise)

            fillAmountAndSave("abc")
            expectError(R.string.error_transaction_amount_invalid)

            fillAmountAndSave("99999999999999999999.00")
            expectError(R.string.error_transaction_amount_too_large)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun deviceLocalTimeRoundsTripsAndRendersWithoutUtcLiteral() {
        seedTransaction(amountMinor = 1500, currency = "USD", description = "Feed", occurredAt = "2024-01-01T12:00:00Z")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            selectTransaction("Feed")
            var editTime: String? = null
            scenario.onActivity { activity ->
                editTime = activity.findViewById<EditText>(R.id.transactionOccurredAtInput).text.toString()
            }
            assertEquals("2024-01-01T12:00:00Z", OffsetDateTime.parse(editTime!!).toInstant().toString())
            onView(withId(R.id.transactionsText)).check(matches(withText(not(containsString("UTC")))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backupExportImportPreservesNprPresentation() {
        seedTransaction(amountMinor = 12345, currency = "NPR", description = "Feed", occurredAt = "2024-01-01T12:00:00Z", income = true)
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            var backup: String? = null
            scenario.onActivity { activity -> backup = activity.createBackupContentForCurrentFarm() }
            assertNotNull(backup)

            val envelope = FarmBackupCodec.decode(backup!!)
            assertEquals("NPR", envelope.farm.transactions.single().currency)
            assertEquals(12345, envelope.farm.transactions.single().amountMinor)

            SharedPreferencesFarmStore(context).clear()
            scenario.onActivity { activity -> activity.handleImportedBackupContent(backup!!) }
            clickDialogAction(R.string.action_replace_farm)

            var balance: String? = null
            scenario.onActivity { activity -> balance = activity.formatMoney("NPR", 12345) }
            onView(withId(R.id.summaryText)).check(matches(withText(containsString("Balance: $balance"))))
        } finally {
            scenario.close()
        }
    }

    private fun fillForm(description: String, amount: String, currency: String) {
        onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText(description), closeSoftKeyboard())
        onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
        onView(withId(R.id.transactionCurrencyInput)).perform(scrollTo(), replaceText(currency), closeSoftKeyboard())
        onView(withId(R.id.transactionOccurredAtInput))
            .perform(scrollTo(), replaceText("2024-01-01T17:45:00+05:45"), closeSoftKeyboard())
    }

    private fun clickSave(scenario: ActivityScenario<FarmActivity>) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun selectTransaction(description: String) {
        onView(withId(R.id.transactionSelectionSpinner)).perform(click())
        onView(withText(containsString(description)))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
            .perform(click())
    }

    private fun clickDialogAction(@StringRes labelRes: Int) {
        onView(withText(labelRes))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click())
    }

    private fun seedTransaction(
        amountMinor: Long,
        currency: String,
        description: String,
        occurredAt: String = "2024-01-01T12:00:00Z",
        income: Boolean = false
    ) {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Demo Farm")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = if (income) TransactionType.INCOME else TransactionType.EXPENSE,
                category = if (income) TransactionCategory.SALES else TransactionCategory.FEED,
                amountMinor = amountMinor,
                currency = currency,
                description = description,
                occurredAt = occurredAt
            )
        )
    }

    private fun setAppLocale(locale: Locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.forLanguageTags(locale.toLanguageTag())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale.toLanguageTag()))
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun resetAppLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.getEmptyLocaleList()
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
