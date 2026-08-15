package com.susankhya.kisab.ui

import androidx.appcompat.app.AppCompatDelegate

/**
 * App-local display preferences shown in Settings → Appearance. These are
 * presentation-only and never stored in farm data.
 */
enum class AppearanceMode(val storageValue: String) {
    FOLLOW_SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    val nightMode: Int
        get() = when (this) {
            FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }

    companion object {
        fun fromValue(value: String?): AppearanceMode =
            values().firstOrNull { it.storageValue == value } ?: FOLLOW_SYSTEM
    }
}

interface AppearancePreferences {
    fun currencyDisplayOn(): Boolean
    fun saveCurrencyDisplay(on: Boolean)
    fun numberGroupingOn(): Boolean
    fun saveNumberGrouping(on: Boolean)
    fun appearanceMode(): AppearanceMode
    fun saveAppearanceMode(mode: AppearanceMode)
}
