package com.susankhya.kisab.ui

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Test

class AppAppearanceTest {

    @Test
    fun fromValueMapsStoredValues() {
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, AppearanceMode.fromValue("system"))
        assertEquals(AppearanceMode.LIGHT, AppearanceMode.fromValue("light"))
        assertEquals(AppearanceMode.DARK, AppearanceMode.fromValue("dark"))
    }

    @Test
    fun fromValueFallsBackToFollowSystem() {
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, AppearanceMode.fromValue(null))
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, AppearanceMode.fromValue("unknown"))
    }

    @Test
    fun nightModeMapsToAppCompatModes() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppearanceMode.FOLLOW_SYSTEM.nightMode)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppearanceMode.LIGHT.nightMode)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppearanceMode.DARK.nightMode)
    }
}
