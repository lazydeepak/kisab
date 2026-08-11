package com.susankhya.kisab

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
import java.util.Locale
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests verifying FarmActivity renders its clean first-run and populated
 * screens in English and Nepali, independent of execution order and existing
 * device data. The app-level locale is applied with the system
 * [android.app.LocaleManager] API and restored afterward.
 */
@RunWith(AndroidJUnit4::class)
class FarmActivityLocalizationSmokeTest {

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

    private fun stringFor(locale: Locale, resId: Int): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config).getString(resId)
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

    private fun seedFarm() {
        val store = SharedPreferencesFarmStore(context)
        val service = FarmSliceService(store)
        val farm = service.createFarm("Demo Farm")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 3))
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 1500,
                description = "Feed",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
    }

    @Test
    fun cleanFirstRunScreenRendersInEnglish() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        onView(withId(R.id.farmNameInput)).check(matches(isDisplayed()))
        onView(withId(R.id.createFarmButton)).check(matches(withText(R.string.create_farm_action)))
        onView(withId(R.id.createFarmButton)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun cleanFirstRunScreenRendersInNepali() {
        setAppLocale(Locale.forLanguageTag("ne"))
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        onView(withId(R.id.farmNameInput)).check(matches(isDisplayed()))
        scenario.onActivity { activity ->
            val expectedNepali = activity.getString(R.string.farm_name_hint)
            val expectedEnglish = stringFor(Locale.ENGLISH, R.string.farm_name_hint)
            val actualHint = activity.findViewById<EditText>(R.id.farmNameInput).hint?.toString()
            assertEquals("FarmActivity did not render the Nepali hint", expectedNepali, actualHint)
            assertNotEquals(expectedEnglish, actualHint)
        }
        scenario.close()
    }

    @Test
    fun populatedFarmScreenRendersInEnglish() {
        seedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        onView(withId(R.id.summaryText)).check(matches(withText(containsString("Farm: Demo Farm"))))
        onView(withId(R.id.exportBackupButton)).check(matches(withText(R.string.export_backup_action)))
        onView(withId(R.id.importBackupButton)).check(matches(withText(R.string.import_backup_action)))
        onView(withId(R.id.entriesText)).check(matches(withText(containsString("Goat"))))
        scenario.onActivity { activity ->
            val row = activity.findViewById<LinearLayout>(R.id.recentTransactionsContainer).getChildAt(0) as TextView
            assertTrue("Expected seeded transaction in recent rows", row.text.contains("Feed"))
        }
        scenario.close()
    }

    @Test
    fun populatedFarmScreenRendersInNepali() {
        setAppLocale(Locale.forLanguageTag("ne"))
        seedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        scenario.onActivity { activity ->
            val actualSummary = activity.findViewById<TextView>(R.id.summaryText).text.toString()
            val exportLabel = activity.findViewById<android.widget.Button>(R.id.exportBackupButton).text.toString()
            assertTrue("Expected Nepali summary prefix, was: $actualSummary", actualSummary.startsWith("फार्म: Demo Farm"))
            assertTrue("Expected Nepali labels in summary, was: $actualSummary", actualSummary.contains("प्रविष्टि सङ्ख्या:"))
            assertFalse("English summary leaked into Nepali: $actualSummary", actualSummary.startsWith("Farm:"))
            val expectedExportLabel = activity.getString(R.string.export_backup_action)
            assertEquals(expectedExportLabel, exportLabel)
            assertNotEquals(stringFor(Locale.ENGLISH, R.string.export_backup_action), exportLabel)
        }
        scenario.close()
    }
}
