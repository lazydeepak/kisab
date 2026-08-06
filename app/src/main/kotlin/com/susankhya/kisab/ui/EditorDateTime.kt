package com.susankhya.kisab.ui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Combines date/time picker values into an offset-bearing date-time resolved
 * in the current device zone.
 *
 * `ZonedDateTime.of` applies the zone rules deterministically: overlapping
 * times resolve to the earlier offset and gap times are shifted forward by
 * the gap duration. The domain then normalizes the resulting instant to UTC.
 */
object EditorDateTime {
    fun fromPickerValues(
        year: Int,
        pickerMonth: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int,
        zoneId: ZoneId
    ): OffsetDateTime =
        ZonedDateTime.of(year, pickerMonth + 1, dayOfMonth, hourOfDay, minute, 0, 0, zoneId).toOffsetDateTime()
}
