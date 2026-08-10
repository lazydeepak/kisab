package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun fromValueResolvesKnownSelections() {
        assertEquals(AppLanguage.FOLLOW_DEVICE, AppLanguage.fromValue("system"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromValue("en"))
        assertEquals(AppLanguage.NEPALI, AppLanguage.fromValue("ne"))
    }

    @Test
    fun fromValueFallsBackToFollowDeviceForUnknown() {
        assertEquals(AppLanguage.FOLLOW_DEVICE, AppLanguage.fromValue(null))
        assertEquals(AppLanguage.FOLLOW_DEVICE, AppLanguage.fromValue("fr"))
        assertEquals(AppLanguage.FOLLOW_DEVICE, AppLanguage.fromValue(""))
    }

    @Test
    fun languageTagsMapToPerAppLocales() {
        assertEquals(null, AppLanguage.FOLLOW_DEVICE.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
        assertEquals("ne", AppLanguage.NEPALI.languageTag)
    }

    @Test
    fun followDeviceStorageValueIsSystemDefault() {
        assertEquals("system", AppLanguage.FOLLOW_DEVICE.storageValue)
        assertEquals("en", AppLanguage.ENGLISH.storageValue)
        assertEquals("ne", AppLanguage.NEPALI.storageValue)
    }
}