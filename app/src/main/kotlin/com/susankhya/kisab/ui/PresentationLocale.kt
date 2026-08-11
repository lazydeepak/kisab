package com.susankhya.kisab.ui

import java.util.Locale

/**
 * Normalizes the app's current locale into a stable presentation locale.
 *
 * Nepali UI renders as ne-NP and English UI as en-NP; any other language keeps
 * its own locale so that number and date formatting follow that language's
 * conventions. Time presentation always uses the device timezone regardless
 * of the presentation locale.
 */
object PresentationLocale {
    val ENGLISH: Locale = Locale.forLanguageTag("en-NP")
    val NEPALI: Locale = Locale.forLanguageTag("ne-NP")

    fun presentationLocale(appLocale: Locale): Locale = when (appLocale.language) {
        "ne" -> NEPALI
        "en" -> ENGLISH
        else -> appLocale
    }
}
