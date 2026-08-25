package com.susankhya.kisab.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    private val formatter = NumberFormatter()
    private val ne = Locale.forLanguageTag("ne-NP")

    @Test
    fun formatsSmallIntegersInEnglish() {
        assertEquals("3", formatter.format(Locale.US, 3))
        assertEquals("0", formatter.format(Locale.US, 0))
    }

    @Test
    fun groupsIntegersInEnglish() {
        assertEquals("1,234", formatter.format(Locale.US, 1234))
    }

    @Test
    fun formatsIntegersWithLocalizedDigits() {
        assertEquals("३", formatter.format(ne, 3))
        assertEquals("१,२३४", formatter.format(ne, 1234))
    }

    @Test
    fun disablesNumberGroupingWhenRequested() {
        assertEquals("1234", formatter.format(Locale.US, 1234, grouping = false))
        assertEquals("१२३४", formatter.format(ne, 1234, grouping = false))
    }
}
