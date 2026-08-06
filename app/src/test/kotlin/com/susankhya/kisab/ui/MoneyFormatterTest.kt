package com.susankhya.kisab.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    private val formatter = MoneyFormatter()
    private val ne = Locale("ne", "NP")

    @Test
    fun fractionDigitsFollowCurrencyDefinition() {
        assertEquals(2, formatter.fractionDigits("USD"))
        assertEquals(2, formatter.fractionDigits("NPR"))
        assertEquals(0, formatter.fractionDigits("JPY"))
        assertEquals(3, formatter.fractionDigits("KWD"))
    }

    @Test
    fun unknownValidCodeFallsBackToTwoFractionDigits() {
        assertEquals(2, formatter.fractionDigits("ZZZ"))
    }

    @Test
    fun formatsNprWithTwoFractionDigits() {
        assertEquals("123.45 NPR", formatter.format(Locale.US, "NPR", 12345))
    }

    @Test
    fun formatsJpyWithZeroFractionDigits() {
        assertEquals("1,500 JPY", formatter.format(Locale.US, "JPY", 1500))
    }

    @Test
    fun formatsKwdWithThreeFractionDigits() {
        assertEquals("1.500 KWD", formatter.format(Locale.US, "KWD", 1500))
    }

    @Test
    fun unknownValidCodeFormatsWithIsoCode() {
        assertEquals("15.00 ZZZ", formatter.format(Locale.US, "ZZZ", 1500))
    }

    @Test
    fun formatsNegativeAndZeroBalances() {
        assertEquals("-1,234.56 USD", formatter.format(Locale.US, "USD", -123456))
        assertEquals("0.00 USD", formatter.format(Locale.US, "USD", 0))
    }

    @Test
    fun formatsWithLocaleDigitsAndGrouping() {
        assertEquals("१,२३४.५६ NPR", formatter.format(ne, "NPR", 123456))
    }

    @Test
    fun editFieldValueHasNoGroupingAndExactFractionDigits() {
        assertEquals("1234.56", formatter.toEditFieldValue(Locale.US, "USD", 123456))
        assertEquals("1500", formatter.toEditFieldValue(Locale.US, "JPY", 1500))
        assertEquals("1.500", formatter.toEditFieldValue(Locale.US, "KWD", 1500))
        assertEquals("१२३४.५६", formatter.toEditFieldValue(ne, "NPR", 123456))
    }

    @Test
    fun editFieldValueRoundTripsThroughParser() {
        val parser = MoneyInputParser(formatter)
        for (value in listOf(1L, 12345L, 123456L, 9223372036854775807L)) {
            for (code in listOf("NPR", "JPY", "KWD")) {
                val edit = formatter.toEditFieldValue(Locale.US, code, value)
                val parsed = parser.parse(Locale.US, code, edit)
                assertEquals("Round trip failed for $code $value ($edit)", MoneyInputResult.Valid(value), parsed)
            }
        }
    }
}
