package com.susankhya.kisab.ui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Presents stored UTC instants in the device timezone.
 *
 * Stored data stays in UTC; only presentation converts to the device zone.
 * The edit field carries an ISO-8601 offset date-time of the device-local
 * instant, so saving an unchanged value preserves the exact stored instant.
 * The "today" helpers let the UI render a compact label for the current local
 * day and the full localized date/time otherwise.
 */
class TimePresentation {

    fun displayDateTime(locale: Locale, zoneId: ZoneId, stored: OffsetDateTime): String =
        stored.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).withZone(zoneId))

    fun shortTime(locale: Locale, zoneId: ZoneId, stored: OffsetDateTime): String =
        stored.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zoneId))

    fun isToday(zoneId: ZoneId, stored: OffsetDateTime, now: OffsetDateTime): Boolean =
        stored.atZoneSameInstant(zoneId).toLocalDate() == now.atZoneSameInstant(zoneId).toLocalDate()

    fun toEditFieldValue(zoneId: ZoneId, stored: OffsetDateTime): String =
        stored.atZoneSameInstant(zoneId).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
