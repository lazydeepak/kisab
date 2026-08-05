package com.susankhya.kisab

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
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
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

            val expectedSummary = context.getString(
                R.string.farm_summary_format,
                "Demo Farm",
                0,
                0,
                0,
                ""
            )
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary)))
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

            val expectedSummary = context.getString(
                R.string.farm_summary_format,
                "Original Farm",
                0,
                0,
                0,
                ""
            )
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary)))
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

            val expectedSummary = context.getString(
                R.string.farm_summary_format,
                "Original Farm",
                0,
                0,
                0,
                ""
            )
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary)))
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

            val expectedSummary = context.getString(
                R.string.farm_summary_format,
                "Original Farm",
                0,
                0,
                0,
                ""
            )
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
                        currency = "USD",
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

            val expectedSummary = context.getString(
                R.string.farm_summary_format,
                "Restore Farm",
                0,
                1,
                -1500,
                " USD"
            )
            onView(withId(R.id.summaryText)).check(matches(withText(expectedSummary)))
        } finally {
            scenario.close()
        }
    }
}
