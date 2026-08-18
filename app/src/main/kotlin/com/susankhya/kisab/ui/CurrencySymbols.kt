package com.susankhya.kisab.ui

import java.util.Locale

/**
 * Deterministic farmer-facing symbols for [FarmCurrencies.SUPPORTED].
 *
 * The farm always stores a canonical three-letter ISO code; this mapping only
 * changes how money is *displayed*. Symbols are explicit per currency instead
 * of being pulled from device locale data so the app never surprises the user
 * with an unexpected or locale-dependent symbol. Where a shared symbol would
 * be ambiguous between supported currencies ($, ¥, Rs) a compact distinct
 * form is used. Codes without a good symbol fall back to their ISO code so
 * currency identity is never lost.
 */
object CurrencySymbols {

    private val SYMBOLS: Map<String, String> = mapOf(
        "NPR" to "रु",
        "INR" to "₹",
        "USD" to "$",
        "JPY" to "¥",
        "EUR" to "€",
        "GBP" to "£",
        "AUD" to "A$",
        "CAD" to "C$",
        "CNY" to "CN¥",
        "BDT" to "৳",
        "LKR" to "Rs",
        "PKR" to "Rs",
        "SAR" to "SR",
        "AED" to "AED",
        "QAR" to "QR",
        "KWD" to "KD",
        "THB" to "฿"
    )

    /** Farmer-facing symbol for [code], falling back to the ISO code itself. */
    fun symbolFor(code: String): String =
        SYMBOLS[code.uppercase(Locale.US)] ?: code.uppercase(Locale.US)
}
