package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmState
import java.util.Currency
import java.util.Locale

/**
 * The governed, farmer-friendly list of currencies Kisab offers.
 *
 * The farm persists a canonical three-letter ISO code (never a symbol), so the
 * amount stored for a transaction never changes when the display currency
 * changes. These are the only currencies presented in the chooser, keeping it
 * short and recognizable instead of dumping the full ISO list.
 */
object FarmCurrencies {

    val SUPPORTED: List<String> = listOf(
        "NPR", "INR", "USD", "JPY",
        "EUR", "GBP", "AUD", "CAD",
        "CNY", "BDT", "LKR", "PKR",
        "SAR", "AED", "QAR", "KWD", "THB"
    )

    fun isSupported(code: String): Boolean = code.uppercase(Locale.US) in SUPPORTED

    /**
     * Suggests the currency for a farm based on the device region. When the
     * region maps to a supported currency that currency is returned, otherwise
     * the app default is used. The farmer can still change the suggestion.
     */
    fun defaultFor(locale: Locale): String {
        val inferred = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        return if (inferred != null && isSupported(inferred)) inferred else FarmState.DEFAULT_CURRENCY_CODE
    }

    /** Farmer-facing label such as "Nepalese Rupee (NPR)" for the given locale. */
    fun label(code: String, locale: Locale): String {
        val normalized = code.uppercase(Locale.US)
        val name = runCatching { Currency.getInstance(normalized).getDisplayName(locale) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && it != normalized }
            ?: normalized
        return "$name ($normalized)"
    }
}
