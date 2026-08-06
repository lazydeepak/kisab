package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.ui.FarmLabels
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies representative English and Nepali resource resolution, including
 * formatted strings, enum coverage, validation and backup messages, and that
 * Nepali resolution never silently falls back to English.
 */
@RunWith(AndroidJUnit4::class)
class LocalizedResourceResolutionTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val english = appContext.withLocale(Locale.ENGLISH)
    private val nepali = appContext.withLocale(Locale("ne"))

    @Test
    fun englishResolvesRepresentativeUiText() {
        assertEquals("Entries", english.getString(R.string.entries_section))
        assertEquals("Create farm", english.getString(R.string.create_farm_action))
        assertEquals("Farm name is required", english.getString(R.string.error_farm_name_required))
        assertEquals("Invalid or unsupported backup", english.getString(R.string.error_backup_invalid_or_unsupported))
    }

    @Test
    fun formattedSummaryResolvesInEnglish() {
        val summary = english.getString(R.string.farm_summary_format, "Demo Farm", "1", "2", "USD 30.00")
        assertEquals("Farm: Demo Farm\nEntry count: 1\nTransaction count: 2\nBalance: USD 30.00", summary)
    }

    @Test
    fun formattedDialogMessageResolvesInEnglish() {
        val message = english.getString(R.string.dialog_import_backup_message_format, "Demo Farm", "Entries: 0\nTransactions: 0")
        assertTrue(message.startsWith("Import backup for Demo Farm?"))
        assertTrue(message.endsWith("This will replace the current farm permanently."))
    }

    @Test
    fun nepaliResolvesRepresentativeUiTextWithoutEnglishFallback() {
        assertNotEquals(english.getString(R.string.entries_section), nepali.getString(R.string.entries_section))
        assertNotEquals(english.getString(R.string.create_farm_action), nepali.getString(R.string.create_farm_action))
        assertNotEquals(english.getString(R.string.error_farm_name_required), nepali.getString(R.string.error_farm_name_required))
        assertNotEquals(
            english.getString(R.string.error_backup_invalid_or_unsupported),
            nepali.getString(R.string.error_backup_invalid_or_unsupported)
        )
    }

    @Test
    fun formattedSummaryResolvesInNepaliWithoutEnglishFallback() {
        val englishSummary = english.getString(R.string.farm_summary_format, "Demo Farm", "1", "2", "USD 30.00")
        val nepaliSummary = nepali.getString(R.string.farm_summary_format, "Demo Farm", "1", "2", "USD 30.00")
        assertTrue(nepaliSummary.isNotBlank())
        assertNotEquals(englishSummary, nepaliSummary)
        assertTrue(nepaliSummary.contains("Demo Farm"))
    }

    @Test
    fun everyEntryKindResolvesInEnglishAndNepali() {
        for (kind in FarmEntryKind.values()) {
            val englishLabel = english.getString(FarmLabels.entryKindRes(kind))
            val nepaliLabel = nepali.getString(FarmLabels.entryKindRes(kind))
            assertTrue("English label blank for ${kind.name}", englishLabel.isNotBlank())
            assertTrue("Nepali label blank for ${kind.name}", nepaliLabel.isNotBlank())
            assertNotEquals("Raw enum name leaked for ${kind.name}", kind.name, englishLabel)
            assertNotEquals("English fallback for ${kind.name}", englishLabel, nepaliLabel)
        }
    }

    @Test
    fun everyTransactionTypeResolvesInEnglishAndNepali() {
        for (type in TransactionType.values()) {
            val englishLabel = english.getString(FarmLabels.transactionTypeRes(type))
            val nepaliLabel = nepali.getString(FarmLabels.transactionTypeRes(type))
            assertTrue("English label blank for ${type.name}", englishLabel.isNotBlank())
            assertTrue("Nepali label blank for ${type.name}", nepaliLabel.isNotBlank())
            assertNotEquals("Raw enum name leaked for ${type.name}", type.name, englishLabel)
            assertNotEquals("English fallback for ${type.name}", englishLabel, nepaliLabel)
        }
    }

    @Test
    fun everyTransactionCategoryResolvesInEnglishAndNepali() {
        for (category in TransactionCategory.values()) {
            val englishLabel = english.getString(FarmLabels.transactionCategoryRes(category))
            val nepaliLabel = nepali.getString(FarmLabels.transactionCategoryRes(category))
            assertTrue("English label blank for ${category.name}", englishLabel.isNotBlank())
            assertTrue("Nepali label blank for ${category.name}", nepaliLabel.isNotBlank())
            assertNotEquals("Raw enum name leaked for ${category.name}", category.name, englishLabel)
            assertNotEquals("English fallback for ${category.name}", englishLabel, nepaliLabel)
        }
    }

    @Test
    fun validationAndBackupMessagesResolveInBothLocales() {
        val validationKeys = listOf(
            R.string.error_farm_name_required,
            R.string.error_transaction_date_time_invalid,
            R.string.error_transaction_selection_required,
            R.string.error_unexpected
        )
        val backupKeys = listOf(
            R.string.error_backup_invalid_or_unsupported,
            R.string.error_backup_too_large_or_unreadable
        )
        for (key in validationKeys + backupKeys) {
            val englishText = english.getString(key)
            val nepaliText = nepali.getString(key)
            assertTrue("English text blank for $key", englishText.isNotBlank())
            assertTrue("Nepali text blank for $key", nepaliText.isNotBlank())
            assertNotEquals("English fallback for $key", englishText, nepaliText)
        }
    }
}
