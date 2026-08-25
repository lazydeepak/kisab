package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmState
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmCurrenciesTest {

    @Test
    fun nepaliRegionsDefaultToNpr() {
        assertEquals("NPR", FarmCurrencies.defaultFor(Locale.forLanguageTag("ne-NP")))
        assertEquals("NPR", FarmCurrencies.defaultFor(Locale.forLanguageTag("en-NP")))
    }

    @Test
    fun supportedRegionsDefaultToTheirCurrency() {
        assertEquals("JPY", FarmCurrencies.defaultFor(Locale.JAPAN))
        assertEquals("INR", FarmCurrencies.defaultFor(Locale.forLanguageTag("en-IN")))
        assertEquals("USD", FarmCurrencies.defaultFor(Locale.US))
    }

    @Test
    fun unsupportedOrUnknownRegionsFallBackToAppDefault() {
        assertEquals(FarmState.DEFAULT_CURRENCY_CODE, FarmCurrencies.defaultFor(Locale.forLanguageTag("en-AO")))
        assertEquals(FarmState.DEFAULT_CURRENCY_CODE, FarmCurrencies.defaultFor(Locale.ROOT))
        assertEquals(FarmState.DEFAULT_CURRENCY_CODE, FarmCurrencies.defaultFor(Locale.forLanguageTag("zz")))
        assertEquals(FarmState.DEFAULT_CURRENCY_CODE, FarmCurrencies.defaultFor(Locale("und")))
    }

    @Test
    fun isSupportedChecksTheCuratedListCaseInsensitively() {
        assertTrue(FarmCurrencies.isSupported("NPR"))
        assertTrue(FarmCurrencies.isSupported("usd"))
        assertTrue(FarmCurrencies.isSupported("jpy"))
        assertFalse(FarmCurrencies.isSupported("XYZ"))
        assertFalse(FarmCurrencies.isSupported(""))
    }

    @Test
    fun labelShowsLocalizedNameAndCode() {
        assertEquals("Nepalese Rupee (NPR)", FarmCurrencies.label("NPR", Locale.US))
        assertEquals("US Dollar (USD)", FarmCurrencies.label("USD", Locale.US))
        assertTrue(FarmCurrencies.label("NPR", Locale.forLanguageTag("ne-NP")).endsWith("(NPR)"))
    }

    @Test
    fun unknownCodeLabelFallsBackToCode() {
        assertEquals("XYZ (XYZ)", FarmCurrencies.label("XYZ", Locale.US))
    }
}
