package com.susankhya.kisab.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyInputParserTest {

    private val parser = MoneyInputParser()
    private val ne = Locale("ne", "NP")

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
