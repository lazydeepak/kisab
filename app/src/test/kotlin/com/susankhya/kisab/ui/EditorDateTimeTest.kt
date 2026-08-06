package com.susankhya.kisab.ui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorDateTimeTest {

    @Test
    fun kathmanduPickerValuesProduceExpectedUtcInstant() {
        val offset = EditorDateTime.fromPickerValues(2024, 0, 1, 17, 45, ZoneId.of("Asia/Kathmandu"))
        assertEquals(OffsetDateTime.of(2024, 1, 1, 17, 45, 0, 0, ZoneOffset.ofHoursMinutes(5, 45)), offset)
        assertEquals("2024-01-01T12:00:00Z", offset.toInstant().toString())
    }

    @Test
    fun utcZoneKeepsPickerValuesAsIs() {
        val offset = EditorDateTime.fromPickerValues(2024, 5, 15, 8, 30, ZoneId.of("UTC"))
        assertEquals(OffsetDateTime.of(2024, 6, 15, 8, 30, 0, 0, ZoneOffset.UTC), offset)
    }

    @Test
    fun dstGapResolvesForwardUsingZoneRules() {
        val offset = EditorDateTime.fromPickerValues(2024, 2, 10, 2, 30, ZoneId.of("America/New_York"))
        assertEquals("2024-03-10T07:30:00Z", offset.toInstant().toString())
    }

    @Test
    fun dstOverlapResolvesToEarlierOffsetUsingZoneRules() {
        val offset = EditorDateTime.fromPickerValues(2024, 10, 3, 1, 30, ZoneId.of("America/New_York"))
        assertEquals("2024-11-03T05:30:00Z", offset.toInstant().toString())
    }
}
