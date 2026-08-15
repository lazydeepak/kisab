package com.susankhya.kisab

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.SharedPreferencesAppLanguagePreferences
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.AppLanguage
import com.susankhya.kisab.ui.FarmActivity
import com.susankhya.kisab.ui.FarmCurrencies
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device verification of the M4-02 presentation rules under the M4-03
 * daily-entry flow: major-unit money entry and prefill, NPR defaults, currency
 * derivation and locking, localized amount validation, device-local time
 * rendering, and backup round trips.
 */
@RunWith(AndroidJUnit4::class)
class FarmActivityPresentationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        SharedPreferencesAppLanguagePreferences(context).save(AppLanguage.FOLLOW_DEVICE)
        resetAppLocale()
    }

    @After
    fun tearDown() {
        resetAppLocale()
        SharedPreferencesAppLanguagePreferences(context).save(AppLanguage.FOLLOW_DEVICE)
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun majorUnitAmountEntryAndPrefillRoundTrip() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("NPR Farm")

            openExpenseEditor()
            setOccurredAt(2024, 1, 1, 17, 45)
            fillEditor(description = "Feed", amount = "123.45")
            clickSave(scenario)

            var balance: String? = null
            scenario.onActivity { activity ->
                balance = activity.formatMoney(FarmCurrencies.defaultFor(Locale.getDefault()), 12345)
            }
            onView(withId(R.id.balanceText)).check(matches(withText(containsString(balance))))

            openEditorForTransaction("Feed")
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
                assertEquals(FarmCurrencies.defaultFor(Locale.getDefault()), farm.currencyCode)
                assertEquals(expectedInstant(2024, 1, 1, 17, 45), farm.transactions.single().occurredAt.toInstant().toString())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nepaliLocaleFormatsWithLocalizedDigits() {
        setAppLocale(Locale.forLanguageTag("ne"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("NPR Farm")

            openSettings()
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText(FarmCurrencies.defaultFor(Locale.getDefault()))))
            onView(withId(R.id.navHomeItem)).perform(click())

            openExpenseEditor()
            setOccurredAt(2024, 1, 1, 17, 45)
            fillEditor(description = "Feed", amount = "१२३.४५")
            clickSave(scenario)

            var balance: String? = null
            scenario.onActivity { activity ->
                balance = activity.formatMoney(FarmCurrencies.defaultFor(Locale.getDefault()), 12345)
            }
            assertTrue("Expected Nepali digits in balance, was: $balance", balance!!.contains("१२३.४५"))
            onView(withId(R.id.balanceText)).check(matches(withText(containsString(balance!!))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun usdFarmDerivesCurrencyAndAllowsChange() {
        seedTransaction(amountMinor = 1500, currency = "USD", description = "Feed")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openSettings()
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText("USD")))
            onView(withId(R.id.changeSettingsCurrencyButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            var money: String? = null
            scenario.onActivity { activity -> money = activity.formatMoney("USD", 1500) }
            assertTrue("Expected USD amount in recent row", recentRowText(scenario).contains(money!!))

            onView(withId(R.id.navHomeItem)).perform(click())
            openExpenseEditor()
            setOccurredAt(2024, 1, 1, 17, 45)
            fillEditor(description = "More feed", amount = "10.00")
            clickSave(scenario)

            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                val farm = service.loadFarm(service.currentFarmId()!!)!!
                assertEquals(2, farm.transactions.size)
                assertEquals("USD", farm.currencyCode)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun amountValidationShowsLocalizedErrors() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Demo Farm")
            openExpenseEditor()
            setOccurredAt(2024, 1, 1, 17, 45)

            fun fillAmountAndSave(amount: String) {
                onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText("Feed"), closeSoftKeyboard())
                onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
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
            openEditorForTransaction("Feed")
            var editTime: String? = null
            scenario.onActivity { activity -> editTime = activity.editorOccurredAtIsoForTest() }
            assertEquals("2024-01-01T12:00:00Z", OffsetDateTime.parse(editTime!!).toInstant().toString())
            assertFalse("UTC literal leaked into recent row", recentRowText(scenario).contains("UTC"))
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
            assertEquals("NPR", envelope.farm.currencyCode)
            assertEquals(12345, envelope.farm.transactions.single().amountMinor)

            SharedPreferencesFarmStore(context).clear()
            scenario.onActivity { activity -> activity.handleImportedBackupContent(backup!!) }
            clickDialogAction(R.string.action_replace_farm)

            var balance: String? = null
            scenario.onActivity { activity -> balance = activity.formatMoney("NPR", 12345) }
            onView(withId(R.id.balanceText)).check(matches(withText(containsString(balance))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun languageSelectionAppliesNepaliAndPersists() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openSettings()
            onView(withId(R.id.languageNepaliRadio)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.languageNepaliRadio)).perform(scrollTo(), click())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals(AppLanguage.NEPALI, SharedPreferencesAppLanguagePreferences(context).load())
            scenario.onActivity { activity ->
                assertEquals(
                    "अङ्ग्रेजी",
                    activity.findViewById<android.widget.RadioButton>(R.id.languageEnglishRadio).text.toString()
                )
                assertEquals(
                    activity.getString(R.string.language_nepali),
                    activity.findViewById<android.widget.RadioButton>(R.id.languageNepaliRadio).text.toString()
                )
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun languageFollowDeviceResetsToSystemLocale() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openSettings()
            onView(withId(R.id.languageNepaliRadio)).perform(scrollTo(), click())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.languageFollowDeviceRadio)).perform(scrollTo(), click())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals(AppLanguage.FOLLOW_DEVICE, SharedPreferencesAppLanguagePreferences(context).load())
        } finally {
            scenario.close()
        }
    }

    private fun createFarm(name: String) {
        onView(withId(R.id.farmNameInput)).perform(typeText(name), closeSoftKeyboard())
        onView(withId(R.id.createFarmButton)).perform(click())
    }

    private fun openExpenseEditor() {
        onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
    }

    private fun fillEditor(description: String, amount: String) {
        onView(withId(R.id.transactionAmountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
        onView(withId(R.id.transactionDescriptionInput)).perform(scrollTo(), replaceText(description), closeSoftKeyboard())
    }

    private fun setOccurredAt(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        PickerTestHelpers.pickDateTime(year, month - 1, day, hour, minute)
    }

    private fun expectedInstant(year: Int, month: Int, day: Int, hour: Int, minute: Int): String =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault())
            .toInstant()
            .toString()

    private fun openEditorForTransaction(description: String) {
        onView(allOf(withId(R.id.recentTransactionRow), withText(containsString(description))))
            .perform(scrollTo(), click())
    }

    private fun recentRowText(scenario: ActivityScenario<FarmActivity>): String {
        var text = ""
        scenario.onActivity { activity ->
            val container = activity.findViewById<LinearLayout>(R.id.recentTransactionsContainer)
            text = (container.getChildAt(0) as TextView).text.toString()
        }
        return text
    }

    private fun clickSave(scenario: ActivityScenario<FarmActivity>) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
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
        val farm = service.createFarm("Demo Farm", currencyCode = currency)
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = if (income) TransactionType.INCOME else TransactionType.EXPENSE,
                category = if (income) TransactionCategory.SALES else TransactionCategory.FEED,
                amountMinor = amountMinor,
                description = description,
                occurredAt = occurredAt
            )
        )
    }

    private fun openSettings() {
        onView(withId(R.id.shellMenuButton)).perform(click())
        onView(withText(R.string.nav_settings)).perform(click())
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
