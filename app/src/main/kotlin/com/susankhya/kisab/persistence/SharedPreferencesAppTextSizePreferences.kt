package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.ui.AppTextSize
import com.susankhya.kisab.ui.AppTextSizePreferences

class SharedPreferencesAppTextSizePreferences(context: Context) : AppTextSizePreferences {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): Int = AppTextSize.coerce(
        prefs.getInt(KEY_TEXT_SIZE_SP, AppTextSize.DEFAULT_SP)
    )

    override fun save(textSizeSp: Int) {
        prefs.edit().putInt(KEY_TEXT_SIZE_SP, AppTextSize.coerce(textSizeSp)).commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_app_appearance"
        private const val KEY_TEXT_SIZE_SP = "text_size_sp"
    }
}
