package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.ui.AppLanguage
import com.susankhya.kisab.ui.AppLanguagePreferences

class SharedPreferencesAppLanguagePreferences(context: Context) : AppLanguagePreferences {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): AppLanguage =
        AppLanguage.fromValue(prefs.getString(KEY_LANGUAGE, null))

    override fun save(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.storageValue).commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_app_language"
        private const val KEY_LANGUAGE = "language"
    }
}