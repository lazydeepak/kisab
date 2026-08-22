package com.susankhya.kisab

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
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

/**
 * Deterministically applies per-app locales and waits until the system has
 * acknowledged them. Setting applicationLocales is asynchronous: the system
 * broadcasts a configuration change and relaunches the app's activities.
 * Tests that launch an activity right after a locale change otherwise race
 * that relaunch wave (NoActivityResumedException / stale hierarchies).
 */
fun setApplicationLocalesAndWait(languageTags: List<String>) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = ApplicationProviderContext()
    val desired = android.os.LocaleList.forLanguageTags(languageTags.joinToString(","))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = desired
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (manager.applicationLocales.toLanguageTags() == desired.toLanguageTags()) break
            Thread.sleep(50)
        }
        check(manager.applicationLocales.toLanguageTags() == desired.toLanguageTags()) {
            "applicationLocales did not settle to ${desired.toLanguageTags()}"
        }
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(desired.toLanguageTags()))
    }
    instrumentation.waitForIdleSync()
}

/** Resets per-app locales to follow the system language, with the same settlement barrier. */
fun resetApplicationLocalesAndWait() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = ApplicationProviderContext()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = android.os.LocaleList.getEmptyLocaleList()
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (manager.applicationLocales.isEmpty) break
            Thread.sleep(50)
        }
        check(manager.applicationLocales.isEmpty) { "applicationLocales did not settle to empty" }
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
    instrumentation.waitForIdleSync()
}

private fun ApplicationProviderContext(): Context =
    androidx.test.core.app.ApplicationProvider.getApplicationContext()

