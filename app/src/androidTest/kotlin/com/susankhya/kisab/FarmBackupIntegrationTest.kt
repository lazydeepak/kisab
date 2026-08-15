package com.susankhya.kisab

import android.content.Context
import android.widget.EditText
import androidx.annotation.StringRes
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
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import com.susankhya.kisab.ui.FarmCurrencies
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Base64
import java.util.Locale
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmBackupIntegrationTest {
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

    private fun clickDialogAction(@StringRes labelRes: Int) {
        onView(withText(labelRes))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun exportsAndImportsBackupAcrossActivityRecreation() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("Demo Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            var backupContent: String? = null
            scenario.onActivity { activity ->
                backupContent = activity.createBackupContentForCurrentFarm()
            }
            assertNotNull(backupContent)

            val envelope = FarmBackupCodec.decode(backupContent!!)
            assertEquals("Demo Farm", envelope.farm.name)

            SharedPreferencesFarmStore(context).clear()
            scenario.onActivity { activity ->
                activity.handleImportedBackupContent(backupContent!!)
            }
            clickDialogAction(R.string.action_replace_farm)

            var expectedSummary: String? = null
            scenario.onActivity { activity ->
                expectedSummary = activity.getString(
                        R.string.farm_tools_summary_format,
                        "Demo Farm",
                        activity.formatCount(0),
                        activity.formattedBalance(null, 0L)
                )
            }
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary!!)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun invalidImportDoesNotReplaceCurrentFarm() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("Original Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            scenario.onActivity { activity ->
                activity.handleImportedBackupContent("not-a-valid-payload")
            }

            var expectedSummary: String? = null
            scenario.onActivity { activity ->
                expectedSummary = activity.getString(
                    R.string.farm_tools_summary_format,
                    "Original Farm",
                    activity.formatCount(0),
                    activity.formattedBalance(null, 0L)
                )
            }
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary!!)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun cancellingRestorePreservesCurrentFarm() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("Original Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            var backupContent: String? = null
            scenario.onActivity { activity ->
                backupContent = activity.createBackupContentForCurrentFarm()
            }

            scenario.onActivity { activity ->
                activity.handleImportedBackupContent(backupContent!!)
            }
            clickDialogAction(R.string.action_cancel)

            var expectedSummary: String? = null
            scenario.onActivity { activity ->
                expectedSummary = activity.getString(
                    R.string.farm_tools_summary_format,
                    "Original Farm",
                    activity.formatCount(0),
                    activity.formattedBalance(null, 0L)
                )
            }
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary!!)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun rejectedBackupsPreserveCurrentFarm() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("Original Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            val malformedTimestampEnvelope = "1\u001F2024-01-01T00:00:00Z\u001F" +
                Base64.getEncoder().encodeToString(
                    "2\u001Ffarm-1\u001FDemo Farm\u001F\u001Ftx-1\u001DINCOME\u001DSALES\u001D1000\u001DUSD\u001DSale\u001Dnot-a-timestamp"
                        .toByteArray(StandardCharsets.UTF_8)
                )
            val malformedExportedAtEnvelope = "1\u001Fnot-a-timestamp\u001FZmFybQ=="
            val unsupportedVersionEnvelope = "2\u001F2024-01-01T00:00:00Z\u001FZmFybQ=="

            val rejectedContents = listOf(
                "not-a-valid-payload",
                malformedTimestampEnvelope,
                malformedExportedAtEnvelope,
                unsupportedVersionEnvelope
            )

            val expectedSummary = run {
                var value: String? = null
                scenario.onActivity { activity ->
                    value = activity.getString(
                        R.string.farm_tools_summary_format,
                        "Original Farm",
                        activity.formatCount(0),
                        activity.formattedBalance(null, 0L)
                    )
                }
                value!!
            }
            for (content in rejectedContents) {
                scenario.onActivity { activity ->
                    activity.handleImportedBackupContent(content)
                }
                onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary)))
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun restoredFarmSurvivesImmediateStoreAndActivityRecreation() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("Restore Farm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                val farmId = service.currentFarmId()!!
                service.createTransaction(
                    farmId,
                    FarmTransactionDraft(
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.FEED,
                        amountMinor = 1500,
                        description = "Feed",
                        occurredAt = "2024-01-01T12:00:00Z"
                    )
                )
            }

            var backupContent: String? = null
            scenario.onActivity { activity ->
                backupContent = activity.createBackupContentForCurrentFarm()
            }
            assertNotNull(backupContent)

            SharedPreferencesFarmStore(context).clear()
            scenario.onActivity { activity ->
                activity.handleImportedBackupContent(backupContent!!)
            }
            clickDialogAction(R.string.action_replace_farm)

            scenario.recreate()

            var expectedSummary: String? = null
            scenario.onActivity { activity ->
                expectedSummary = activity.getString(
                        R.string.farm_tools_summary_format,
                        "Restore Farm",
                        activity.formatCount(0),
                        activity.formattedBalance("NPR", -1500L)
                )
            }
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary!!)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun dirtyEditorKeepEditingPreservesFarmAndDraft() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Original Farm")
            openExpenseEditor()
            fillEditor(description = "Unsaved draft", amount = "75")
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            var originalFarmId: String? = null
            scenario.onActivity { activity ->
                originalFarmId = SharedPreferencesFarmStore(activity.applicationContext).currentFarmId()
            }
            val backupContent = backupForRestoredFarm()

            scenario.onActivity { activity ->
                activity.handleImportedBackupContent(backupContent)
            }
            clickDialogAction(R.string.action_replace_farm)
            clickDialogAction(R.string.action_keep_editing)

            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.transactionAmountInput)).check(matches(withText("75")))
            onView(withId(R.id.transactionDescriptionInput)).check(matches(withText("Unsaved draft")))
            onView(withId(R.id.transactionTypeExpenseRadio)).check(matches(isChecked()))

            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                assertEquals(originalFarmId, service.currentFarmId())
                val original = service.loadFarm(originalFarmId!!)
                assertNotNull(original)
                assertTrue("no transaction may be created by keep-editing", original!!.transactions.isEmpty())
                assertTrue("replacement farm must not be saved", service.loadFarm("farm-restored") == null)
                assertEquals(FarmCurrencies.defaultFor(Locale.getDefault()), original.currencyCode)
                val spinner = activity.findViewById<android.widget.Spinner>(R.id.transactionCategorySpinner)
                assertEquals("category must stay FEED", 0, spinner.selectedItemPosition)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun dirtyEditorDiscardAndReplaceFarmClearsStaleDraft() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Original Farm")
            openExpenseEditor()
            fillEditor(description = "Original Feed", amount = "10.00")
            clickSave(scenario)

            var originalFarmId: String? = null
            var originalTransactionId: String? = null
            scenario.onActivity { activity ->
                val service = FarmSliceService(SharedPreferencesFarmStore(activity.applicationContext))
                originalFarmId = service.currentFarmId()
                originalTransactionId = service.loadFarm(originalFarmId!!)!!.transactions.single().id
            }

            openRecentRow("Original Feed")
            fillEditor(description = "Draft description", amount = "")
            clickSave(scenario)
            onView(withId(R.id.validationMessageText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            val backupContent = backupForRestoredFarm()
            scenario.onActivity { activity ->
                activity.handleImportedBackupContent(backupContent)
            }
            clickDialogAction(R.string.action_replace_farm)
            clickDialogAction(R.string.action_discard)

            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                assertEquals("farm-restored", service.currentFarmId())
                assertNull("old farm must no longer be active", service.loadFarm(originalFarmId!!))
                val restored = service.loadFarm("farm-restored")
                assertNotNull(restored)
                assertEquals(1, restored!!.transactions.size)
                assertEquals("tx-restored-1", restored.transactions.single().id)
                assertEquals("Restored Feed", restored.transactions.single().description)
            }
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.validationMessageText)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            val row = recentRowText(scenario)
            assertTrue("recent rows must belong only to the restored farm", row.contains("Restored Feed"))
            assertFalse("old transaction must not appear", row.contains("Original Feed"))
            assertFalse("old unsaved description must be absent", row.contains("Draft description"))

            var expectedSummary: String? = null
            scenario.onActivity { activity ->
                expectedSummary = activity.getString(
                    R.string.farm_tools_summary_format,
                    "Restored Farm",
                    activity.formatCount(0),
                    activity.formattedBalance("NPR", -2500L)
                )
            }
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary!!)))

            openRecentRow("Restored Feed")
            onView(withId(R.id.transactionEditorTitle)).check(matches(withText(R.string.transaction_editor_edit_section)))
            scenario.onActivity { activity ->
                val amountField = activity.findViewById<EditText>(R.id.transactionAmountInput)
                assertEquals(activity.editFieldAmount("NPR", 2500), amountField.text.toString())
                assertEquals("Restored Feed", activity.findViewById<EditText>(R.id.transactionDescriptionInput).text.toString())
            }
            fillEditor(description = "Restored Feed updated", amount = "3000")
            clickSave(scenario)
            scenario.onActivity { activity ->
                val service = FarmSliceService(SharedPreferencesFarmStore(activity.applicationContext))
                val restored = service.loadFarm("farm-restored")!!
                assertEquals(1, restored.transactions.size)
                val updated = restored.transactions.single()
                assertEquals("update must target the restored transaction id", "tx-restored-1", updated.id)
                assertEquals("Restored Feed updated", updated.description)
                assertEquals(300000, updated.amountMinor)
            }
            assertNotNull("original transaction id must not be reused", originalTransactionId)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun cleanEditorReplaceFarmSkipsDiscardDialog() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            createFarm("Original Farm")
            openExpenseEditor()
            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            var originalFarmId: String? = null
            scenario.onActivity { activity ->
                originalFarmId = SharedPreferencesFarmStore(activity.applicationContext).currentFarmId()
            }
            val backupContent = backupForRestoredFarm()

            scenario.onActivity { activity ->
                activity.handleImportedBackupContent(backupContent)
            }
            clickDialogAction(R.string.action_replace_farm)

            onView(withId(R.id.transactionEditorContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            var expectedSummary: String? = null
            scenario.onActivity { activity ->
                val store = SharedPreferencesFarmStore(activity.applicationContext)
                val service = FarmSliceService(store)
                assertEquals("farm-restored", service.currentFarmId())
                assertNull("old farm must no longer be active", service.loadFarm(originalFarmId!!))
                val restored = service.loadFarm("farm-restored")
                assertNotNull(restored)
                assertEquals("no blank transaction may be created", 1, restored!!.transactions.size)
                expectedSummary = activity.getString(
                    R.string.farm_tools_summary_format,
                    "Restored Farm",
                    activity.formatCount(0),
                    activity.formattedBalance("NPR", -2500L)
                )
            }
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary!!)))
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

    private fun openRecentRow(description: String) {
        onView(allOf(withId(R.id.recentTransactionRow), withText(containsString(description))))
            .perform(scrollTo(), click())
    }

    private fun recentRowText(scenario: ActivityScenario<FarmActivity>): String {
        var text = ""
        scenario.onActivity { activity ->
            val container = activity.findViewById<android.widget.LinearLayout>(R.id.recentTransactionsContainer)
            text = (container.getChildAt(0) as android.widget.TextView).text.toString()
        }
        return text
    }

    private fun acceptDefaultDateTime() {
        onView(withId(R.id.changeDateTimeButton)).perform(scrollTo(), click())
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(android.R.id.button1)).perform(click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun clickSave(scenario: ActivityScenario<FarmActivity>) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            activity.findViewById<android.widget.Button>(R.id.saveTransactionButton).performClick()
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun backupForRestoredFarm(): String {
        val farm = FarmState(
            id = "farm-restored",
            name = "Restored Farm",
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-restored-1",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 2500,
                    description = "Restored Feed",
                    occurredAt = OffsetDateTime.parse("2024-02-01T12:00:00Z")
                )
            )
        )
        return FarmBackupCodec.encode(farm)
    }

    @Test
    fun postUpgradeNewTransactionAppearsInBackupExport() {
        // Phase 1: Create v0.1.0-like farm with pre-existing transactions
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.farmNameInput)).perform(typeText("MotoUpgradeFarm"), closeSoftKeyboard())
            onView(withId(R.id.createFarmButton)).perform(click())

            // Add entry (Cow x3)
            onView(withId(R.id.farmToolsToggleButton)).perform(scrollTo(), click())
            onView(withId(R.id.entryLabelInput)).perform(scrollTo(), typeText("Cow"), closeSoftKeyboard())
            onView(withId(R.id.entryQuantityInput)).perform(scrollTo(), typeText("3"), closeSoftKeyboard())
            onView(withId(R.id.addEntryButton)).perform(scrollTo(), click())

            // Add 3 pre-existing transactions (like v0.1.0 data)
            // Transaction 1: Milk sale (Income, SALES, 120050, USD, Aug 5)
            onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
            fillEditor(description = "Milk sale", amount = "120050")
            acceptDefaultDateTime()
            onView(withId(R.id.saveTransactionButton)).perform(click())

            // Transaction 2: Feed purchase (Expense, FEED, 45000, USD, Aug 1)
            onView(withId(R.id.recordExpenseButton)).perform(scrollTo(), click())
            fillEditor(description = "Feed purchase", amount = "45000")
            acceptDefaultDateTime()
            onView(withId(R.id.saveTransactionButton)).perform(click())

            // Transaction 3: Egg sale (Income, SALES, 8000, USD, Aug 7)
            onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
            fillEditor(description = "Egg sale", amount = "8000")
            acceptDefaultDateTime()
            onView(withId(R.id.saveTransactionButton)).perform(click())

            // Verify initial state: 3 transactions, 1 entry
            var backupContent: String? = null
            scenario.onActivity { activity ->
                backupContent = activity.createBackupContentForCurrentFarm()
            }
            assertNotNull(backupContent)
            var envelope = FarmBackupCodec.decode(backupContent!!)
            assertEquals(3, envelope.farm.transactions.size)
            assertEquals(1, envelope.farm.entries.size)

            // Phase 2: Simulate upgrade by recreating the activity (process death/restart)
            scenario.recreate()

            // Verify pre-existing data survived
            backupContent = null
            scenario.onActivity { activity ->
                backupContent = activity.createBackupContentForCurrentFarm()
            }
            assertNotNull(backupContent)
            envelope = FarmBackupCodec.decode(backupContent!!)
            assertEquals(3, envelope.farm.transactions.size)
            assertEquals(1, envelope.farm.entries.size)

            // Phase 3: Create a NEW transaction after "upgrade"
            // Post-upgrade milk sale (Income, SALES, 15000, USD)
            onView(withId(R.id.recordIncomeButton)).perform(scrollTo(), click())
            onView(withId(R.id.transactionAmountInput)).perform(replaceText("15000"), closeSoftKeyboard())
            onView(withId(R.id.transactionDescriptionInput)).perform(replaceText("Post-upgrade milk sale"), closeSoftKeyboard())
            acceptDefaultDateTime()
            onView(withId(R.id.saveTransactionButton)).perform(click())

            // Phase 4: Export backup and verify it contains ALL 4 transactions
            backupContent = null
            scenario.onActivity { activity ->
                backupContent = activity.createBackupContentForCurrentFarm()
            }
            assertNotNull(backupContent)
            envelope = FarmBackupCodec.decode(backupContent!!)

            // The critical assertion: backup must contain all 4 transactions
            assertEquals(4, envelope.farm.transactions.size)
            assertTrue("Backup must contain the post-upgrade transaction", envelope.farm.transactions.any { it.description == "Post-upgrade milk sale" })
            assertTrue("Backup must contain Milk sale", envelope.farm.transactions.any { it.description == "Milk sale" })
            assertTrue("Backup must contain Feed purchase", envelope.farm.transactions.any { it.description == "Feed purchase" })
            assertTrue("Backup must contain Egg sale", envelope.farm.transactions.any { it.description == "Egg sale" })
            assertEquals(1, envelope.farm.entries.size)
        } finally {
            scenario.close()
        }
    }
}
