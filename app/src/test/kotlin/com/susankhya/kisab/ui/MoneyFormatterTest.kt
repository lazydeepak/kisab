package com.susankhya.kisab.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    private val formatter = MoneyFormatter()
    private val ne = Locale.forLanguageTag("ne-NP")

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
    fun pseudoCurrencyNegativeFractionDigitsFallBackToTwo() {
        assertEquals(2, formatter.fractionDigits("XXX"))
    }

    @Test
    fun pseudoCurrencyFormatsAndParsesSafely() {
        assertEquals("XXX 15.00", formatter.format(Locale.US, "XXX", 1500))
        assertEquals("15.00", formatter.toEditFieldValue(Locale.US, "XXX", 1500))
        assertEquals(MoneyInputResult.Valid(1500), MoneyInputParser(formatter).parse(Locale.US, "XXX", "15.00"))
    }

    @Test
    fun formatsNprWithTwoFractionDigitsAndRupeeSymbol() {
        assertEquals("रु 123.45", formatter.format(Locale.US, "NPR", 12345))
    }

    @Test
    fun formatsInrWithRupeeSymbol() {
        assertEquals("₹ 1,234.56", formatter.format(Locale.US, "INR", 123456))
    }

    @Test
    fun formatsJpyWithZeroFractionDigitsAndYenSymbol() {
        assertEquals("¥ 1,500", formatter.format(Locale.US, "JPY", 1500))
    }

    @Test
    fun formatsUsdWithDollarSymbol() {
        assertEquals("$ 1,234.56", formatter.format(Locale.US, "USD", 123456))
    }

    @Test
    fun disambiguatesDollarCurrenciesWithCompactPrefixes() {
        assertEquals("A$ 1,234.56", formatter.format(Locale.US, "AUD", 123456))
        assertEquals("C$ 1,234.56", formatter.format(Locale.US, "CAD", 123456))
    }

    @Test
    fun disambiguatesSharedSymbolsAcrossSupportedCurrencies() {
        assertEquals("¥ 1,500", formatter.format(Locale.US, "JPY", 1500))
        assertEquals("CN¥ 15.00", formatter.format(Locale.US, "CNY", 1500))
        assertEquals("Rs 1,234.56", formatter.format(Locale.US, "LKR", 123456))
        assertEquals("Rs 1,234.56", formatter.format(Locale.US, "PKR", 123456))
    }

    @Test
    fun formatsKwdWithThreeFractionDigitsAndDinarPrefix() {
        assertEquals("KD 1.500", formatter.format(Locale.US, "KWD", 1500))
    }

    @Test
    fun everyGovernedCurrencyHasADeterministicSymbol() {
        val expected = mapOf(
            "NPR" to "रु", "INR" to "₹", "USD" to "$", "JPY" to "¥",
            "EUR" to "€", "GBP" to "£", "AUD" to "A$", "CAD" to "C$",
            "CNY" to "CN¥", "BDT" to "৳", "LKR" to "Rs", "PKR" to "Rs",
            "SAR" to "SR", "AED" to "AED", "QAR" to "QR", "KWD" to "KD", "THB" to "฿"
        )
        assertEquals(expected, FarmCurrencies.SUPPORTED.associateWith { CurrencySymbols.symbolFor(it) })
    }

    @Test
    fun unknownValidCodeFallsBackToIsoCode() {
        assertEquals("ZZZ 15.00", formatter.format(Locale.US, "ZZZ", 1500))
    }

    @Test
    fun lowercaseInputIsNormalizedBeforeSymbolLookup() {
        assertEquals("रु 123.45", formatter.format(Locale.US, "npr", 12345))
    }

    @Test
    fun formatsNegativeAndZeroBalances() {
        assertEquals("-$ 1,234.56", formatter.format(Locale.US, "USD", -123456))
        assertEquals("$ 0.00", formatter.format(Locale.US, "USD", 0))
    }

    @Test
    fun formatsExtremeValuesWithoutOverflow() {
        assertEquals("$ 0.01", formatter.format(Locale.US, "USD", 1))
        assertEquals("-$ 0.01", formatter.format(Locale.US, "USD", -1))
        assertEquals("$ 92,233,720,368,547,758.07", formatter.format(Locale.US, "USD", Long.MAX_VALUE))
        assertEquals("-$ 92,233,720,368,547,758.08", formatter.format(Locale.US, "USD", Long.MIN_VALUE))
        assertEquals("-92233720368547758.08", formatter.toEditFieldValue(Locale.US, "USD", Long.MIN_VALUE))
    }

    @Test
    fun formatsWithLocaleDigitsAndGrouping() {
        assertEquals("रु १,२३४.५६", formatter.format(ne, "NPR", 123456))
    }

    @Test
    fun hidesCurrencySymbolWhenRequested() {
        assertEquals("15.00", formatter.format(Locale.US, "XXX", 1500, showCurrency = false))
        assertEquals("123.45", formatter.format(Locale.US, "NPR", 12345, showCurrency = false))
        assertEquals("१,२३४.५६", formatter.format(ne, "NPR", 123456, showCurrency = false))
        assertEquals("-1,234.56", formatter.format(Locale.US, "USD", -123456, showCurrency = false))
    }

    @Test
    fun disablesNumberGroupingWhenRequested() {
        assertEquals("रु 123456.78", formatter.format(Locale.US, "NPR", 12345678, grouping = false))
        assertEquals("¥ 1500", formatter.format(Locale.US, "JPY", 1500, grouping = false))
    }

    @Test
    fun hidesCurrencyAndGroupingTogether() {
        assertEquals("123456.78", formatter.format(Locale.US, "NPR", 12345678, showCurrency = false, grouping = false))
    }

    @Test
    fun defaultFormatterBehaviorRemainsDeterministic() {
        val a = formatter.format(Locale.US, "NPR", 123456)
        val b = formatter.format(Locale.US, "NPR", 123456)
        assertEquals(a, b)
        assertEquals("₹ 1,234.56", formatter.format(Locale.US, "INR", 123456))
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
