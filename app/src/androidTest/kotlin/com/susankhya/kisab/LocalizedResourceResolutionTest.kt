package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.ArithmeticOperation
import com.susankhya.kisab.domain.LandUnit
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
    private val nepali = appContext.withLocale(Locale.forLanguageTag("ne"))

    @Test
    fun englishResolvesRepresentativeUiText() {
        assertEquals("Entries", english.getString(R.string.entries_section))
        assertEquals("Create farm", english.getString(R.string.create_farm_action))
        assertEquals("Record income", english.getString(R.string.record_income_action))
        assertEquals("Farm name is required", english.getString(R.string.error_farm_name_required))
        assertEquals("Invalid or unsupported backup", english.getString(R.string.error_backup_invalid_or_unsupported))
        assertEquals("More options", english.getString(R.string.shell_menu_content_description))
    }

    @Test
    fun formattedSummaryResolvesInEnglish() {
        val summary = english.getString(R.string.farm_tools_summary_format, "Demo Farm", "1", "USD 30.00")
        assertEquals("Farm: Demo Farm\nEntry count: 1\nBalance: USD 30.00", summary)
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
        assertNotEquals(english.getString(R.string.record_income_action), nepali.getString(R.string.record_income_action))
        assertNotEquals(english.getString(R.string.error_farm_name_required), nepali.getString(R.string.error_farm_name_required))
        assertNotEquals(
            english.getString(R.string.error_backup_invalid_or_unsupported),
            nepali.getString(R.string.error_backup_invalid_or_unsupported)
        )
        assertNotEquals(
            english.getString(R.string.shell_menu_content_description),
            nepali.getString(R.string.shell_menu_content_description)
        )
    }

    @Test
    fun formattedSummaryResolvesInNepaliWithoutEnglishFallback() {
        val englishSummary = english.getString(R.string.farm_tools_summary_format, "Demo Farm", "1", "USD 30.00")
        val nepaliSummary = nepali.getString(R.string.farm_tools_summary_format, "Demo Farm", "1", "USD 30.00")
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
            R.string.error_currency_iso_three_letters,
            R.string.error_currency_locked,
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

    @Test
    fun todayTimeFormatResolvesInBothLocales() {
        val englishToday = english.getString(R.string.today_label)
        val englishShort = english.getString(
            R.string.today_time_format,
            englishToday,
            "5:45 PM"
        )
        assertEquals("$englishToday, 5:45 PM", englishShort)

        val nepaliToday = nepali.getString(R.string.today_label)
        val nepaliShort = nepali.getString(
            R.string.today_time_format,
            nepaliToday,
            "5:45 PM"
        )
        assertNotEquals("English fallback for today_time_format", englishShort, nepaliShort)
        assertTrue(nepaliShort.contains(nepaliToday))
        assertTrue(nepaliShort.contains(", "))
    }

    @Test
    fun dailyEntryCurrencyStringsResolveInBothLocales() {
        val keys = listOf(
            R.string.currency_choice_dialog_title,
            R.string.currency_iso_hint,
            R.string.farm_currency_label,
            R.string.currency_locked_note,
            R.string.action_ok,
            R.string.action_cancel
        )
        for (key in keys) {
            val englishText = english.getString(key)
            val nepaliText = nepali.getString(key)
            assertTrue("English text blank for $key", englishText.isNotBlank())
            assertTrue("Nepali text blank for $key", nepaliText.isNotBlank())
            assertNotEquals("English fallback for $key", englishText, nepaliText)
        }
    }

    @Test
    fun shellNavigationStringsResolveInBothLocales() {
        val keys = listOf(
            R.string.nav_home,
            R.string.nav_hisab_kitab,
            R.string.nav_hisab,
            R.string.nav_settings,
            R.string.settings_action,
            R.string.hisab_calculator_title,
            R.string.hisab_calculator_body,
            R.string.hisab_no_farm,
            R.string.hisab_no_parties,
            R.string.hisab_party_label,
            R.string.hisab_party_role_format,
            R.string.hisab_period_activity_section,
            R.string.hisab_position_section,
            R.string.hisab_activity_empty,
            R.string.hisab_position_empty,
            R.string.settings_farm_currency_section,
            R.string.settings_language_section,
            R.string.settings_no_farm_text,
            R.string.language_follow_device,
            R.string.language_english,
            R.string.language_nepali
        )
        for (key in keys) {
            val englishText = english.getString(key)
            val nepaliText = nepali.getString(key)
            assertTrue("English text blank for $key", englishText.isNotBlank())
            assertTrue("Nepali text blank for $key", nepaliText.isNotBlank())
            assertNotEquals("English fallback for $key", englishText, nepaliText)
        }
    }

    @Test
    fun calculatorLabelsResolveInEnglishAndNepali() {
        val resourceIds = ArithmeticOperation.values().map(FarmLabels::arithmeticOperationRes) +
            LandUnit.values().map(FarmLabels::landUnitRes) +
            com.susankhya.kisab.domain.FarmPlanningCalculator.values()
                .map(FarmLabels::farmPlanningCalculatorRes) + listOf(
                R.string.kisan_tools_title,
                R.string.kisan_tools_body,
                R.string.profit_title,
                R.string.interest_title,
                R.string.land_converter_title,
                R.string.calculator_input_error,
                R.string.farm_planning_title,
                R.string.seed_result_format,
                R.string.fertilizer_result_format,
                R.string.feed_result_format,
                R.string.milk_result_format,
                R.string.crop_yield_result_format
            )
        for (resourceId in resourceIds) {
            val englishText = english.getString(resourceId)
            val nepaliText = nepali.getString(resourceId)
            assertTrue("English text blank for $resourceId", englishText.isNotBlank())
            assertTrue("Nepali text blank for $resourceId", nepaliText.isNotBlank())
            assertNotEquals("English fallback for $resourceId", englishText, nepaliText)
        }
    }

    private fun assertContentStringsResolveInBothLocales(keys: List<Int>, placeholderOnlyKeys: List<Int> = emptyList()) {
        for (key in keys) {
            val englishText = english.getString(key)
            val nepaliText = nepali.getString(key)
            assertTrue("English text blank for $key", englishText.isNotBlank())
            assertTrue("Nepali text blank for $key", nepaliText.isNotBlank())
            if (key !in placeholderOnlyKeys) {
                assertNotEquals("English fallback for $key", englishText, nepaliText)
            }
        }
    }

    @Test
    fun partyStringsResolveInBothLocales() {
        val keys = listOf(
            R.string.parties_section,
            R.string.parties_empty,
            R.string.add_party_action,
            R.string.party_editor_title,
            R.string.party_name_label,
            R.string.party_name_hint,
            R.string.party_role_label,
            R.string.party_contact_label,
            R.string.party_contact_hint,
            R.string.party_notes_label,
            R.string.party_notes_hint,
            R.string.party_role_customer,
            R.string.party_role_supplier,
            R.string.party_role_both,
            R.string.party_role_other,
            R.string.save_party_action,
            R.string.error_party_name_required,
            R.string.party_row_format,
            R.string.dialog_delete_party_title,
            R.string.dialog_delete_party_message,
            R.string.delete_party_action,
            R.string.toast_party_saved,
            R.string.toast_party_deleted
        )
        assertContentStringsResolveInBothLocales(keys, placeholderOnlyKeys = listOf(R.string.party_row_format))
    }

    @Test
    fun tradeStringsResolveInBothLocales() {
        val keys = listOf(
            R.string.trade_type_sale,
            R.string.trade_type_purchase,
            R.string.new_sale_action,
            R.string.new_purchase_action,
            R.string.trade_editor_new_sale,
            R.string.trade_editor_new_purchase,
            R.string.trade_editor_edit_sale,
            R.string.trade_editor_edit_purchase,
            R.string.save_sale_action,
            R.string.save_purchase_action,
            R.string.update_trade_action,
            R.string.cancel_trade_action,
            R.string.delete_trade_action,
            R.string.total_amount_label,
            R.string.trade_party_label,
            R.string.trade_party_none,
            R.string.trade_party_hint,
            R.string.trade_description_hint,
            R.string.payment_status_label,
            R.string.payment_status_paid,
            R.string.payment_status_partial,
            R.string.payment_status_unpaid,
            R.string.amount_paid_label,
            R.string.amount_due_label,
            R.string.to_receive_label,
            R.string.to_pay_label,
            R.string.to_receive_summary_format,
            R.string.to_pay_summary_format,
            R.string.cash_sale_label,
            R.string.cash_purchase_label,
            R.string.trades_section,
            R.string.trades_empty,
            R.string.trade_row_format,
            R.string.trade_row_status_due_format,
            R.string.trade_row_paid,
            R.string.trade_row_time_format,
            R.string.dialog_delete_trade_title,
            R.string.dialog_delete_trade_message,
            R.string.error_party_has_trades,
            R.string.error_party_role_incompatible,
            R.string.error_trade_party_required,
            R.string.error_trade_paid_out_of_range,
            R.string.error_trade_total_required,
            R.string.toast_trade_created,
            R.string.toast_trade_updated,
            R.string.toast_trade_deleted
        )
        assertContentStringsResolveInBothLocales(
            keys,
            placeholderOnlyKeys = listOf(
                R.string.trade_row_format,
                R.string.trade_row_time_format
            )
        )
    }
}
