package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.ui.AppearanceMode
import com.susankhya.kisab.ui.AppearancePreferences

class SharedPreferencesAppAppearancePreferences(context: Context) : AppearancePreferences {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun currencyDisplayOn(): Boolean = prefs.getBoolean(KEY_CURRENCY_DISPLAY, true)

    override fun saveCurrencyDisplay(on: Boolean) {
        prefs.edit().putBoolean(KEY_CURRENCY_DISPLAY, on).commit()
    }

    override fun numberGroupingOn(): Boolean = prefs.getBoolean(KEY_NUMBER_GROUPING, true)

    override fun saveNumberGrouping(on: Boolean) {
        prefs.edit().putBoolean(KEY_NUMBER_GROUPING, on).commit()
    }

    override fun appearanceMode(): AppearanceMode =
        AppearanceMode.fromValue(prefs.getString(KEY_APPEARANCE_MODE, null))

    override fun saveAppearanceMode(mode: AppearanceMode) {
        prefs.edit().putString(KEY_APPEARANCE_MODE, mode.storageValue).commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_app_appearance"
        private const val KEY_CURRENCY_DISPLAY = "currency_display"
        private const val KEY_NUMBER_GROUPING = "number_grouping"
        private const val KEY_APPEARANCE_MODE = "appearance_mode"
    }
}
