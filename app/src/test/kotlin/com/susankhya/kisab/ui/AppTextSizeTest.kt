package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTextSizeTest {

    @Test
    fun coerceKeepsSupportedValues() {
        assertEquals(14, AppTextSize.coerce(14))
        assertEquals(16, AppTextSize.coerce(16))
        assertEquals(24, AppTextSize.coerce(24))
    }

    @Test
    fun coerceBoundsUnsupportedValues() {
        assertEquals(14, AppTextSize.coerce(10))
        assertEquals(24, AppTextSize.coerce(30))
    }
}
