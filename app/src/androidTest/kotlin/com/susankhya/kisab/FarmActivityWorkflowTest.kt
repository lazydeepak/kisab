package com.susankhya.kisab

import android.content.Context
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
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
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import com.susankhya.kisab.ui.FarmOrdering
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device verification of the M4-03 daily-entry workflow: quick-action
 * income/expense entry, derived currency and default-now time, edit with
 * stable identity, recreation and discard protection, and farm-tools
 * availability.
 */
@RunWith(AndroidJUnit4::class)
class FarmActivityWorkflowTest {

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

    private data class SeedTransaction(
        val description: String,
        val amountMinor: Long,
        val currency: String,
        val occurredAt: String = "2024-01-01T12:00:00Z",
        val income: Boolean = false
    )

    @Test
    fun transportExpenseCategorySelectableAndPersisted() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Transport Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            onView(withId(R.id.transactionTypeExpenseRadio)).check(matches(isChecked()))

            scenario.onActivity { activity ->
                val spinner = activity.findViewById<android.widget.Spinner>(R.id.transactionCategorySpinner)
                val labels: List<String> = (0 until spinner.count).map { spinner.getItemAtPosition(it) as String }
                assertTrue("Transport must be offered for an expense", labels.contains(activity.getString(R.string.transaction_category_transport)))
                spinner.setSelection(labels.indexOf(activity.getString(R.string.transaction_category_transport)))
                assertEquals(TransactionCategory.TRANSPORT, FarmOrdering.categoriesFor(TransactionType.EXPENSE)[spinner.selectedItemPosition])
            }

            fillEditor(description = "Van hire", amount = "1500.00")
            onView(withId(R.id.saveTransactionButton)).perform(scrollTo(), click())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val farm = farmFor(activity)
                assertEquals(1, farm.transactions.size)
                val transaction = farm.transactions.single()
                assertEquals(TransactionType.EXPENSE, transaction.type)
                assertEquals(TransactionCategory.TRANSPORT, transaction.category)
                assertEquals(150000L, transaction.amountMinor)
                assertEquals("Van hire", transaction.description)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun firstIncomeQuickActionRecordsWithoutCurrencyOrIsoInput() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Workflow Farm")
            onView(withId(R.id.firstActionPrompt)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
            scenario.onActivity { activity ->
                assertTrue("Amount field must receive initial focus", activity.findViewById<EditText>(R.id.transactionAmountInput).hasFocus())
            }
            onView(withId(R.id.saveTransactionButton)).check(matches(withText(R.string.save_income_action)))

            PickerTestHelpers.pickDateTime(2024, 0, 1, 17, 45)
            fillEditor(description = "Milk sale", amount = "1500")
            clickSave(scenario)

            scenario.onActivity { activity ->
                val farm = farmFor(activity)
                assertEquals(1, farm.transactions.size)
                val transaction = farm.transactions.single()
                assertEquals(TransactionType.INCOME, transaction.type)
                assertEquals(150000, transaction.amountMinor)
                assertEquals("NPR", farm.currencyCode)
                assertEquals(expectedInstant(2024, 1, 1, 17, 45), transaction.occurredAt.toInstant().toString())
            }

            var income: String? = null
            scenario.onActivity { activity -> income = activity.formatMoney("NPR", 150000) }
            onView(withId(R.id.balanceText)).check(matches(withText(containsString(income))))
            onView(withId(R.id.incomeText)).check(matches(withText(containsString(income))))

            assertTrue("Expected saved transaction in recent rows", recentRowText(scenario).contains("Milk sale"))
            onView(withId(R.id.firstActionPrompt)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun repeatedExpenseDerivesCurrencyAndSuppliesCurrentTime() {
        seedFarm("Currency Farm", SeedTransaction("Feed", 1000, "USD"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            onView(withId(R.id.saveTransactionButton)).check(matches(withText(R.string.save_expense_action)))

            scenario.onActivity { activity ->
                val iso = activity.editorOccurredAtIsoForTest()
                val elapsed = Duration.between(OffsetDateTime.parse(iso).toInstant(), Instant.now()).toMillis()
                assertTrue("Expected default-now timestamp, was $iso", elapsed in 0..120_000)
            }

            fillEditor(description = "More feed", amount = "2500")
            clickSave(scenario)

            var expenses: String? = null
            scenario.onActivity { activity -> expenses = activity.formatMoney("USD", 251000) }
            onView(withId(R.id.expensesText)).check(matches(withText(containsString(expenses))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun editPreservesIdAndRecalculatesTotals() {
        seedFarm("Edit Farm", SeedTransaction("Feed", 1000, "NPR"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            var originalId: String? = null
            scenario.onActivity { activity -> originalId = farmFor(activity).transactions.single().id }

            openEditorForTransaction("Feed")
            onView(withId(R.id.transactionEditorTitle)).check(matches(withText(R.string.transaction_editor_edit_section)))
            onView(withId(R.id.deleteTransactionButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.saveTransactionButton)).check(matches(withText(R.string.update_transaction_action)))
            scenario.onActivity { activity ->
                val amountField = activity.findViewById<EditText>(R.id.transactionAmountInput)
                assertEquals(activity.editFieldAmount("NPR", 1000), amountField.text.toString())
                assertEquals("Feed", activity.findViewById<EditText>(R.id.transactionDescriptionInput).text.toString())
            }

            fillEditor(description = "Feed updated", amount = "2000")
            PickerTestHelpers.pickDateTime(2024, 0, 5, 17, 45)
            clickSave(scenario)

            scenario.onActivity { activity ->
                val farm = farmFor(activity)
                assertEquals(1, farm.transactions.size)
                val transaction = farm.transactions.single()
                assertEquals("Transaction identity must be stable", originalId, transaction.id)
                assertEquals(200000, transaction.amountMinor)
                assertEquals("Feed updated", transaction.description)
                assertEquals(expectedInstant(2024, 1, 5, 17, 45), transaction.occurredAt.toInstant().toString())
            }

            var expenses: String? = null
            scenario.onActivity { activity -> expenses = activity.formatMoney("NPR", 200000) }
            onView(withId(R.id.expensesText)).check(matches(withText(containsString(expenses))))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun editWithoutChangingDateTimePreservesExactInstant() {
        seedFarm("Instant Farm", SeedTransaction("Feed", 1000, "NPR"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("Feed")
            fillEditor(description = "Feed", amount = "12.00")
            clickSave(scenario)

            scenario.onActivity { activity ->
                val transaction = farmFor(activity).transactions.single()
                assertEquals(1200, transaction.amountMinor)
                assertEquals("2024-01-01T12:00:00Z", transaction.occurredAt.toInstant().toString())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreationPreservesEditorDraft() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Recreate Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Partial", amount = "50")

            scenario.recreate()

            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("50")))
            onView(withId(R.id.transactionDescriptionInput)).check(matches(withText("Partial")))
            scenario.onActivity { activity ->
                assertEquals(0, farmFor(activity).transactions.size)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun dirtyCancelAndBackRequireDiscardConfirmation() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Discard Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Change me", amount = "10")

            onView(withId(R.id.cancelTransactionButton)).perform(scrollTo(), click())
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_discard)
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            scenario.onActivity { activity -> assertEquals(0, farmFor(activity).transactions.size) }

            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Change me", amount = "20")
            Espresso.pressBack()
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_keep_editing)
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("20")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun switchingWhileDirtyConfirmsBeforeOpeningAnotherTransaction() {
        seedFarm("Switch Farm", SeedTransaction("Feed", 1000, "NPR"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Unsaved", amount = "10")

            openEditorForTransaction("Feed")
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_discard)

            onView(withId(R.id.transactionEditorTitle)).check(matches(withText(R.string.transaction_editor_edit_section)))
            scenario.onActivity { activity ->
                val amountField = activity.findViewById<EditText>(R.id.transactionAmountInput)
                assertEquals(activity.editFieldAmount("NPR", 1000), amountField.text.toString())
                assertEquals("Feed", activity.findViewById<EditText>(R.id.transactionDescriptionInput).text.toString())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recordButtonWhileDirtyConfirmsDiscard() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Switch Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Unsaved", amount = "10")

            onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_keep_editing)
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("10")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun emptyFarmDefaultsToNprAndAllowsCurrencyChoice() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Currency Farm")
            openSettings()
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText("NPR")))
            onView(withId(R.id.changeSettingsCurrencyButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            changeSettingCurrency("USD")
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText("USD")))

            onView(withId(R.id.navHomeItem)).perform(click())
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            PickerTestHelpers.pickDateTime(2024, 0, 1, 17, 45)
            fillEditor(description = "Sale", amount = "100")
            clickSave(scenario)
            scenario.onActivity { activity ->
                val farm = farmFor(activity)
                assertEquals("USD", farm.currencyCode)
                assertEquals(1, farm.transactions.size)
                assertEquals(TransactionCategory.FEED, farm.transactions.single().category)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun establishedFarmLocksCurrencyInSettings() {
        seedFarm(
            "USD Farm",
            SeedTransaction("Feed", 1000, "USD"),
            SeedTransaction("Supplies", 2000, "USD", occurredAt = "2024-01-02T12:00:00Z")
        )
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openSettings()
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText("USD")))
            onView(withId(R.id.changeSettingsCurrencyButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.settingsCurrencyLockedText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun transactionEditorExposesNoCurrencyControl() {
        seedFarm("Sole Farm", SeedTransaction("Feed", 1000, "USD"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("Feed")
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            clickSave(scenario)
            scenario.onActivity { activity ->
                assertEquals(1, farmFor(activity).transactions.size)
                assertEquals("USD", farmFor(activity).currencyCode)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun dateTimeIsHumanReadableAndPickerOpensWithoutIsoTyping() {
        seedFarm("Time Farm", SeedTransaction("Feed", 1000, "NPR"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openEditorForTransaction("Feed")
            scenario.onActivity { activity ->
                assertFalse("Date/time must not be an editable field", activity.findViewById<View>(R.id.transactionDateTimeText) is EditText)
            }

            onView(withId(R.id.changeDateTimeButton)).perform(scrollTo(), click())
            onView(withClassName(equalTo(DatePicker::class.java.name))).check(matches(isDisplayed()))
            Espresso.pressBack()

            scenario.onActivity { activity ->
                val iso = activity.editorOccurredAtIsoForTest()
                assertEquals("2024-01-01T12:00:00Z", OffsetDateTime.parse(iso).toInstant().toString())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun farmToolsRemainAvailableAfterExpansion() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Tools Farm")
            onView(withId(R.id.farmToolsContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            onView(withId(R.id.farmToolsToggleButton)).perform(scrollTo(), click())
            onView(withId(R.id.farmToolsContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.farmToolsToggleButton)).check(matches(withText(R.string.hide_farm_tools_action)))

            onView(withId(R.id.entryLabelInput)).perform(scrollTo(), typeText("Goat"), closeSoftKeyboard())
            onView(withId(R.id.entryQuantityInput)).perform(scrollTo(), typeText("2"), closeSoftKeyboard())
            onView(withId(R.id.addEntryButton)).perform(scrollTo(), click())
            onView(withId(R.id.exportBackupButton)).check(matches(withText(R.string.export_backup_action)))
            onView(withId(R.id.importBackupButton)).check(matches(withText(R.string.import_backup_action)))

            onView(withId(R.id.farmToolsToggleButton)).perform(scrollTo(), click())
            onView(withId(R.id.farmToolsContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.farmToolsToggleButton)).check(matches(withText(R.string.show_farm_tools_action)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun saveLabelFollowsSelectedTransactionType() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Label Farm")
            onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
            onView(withId(R.id.saveTransactionButton)).check(matches(withText(R.string.save_income_action)))

            onView(withId(R.id.transactionTypeExpenseRadio)).perform(scrollTo(), click())
            onView(withId(R.id.saveTransactionButton)).check(matches(withText(R.string.save_expense_action)))

            onView(withId(R.id.transactionTypeIncomeRadio)).perform(scrollTo(), click())
            onView(withId(R.id.saveTransactionButton)).check(matches(withText(R.string.save_income_action)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun invalidCurrencyIsoKeepsDialogOpenWithError() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Currency Farm")
            openSettings()

            onView(withId(R.id.changeSettingsCurrencyButton)).perform(scrollTo(), click())
            onView(withId(R.id.currencyInput)).inRoot(isDialog()).perform(replaceText("US"), closeSoftKeyboard())
            clickDialogAction(R.string.action_ok)
            onView(withId(R.id.currencyErrorText)).inRoot(isDialog())
                .check(matches(withText(R.string.error_currency_iso_three_letters)))
            onView(withId(R.id.currencyInput)).inRoot(isDialog()).check(matches(isDisplayed()))

            onView(withId(R.id.currencyInput)).inRoot(isDialog()).perform(replaceText("USD"), closeSoftKeyboard())
            clickDialogAction(R.string.action_ok)
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText("USD")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreationPreservesDirtyBaselineForDiscardProtection() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Recreate Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Partial", amount = "50")

            scenario.recreate()

            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("50")))

            Espresso.pressBack()
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_keep_editing)

            onView(withId(R.id.cancelTransactionButton)).perform(scrollTo(), click())
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_keep_editing)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreationPreservesCleanBaselineSoUnchangedEditorClosesSilently() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Recreate Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())

            scenario.recreate()

            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            scenario.onActivity { activity -> assertEquals(0, farmFor(activity).transactions.size) }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun farmToolsExpansionSurvivesRecreation() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Tools Farm")
            onView(withId(R.id.farmToolsToggleButton)).perform(scrollTo(), click())
            onView(withId(R.id.farmToolsContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            scenario.recreate()

            onView(withId(R.id.farmToolsContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.farmToolsToggleButton)).check(matches(withText(R.string.hide_farm_tools_action)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun firstTransactionCurrencyChoiceAppliesIsoFractionDigits() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("JPY Farm")
            openSettings()
            changeSettingCurrency("JPY")
            onView(withId(R.id.settingsCurrencyText)).check(matches(withText("JPY")))
            onView(withId(R.id.navHomeItem)).perform(click())
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Rice", amount = "1500")
            clickSave(scenario)
            scenario.onActivity { activity ->
                assertEquals("JPY", farmFor(activity).currencyCode)
                assertEquals(1500, farmFor(activity).transactions.single().amountMinor)
            }

            openEditorForTransaction("Rice")
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("1500")))
            clickSave(scenario)
            scenario.onActivity { activity ->
                assertEquals("JPY", farmFor(activity).currencyCode)
                assertEquals(1500, farmFor(activity).transactions.single().amountMinor)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun firstTransactionInKwdUsesThreeFractionDigits() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("KWD Farm")
            openSettings()
            changeSettingCurrency("KWD")
            onView(withId(R.id.navHomeItem)).perform(click())
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Feed", amount = "1.500")
            clickSave(scenario)
            scenario.onActivity { activity ->
                assertEquals("KWD", farmFor(activity).currencyCode)
                assertEquals(1500, farmFor(activity).transactions.single().amountMinor)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun bottomNavigationSwitchesBetweenDestinations() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            onView(withId(R.id.navHisabKitabItem)).perform(click())
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.shellTitle)).check(matches(withText(R.string.nav_hisab_kitab)))

            onView(withId(R.id.navHisabItem)).perform(click())
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.shellTitle)).check(matches(withText(R.string.nav_hisab)))

            onView(withId(R.id.shellSettingsButton)).perform(click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.shellTitle)).check(matches(withText(R.string.nav_settings)))

            onView(withId(R.id.navHomeItem)).perform(click())
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backFromSettingsRestoresPriorPrimaryDestination() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabKitabItem)).perform(click())
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.shellSettingsButton)).perform(click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.hisabKitabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            Espresso.pressBack()
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backFromPrimaryDestinationReturnsToHome() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            Espresso.pressBack()
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.navHisabKitabItem)).perform(click())
            Espresso.pressBack()
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun partyListShowsEmptyStateThenAddsAndEditsParty() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Party Farm")
            onView(withId(R.id.navHisabKitabItem)).perform(click())
            onView(withId(R.id.partiesEmptyText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.addPartyButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.addPartyButton)).perform(scrollTo(), click())
            onView(withId(R.id.partyNameInput)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.partyNameInput)).perform(replaceText("Feed Store"), closeSoftKeyboard())
            onView(withId(R.id.partyContactInput)).perform(replaceText("9800000001"), closeSoftKeyboard())
            onView(withId(R.id.savePartyButton)).perform(scrollTo(), click())

            onView(withId(R.id.partiesEmptyText)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.partyRow)).check(matches(withText(containsString("Feed Store"))))

            onView(withId(R.id.partyRow)).perform(click())
            onView(withId(R.id.partyNameInput)).check(matches(withText("Feed Store")))
            onView(withId(R.id.partyContactInput)).check(matches(withText("9800000001")))
            onView(withId(R.id.deletePartyButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.partyNameInput)).perform(replaceText("Corner Feed"), closeSoftKeyboard())
            onView(withId(R.id.savePartyButton)).perform(scrollTo(), click())
            onView(withId(R.id.partyRow)).check(matches(withText(containsString("Corner Feed"))))

            scenario.onActivity { activity ->
                val parties = farmFor(activity).parties
                assertEquals(1, parties.size)
                assertEquals("Corner Feed", parties.single().name)
                assertEquals(com.susankhya.kisab.domain.PartyRole.SUPPLIER, parties.single().role)
                assertEquals("9800000001", parties.single().contact)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun partyEditorRejectsBlankName() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Party Farm")
            onView(withId(R.id.navHisabKitabItem)).perform(click())
            onView(withId(R.id.addPartyButton)).perform(scrollTo(), click())
            onView(withId(R.id.savePartyButton)).perform(scrollTo(), click())
            onView(withText(R.string.error_party_name_required)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                assertEquals(0, farmFor(activity).parties.size)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun partyEditorDeleteConfirmsAndRemovesParty() {
        seedParty("Delete Me", "9800000001")
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabKitabItem)).perform(click())
            onView(withId(R.id.partyRow)).perform(click())
            onView(withId(R.id.deletePartyButton)).perform(scrollTo(), click())
            onView(withText(R.string.dialog_delete_party_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.delete_party_action)
            onView(withId(R.id.partiesEmptyText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            scenario.onActivity { activity ->
                assertEquals(0, farmFor(activity).parties.size)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun destinationSurvivesRecreation() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.shellSettingsButton)).perform(click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            scenario.recreate()

            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun switchingAwayWhileDirtyPromptsDiscardConfirmation() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Switch Shell Farm")
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Unsaved", amount = "10")

            onView(withId(R.id.shellSettingsButton)).perform(click())
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_keep_editing)
            onView(withId(R.id.scrollView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("10")))

            onView(withId(R.id.shellSettingsButton)).perform(click())
            onView(withText(R.string.discard_changes_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            clickDialogAction(R.string.action_discard)
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun settingsShowsNeutralStateBeforeFarmExists() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            openSettings()
            onView(withId(R.id.settingsNoFarmText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.settingsCurrencyText)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.changeSettingsCurrencyButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun appBarTitleReflectsFarmNameOnHome() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Titled Farm")
            onView(withId(R.id.shellTitle)).check(matches(withText("Titled Farm")))
        } finally {
            scenario.close()
        }
    }

    private fun createFarm(name: String) {
        onView(withId(R.id.farmNameInput)).perform(typeText(name), closeSoftKeyboard())
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

    private fun openSettings() {
        onView(withId(R.id.shellSettingsButton)).perform(click())
    }

    private fun changeSettingCurrency(code: String) {
        onView(withId(R.id.changeSettingsCurrencyButton)).perform(scrollTo(), click())
        onView(withId(R.id.currencyInput)).inRoot(isDialog()).perform(replaceText(code), closeSoftKeyboard())
        onView(withId(R.id.currencyInput)).inRoot(isDialog()).check(matches(withText(code)))
        clickDialogAction(R.string.action_ok)
    }

    private fun expectedInstant(year: Int, month: Int, day: Int, hour: Int, minute: Int): String =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault())
            .toInstant()
            .toString()

    private fun clickSave(scenario: ActivityScenario<FarmActivity>) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<android.widget.Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun clickDialogAction(@StringRes labelRes: Int) {
        onView(withText(labelRes))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click())
    }

    private fun recentRowText(scenario: ActivityScenario<FarmActivity>): String {
        var text = ""
        scenario.onActivity { activity ->
            val container = activity.findViewById<LinearLayout>(R.id.recentTransactionsContainer)
            text = (container.getChildAt(0) as TextView).text.toString()
        }
        return text
    }

    private fun farmFor(activity: android.app.Activity): com.susankhya.kisab.domain.FarmState {
        val store = SharedPreferencesFarmStore(activity.applicationContext)
        val service = FarmSliceService(store)
        return service.loadFarm(service.currentFarmId()!!)!!
    }

    private fun seedFarm(name: String, vararg transactions: SeedTransaction) {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm(name, currencyCode = transactions.firstOrNull()?.currency ?: "NPR")
        transactions.forEach { transaction ->
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = if (transaction.income) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = if (transaction.income) TransactionCategory.SALES else TransactionCategory.FEED,
                    amountMinor = transaction.amountMinor,
                    description = transaction.description,
                    occurredAt = transaction.occurredAt
                )
            )
        }
    }

    private fun seedParty(name: String, contact: String, role: com.susankhya.kisab.domain.PartyRole = com.susankhya.kisab.domain.PartyRole.SUPPLIER) {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Party Farm", currencyCode = "NPR")
        service.addParty(
            farm.id,
            com.susankhya.kisab.domain.PartyDraft(name = name, role = role, contact = contact)
        )
    }
}
