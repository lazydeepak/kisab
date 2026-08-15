package com.susankhya.kisab.ui

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Renders stored minor-unit amounts as locale-aware major-unit money.
 *
 * Values are always derived from [amountMinor] through [BigDecimal] arithmetic
 * (never Double/Float). By default the currency code is shown explicitly next
 * to the locale-formatted number so the currency is never ambiguous, but the
 * app can hide it via [showCurrency] (presentation only). [grouping] controls
 * whether thousands separators are shown. Fraction digits follow the
 * currency's ISO definition; unknown-but-valid three-letter codes fall back
 * to two fraction digits and still display their ISO code.
 */
class MoneyFormatter {

    fun fractionDigits(currencyCode: String): Int {
        val digits = runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }.getOrDefault(2)
        return if (digits < 0) 2 else digits
    }

    fun format(
        locale: Locale,
        currencyCode: String,
        amountMinor: Long,
        showCurrency: Boolean = true,
        grouping: Boolean = true
    ): String {
        val number = numberFormat(locale, currencyCode, grouping)
            .format(BigDecimal(amountMinor).movePointLeft(fractionDigits(currencyCode)))
        return if (showCurrency) "$number $currencyCode" else number
    }

    /**
     * Lossless major-unit value for an edit field: no grouping separators so it
     * parses cleanly, and exactly the currency's fraction digits.
     */
    fun toEditFieldValue(locale: Locale, currencyCode: String, amountMinor: Long): String =
        numberFormat(locale, currencyCode, grouping = false)
            .format(BigDecimal(amountMinor).movePointLeft(fractionDigits(currencyCode)))

    private fun numberFormat(locale: Locale, currencyCode: String, grouping: Boolean): NumberFormat {
        val digits = fractionDigits(currencyCode)
        return NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = grouping
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
    }
}
