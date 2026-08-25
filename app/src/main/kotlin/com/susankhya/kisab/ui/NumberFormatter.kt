package com.susankhya.kisab.ui

import java.text.NumberFormat
import java.util.Locale

/**
 * Locale-aware integer rendering (digits, grouping) for counts and quantities.
 */
class NumberFormatter {
    fun format(locale: Locale, value: Int, grouping: Boolean = true): String =
        NumberFormat.getIntegerInstance(locale).apply {
            isGroupingUsed = grouping
        }.format(value.toLong())
}
