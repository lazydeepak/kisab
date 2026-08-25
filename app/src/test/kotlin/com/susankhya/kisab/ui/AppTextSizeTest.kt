package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTextSizeTest {

    @Test
    fun defaultSpIsTwentyFourAsNormalBaseline() {
        assertEquals(24, AppTextSize.DEFAULT_SP)
    }

    @Test
    fun scaleBaseIsSixteenSoDefaultScalesToOnePointFive() {
        assertEquals(16, AppTextSize.BASE_SP)
        assertEquals(1.5f, AppTextSize.DEFAULT_SP.toFloat() / AppTextSize.BASE_SP, 0.0f)
    }

    @Test
    fun defaultSitsWithinSupportedRange() {
        assertTrue(AppTextSize.DEFAULT_SP in AppTextSize.MIN_SP..AppTextSize.MAX_SP)
    }

    @Test
    fun coerceKeepsSupportedValues() {
        assertEquals(14, AppTextSize.coerce(14))
        assertEquals(16, AppTextSize.coerce(16))
        assertEquals(24, AppTextSize.coerce(24))
        assertEquals(36, AppTextSize.coerce(36))
    }

    @Test
    fun coerceBoundsUnsupportedValues() {
        assertEquals(14, AppTextSize.coerce(10))
        assertEquals(36, AppTextSize.coerce(40))
    }

    @Test
    fun fullRangeIsContinuousFromDefaultToLarge() {
        for (sp in AppTextSize.MIN_SP..AppTextSize.MAX_SP) {
            assertEquals(sp, AppTextSize.coerce(sp))
        }
    }
}
