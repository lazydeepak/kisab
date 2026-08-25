package com.susankhya.kisab.ui

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalValueFormatterTest {
    private val formatter = DecimalValueFormatter()

    @Test fun parsesEnglishAndNepaliDigitsWithoutPartialAcceptance() {
        assertDecimal("1234.5", formatter.parse(Locale.ENGLISH, "1,234.5")!!)
        assertDecimal("1234.5", formatter.parse(Locale.forLanguageTag("ne"), "१,२३४.५")!!)
        assertNull(formatter.parse(Locale.ENGLISH, "12x"))
        assertNull(formatter.parse(Locale.ENGLISH, ""))
    }

    @Test fun formatsWithoutUnboundedDecimalNoise() {
        assertEquals("1,234.56789", formatter.format(Locale.ENGLISH, BigDecimal("1234.56789")))
        assertEquals("0.333333", formatter.format(Locale.ENGLISH, BigDecimal.ONE.divide(BigDecimal("3"), 20, java.math.RoundingMode.HALF_UP)))
    }

    @Test fun disablesNumberGroupingWhenRequested() {
        assertEquals("1234.56789", formatter.format(Locale.ENGLISH, BigDecimal("1234.56789"), grouping = false))
    }

    @Test fun parsesLocalizedWholeCountsAndRejectsInvalidCounts() {
        assertEquals(12, formatter.parseNonNegativeWhole(Locale.ENGLISH, "12"))
        assertEquals(12, formatter.parseNonNegativeWhole(Locale.forLanguageTag("ne"), "१२"))
        assertEquals(12, formatter.parseNonNegativeWhole(Locale.ENGLISH, "12.0"))
        assertNull(formatter.parseNonNegativeWhole(Locale.ENGLISH, "12.5"))
        assertNull(formatter.parseNonNegativeWhole(Locale.ENGLISH, "-1"))
        assertNull(formatter.parseNonNegativeWhole(Locale.ENGLISH, "999999999999"))
    }

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))
}
