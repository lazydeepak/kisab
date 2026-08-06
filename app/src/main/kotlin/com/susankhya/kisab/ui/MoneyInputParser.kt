package com.susankhya.kisab.ui

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Outcome of parsing a localized major-unit money input into exact minor units.
 */
sealed interface MoneyInputResult {
    data object Missing : MoneyInputResult
    data object NotPositive : MoneyInputResult
    data object Invalid : MoneyInputResult
    data object TooPrecise : MoneyInputResult
    data object TooLarge : MoneyInputResult
    data class Valid(val amountMinor: Long) : MoneyInputResult
}

/**
 * Parses user-typed major-unit amounts into exact minor units without any
 * rounding. Accepts the locale's decimal separator and digits (localized or
 * ASCII) and valid grouping separators; rejects blank, zero, negative,
 * exponential, malformed, over-precise, and overflowing input.
 */
class MoneyInputParser(private val moneyFormatter: MoneyFormatter = MoneyFormatter()) {

    fun parse(locale: Locale, currencyCode: String, raw: String): MoneyInputResult {
        val input = raw.trim()
        if (input.isEmpty()) return MoneyInputResult.Missing
        if (input.contains('+') || input.contains('e') || input.contains('E')) return MoneyInputResult.Invalid

        var negative = false
        var body = input
        if (body.startsWith('-')) {
            negative = true
            body = body.drop(1)
        }
        if (body.isEmpty() || body.contains('-')) return MoneyInputResult.Invalid

        val symbols = DecimalFormatSymbols.getInstance(locale)
        val decimalSeparator = symbols.decimalSeparator
        val groupingSeparator = symbols.groupingSeparator
        val zeroDigit = symbols.zeroDigit

        val normalized = StringBuilder(body.length)
        for (ch in body) {
            val digit = ch - zeroDigit
            if (digit in 0..9) normalized.append(('0'.code + digit).toChar()) else normalized.append(ch)
        }
        val value = normalized.toString()

        val decimalIndex = value.indexOf(decimalSeparator)
        if (decimalIndex >= 0 && value.indexOf(decimalSeparator, decimalIndex + 1) >= 0) {
            return MoneyInputResult.Invalid
        }
        val integerPart = if (decimalIndex >= 0) value.substring(0, decimalIndex) else value
        val fractionPart = if (decimalIndex >= 0) value.substring(decimalIndex + 1) else ""

        if (integerPart.isEmpty()) return MoneyInputResult.Invalid
        if (decimalIndex >= 0 && fractionPart.isEmpty()) return MoneyInputResult.Invalid
        if (!integerPart.all { it.isDigit() || it == groupingSeparator }) return MoneyInputResult.Invalid
        if (!fractionPart.all { it.isDigit() }) return MoneyInputResult.Invalid
        if (!isValidGrouping(integerPart.split(groupingSeparator).map { it.length })) return MoneyInputResult.Invalid

        val maxFractionDigits = moneyFormatter.fractionDigits(currencyCode)
        if (fractionPart.length > maxFractionDigits) return MoneyInputResult.TooPrecise

        val integerDigits = integerPart.filter { it != groupingSeparator }
        val scaled = integerDigits + fractionPart.padEnd(maxFractionDigits, '0')
        val scaledValue = scaled.toBigDecimalOrNull() ?: return MoneyInputResult.Invalid
        if (scaledValue > BigDecimal(Long.MAX_VALUE)) return MoneyInputResult.TooLarge
        val amountMinor = scaledValue.toLong()
        if (negative || amountMinor == 0L) return MoneyInputResult.NotPositive
        return MoneyInputResult.Valid(amountMinor)
    }

    private fun isValidGrouping(groupSizes: List<Int>): Boolean {
        if (groupSizes.size <= 1) return true
        if (groupSizes.any { it <= 0 }) return false
        val last = groupSizes.last()
        if (last != 3) return false
        val leftmost = groupSizes.first()
        val middle = groupSizes.drop(1).dropLast(1)
        val standard = middle.all { it == 3 } && leftmost in 1..3
        val indian = middle.all { it == 2 } && leftmost in 1..2
        return standard || indian
    }
}
