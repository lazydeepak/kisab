package com.susankhya.kisab.ui

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/** Locale-aware decimal parsing/rendering for non-persisted calculator values. */
class DecimalValueFormatter {
    fun parse(locale: Locale, raw: String): BigDecimal? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val format = NumberFormat.getNumberInstance(locale) as DecimalFormat
        format.isParseBigDecimal = true
        val position = ParsePosition(0)
        val parsed = format.parse(text, position) as? BigDecimal
        return parsed?.takeIf { position.index == text.length }
    }

    /** Parses localized digits while rejecting fractions and values outside Int range. */
    fun parseNonNegativeWhole(locale: Locale, raw: String): Int? {
        val value = parse(locale, raw) ?: return null
        if (value.signum() < 0 || value.stripTrailingZeros().scale() > 0) return null
        return runCatching { value.intValueExact() }.getOrNull()
    }

    fun format(locale: Locale, value: BigDecimal, maximumFractionDigits: Int = 6): String =
        NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = true
            minimumFractionDigits = 0
            this.maximumFractionDigits = maximumFractionDigits
        }.format(value)
}
