package com.susankhya.kisab.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyInputParserTest {

    private val parser = MoneyInputParser()
    private val ne = Locale.forLanguageTag("ne-NP")

    @Test
    fun parsesPlainMajorUnitAmounts() {
        assertEquals(MoneyInputResult.Valid(12345), parser.parse(Locale.US, "USD", "123.45"))
        assertEquals(MoneyInputResult.Valid(123456), parser.parse(Locale.US, "USD", "1234.56"))
    }

    @Test
    fun parsesWithGroupingSeparators() {
        assertEquals(MoneyInputResult.Valid(123456), parser.parse(Locale.US, "USD", "1,234.56"))
        assertEquals(MoneyInputResult.Valid(12345656), parser.parse(Locale.US, "USD", "123,456.56"))
        assertEquals(MoneyInputResult.Valid(1234567890), parser.parse(Locale.US, "USD", "12,345,678.90"))
    }

    @Test
    fun groupingValidatesStandardAndIndianPatterns() {
        val enNp = Locale.forLanguageTag("en-NP")
        val valid = mapOf(
            "1,234" to 123400L,
            "12,345" to 1234500L,
            "1,234,567" to 123456700L,
            "12,345,678" to 1234567800L,
            "1,23,456" to 12345600L,
            "12,34,567" to 123456700L,
            "1,23,45,678" to 1234567800L,
            "12,34,56,789" to 12345678900L,
            "1,234.56" to 123456L,
            "12,34,567.89" to 123456789L,
        )
        for (locale in listOf(Locale.US, enNp, ne)) {
            for ((input, expected) in valid) {
                assertEquals("$locale '$input'", MoneyInputResult.Valid(expected), parser.parse(locale, "USD", input))
            }
        }
    }

    @Test
    fun malformedGroupingIsRejectedAcrossLocales() {
        val enNp = Locale.forLanguageTag("en-NP")
        val invalid = listOf("1,2", "1,23", "12,34", "123,45", "1,234,56", "1,23,456,78", "1,2345", "1234,567", "12,34.56", "1,23.4")
        for (locale in listOf(Locale.US, enNp, ne)) {
            for (input in invalid) {
                assertEquals("$locale '$input'", MoneyInputResult.Invalid, parser.parse(locale, "USD", input))
            }
        }
    }

    @Test
    fun neNpGroupingRulesHoldForDevanagariDigits() {
        assertEquals(MoneyInputResult.Valid(12345600), parser.parse(ne, "NPR", "१,२३,४५६"))
        assertEquals(MoneyInputResult.Valid(12345678900L), parser.parse(ne, "NPR", "१२,३४,५६,७८९"))
        assertEquals(MoneyInputResult.Invalid, parser.parse(ne, "NPR", "१,२३४५"))
        assertEquals(MoneyInputResult.Invalid, parser.parse(ne, "NPR", "१,२"))
    }

    @Test
    fun parsesLocalizedDigitsAndSeparators() {
        assertEquals(MoneyInputResult.Valid(12345), parser.parse(ne, "NPR", "१२३.४५"))
        assertEquals(MoneyInputResult.Valid(123456), parser.parse(ne, "NPR", "१,२३४.५६"))
        assertEquals(MoneyInputResult.Valid(123456), parser.parse(ne, "NPR", "१२३४.५६"))
    }

    @Test
    fun padsFractionToCurrencyDigits() {
        assertEquals(MoneyInputResult.Valid(1230), parser.parse(Locale.US, "USD", "12.3"))
        assertEquals(MoneyInputResult.Valid(30), parser.parse(Locale.US, "USD", "0.3"))
    }

    @Test
    fun respectsZeroFractionCurrencies() {
        assertEquals(MoneyInputResult.Valid(1500), parser.parse(Locale.US, "JPY", "1500"))
        assertEquals(MoneyInputResult.TooPrecise, parser.parse(Locale.US, "JPY", "1500.5"))
    }

    @Test
    fun respectsThreeFractionCurrencies() {
        assertEquals(MoneyInputResult.Valid(1500), parser.parse(Locale.US, "KWD", "1.500"))
    }

    @Test
    fun rejectsMoreFractionDigitsThanCurrencyAllows() {
        assertEquals(MoneyInputResult.TooPrecise, parser.parse(Locale.US, "NPR", "12.345"))
        assertEquals(MoneyInputResult.TooPrecise, parser.parse(Locale.US, "USD", "12.345"))
    }

    @Test
    fun missingInputIsMissing() {
        assertEquals(MoneyInputResult.Missing, parser.parse(Locale.US, "USD", ""))
        assertEquals(MoneyInputResult.Missing, parser.parse(Locale.US, "USD", "   "))
    }

    @Test
    fun zeroAndNegativeInputsAreNotPositive() {
        assertEquals(MoneyInputResult.NotPositive, parser.parse(Locale.US, "USD", "0"))
        assertEquals(MoneyInputResult.NotPositive, parser.parse(Locale.US, "USD", "0.00"))
        assertEquals(MoneyInputResult.NotPositive, parser.parse(Locale.US, "USD", "-5"))
        assertEquals(MoneyInputResult.NotPositive, parser.parse(Locale.US, "USD", "-5.50"))
    }

    @Test
    fun malformedInputIsInvalid() {
        for (input in listOf("abc", "1a2", "12e3", "12+3", "12.3.4", ".50", "50.", "12-3", "1,,2", ",123", "123,", "12 3.45")) {
            assertEquals("Expected Invalid for '$input'", MoneyInputResult.Invalid, parser.parse(Locale.US, "USD", input))
        }
    }

    @Test
    fun overflowInputIsTooLarge() {
        assertEquals(
            MoneyInputResult.Valid(9223372036854775807L),
            parser.parse(Locale.US, "USD", "92233720368547758.07")
        )
        assertEquals(
            MoneyInputResult.TooLarge,
            parser.parse(Locale.US, "USD", "92233720368547758.08")
        )
        assertEquals(MoneyInputResult.TooLarge, parser.parse(Locale.US, "USD", "99999999999999999999.00"))
    }
}
