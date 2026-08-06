package com.susankhya.kisab.ui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimePresentationTest {

    private val presentation = TimePresentation()
    private val kathmandu = ZoneId.of("Asia/Kathmandu")

    @Test
    fun displaysStoredInstantInDeviceZoneWithoutUtcLiteral() {
        val stored = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        val english = presentation.displayDateTime(Locale.ENGLISH, kathmandu, stored)
        assertEquals("Jan 1, 2024, 5:45:00\u202FPM", english)
        assertFalse("UTC literal leaked into display: $english", english.contains("UTC"))

        val nepali = presentation.displayDateTime(Locale("ne", "NP"), kathmandu, stored)
        assertEquals("2024 जनवरी 1, 17:45:00", nepali)
        assertFalse("UTC literal leaked into display: $nepali", nepali.contains("UTC"))
    }

    @Test
    fun editFieldValueUsesDeviceLocalOffset() {
        val stored = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        assertEquals("2024-01-01T17:45:00+05:45", presentation.toEditFieldValue(kathmandu, stored))
    }

    @Test
    fun editFieldValueRoundTripsToTheSameInstant() {
        val stored = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        val editValue = presentation.toEditFieldValue(kathmandu, stored)
        val parsed = OffsetDateTime.parse(editValue)
        assertEquals(stored.toInstant(), parsed.toInstant())
        assertEquals(OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC), parsed.withOffsetSameInstant(ZoneOffset.UTC))
    }

    @Test
    fun historicalDatesUseHistoricalOffsetForTheZone() {
        val stored = OffsetDateTime.parse("2024-06-01T12:00:00Z")
        val newYork = ZoneId.of("America/New_York")
        val editValue = presentation.toEditFieldValue(newYork, stored)
        assertTrue("Expected -04:00 offset, was: $editValue", editValue.endsWith("-04:00"))
        assertEquals(stored.toInstant(), OffsetDateTime.parse(editValue).toInstant())
    }
}
