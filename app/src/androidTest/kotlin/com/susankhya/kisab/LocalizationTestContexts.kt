package com.susankhya.kisab

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Returns a context whose resources resolve against [locale] without mutating
 * any global/device configuration, so no restore is required.
 */
fun Context.withLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    return createConfigurationContext(configuration)
}
