package com.susankhya.kisab.ui

/**
 * App-local base text size for Kisab. The UI presents this as a simple number
 * while the Activity applies it as a proportional scale over existing text.
 *
 * [DEFAULT_SP] is the NORMAL default readability baseline for fresh/default
 * installs (24sp). [BASE_SP] is the authored scale divisor (16sp) the layouts
 * were designed against: scale = load()/BASE_SP, so a default install renders
 * text at 24/16 = 1.5x the authored sizes. Users who explicitly saved a size
 * keep their choice because load() returns the stored value and the divisor is
 * unchanged, so previously saved sizes render exactly as before the upgrade.
 */
object AppTextSize {
    const val MIN_SP = 14
    const val DEFAULT_SP = 24
    const val BASE_SP = 16
    const val MAX_SP = 36

    fun coerce(value: Int): Int = value.coerceIn(MIN_SP, MAX_SP)
}

interface AppTextSizePreferences {
    fun load(): Int
    fun save(textSizeSp: Int)
}
