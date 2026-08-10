package com.susankhya.kisab.ui

/**
 * The app-level language selection exposed in Settings.
 *
 * [FOLLOW_DEVICE] keeps whatever the system provides; [ENGLISH] and [NEPALI]
 * pin the app to that language. The language tag drives
 * `AppCompatDelegate.setApplicationLocales` (null resets to the system
 * selection). Presentation stays governed by [PresentationLocale].
 */
enum class AppLanguage(val storageValue: String, val languageTag: String?) {
    FOLLOW_DEVICE("system", null),
    ENGLISH("en", "en"),
    NEPALI("ne", "ne");

    companion object {
        fun fromValue(value: String?): AppLanguage =
            values().firstOrNull { it.storageValue == value } ?: FOLLOW_DEVICE
    }
}

interface AppLanguagePreferences {
    fun load(): AppLanguage
    fun save(language: AppLanguage)
}