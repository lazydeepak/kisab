package com.susankhya.kisab.ui

/**
 * App-local base text size for Kisab. The UI presents this as a simple number
 * while the Activity applies it as a proportional scale over existing text.
 */
object AppTextSize {
    const val MIN_SP = 14
    const val DEFAULT_SP = 16
    const val MAX_SP = 24

    fun coerce(value: Int): Int = value.coerceIn(MIN_SP, MAX_SP)
}

interface AppTextSizePreferences {
    fun load(): Int
    fun save(textSizeSp: Int)
}
