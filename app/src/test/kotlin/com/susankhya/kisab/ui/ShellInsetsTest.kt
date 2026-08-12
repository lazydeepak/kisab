package com.susankhya.kisab.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellInsetsTest {

    @Test
    fun appBarTopPadding_preservesBaseWhenNoInset() {
        assertEquals(0, ShellInsets.appBarTopPadding(baseTopPadding = 0, statusBarTopInset = 0))
        assertEquals(24, ShellInsets.appBarTopPadding(baseTopPadding = 24, statusBarTopInset = 0))
    }

    @Test
    fun appBarTopPadding_addsInsetWhenProvided() {
        assertEquals(128, ShellInsets.appBarTopPadding(baseTopPadding = 0, statusBarTopInset = 128))
        assertEquals(152, ShellInsets.appBarTopPadding(baseTopPadding = 24, statusBarTopInset = 128))
    }

    @Test
    fun bottomNavigationBottomPadding_preservesBaseWhenNoInset() {
        assertEquals(6, ShellInsets.bottomNavigationBottomPadding(baseBottomPadding = 6, navigationBarBottomInset = 0))
        assertEquals(0, ShellInsets.bottomNavigationBottomPadding(baseBottomPadding = 0, navigationBarBottomInset = 0))
    }

    @Test
    fun bottomNavigationBottomPadding_addsInsetWhenProvided() {
        assertEquals(86, ShellInsets.bottomNavigationBottomPadding(baseBottomPadding = 6, navigationBarBottomInset = 80))
        assertEquals(136, ShellInsets.bottomNavigationBottomPadding(baseBottomPadding = 6, navigationBarBottomInset = 130))
    }

    @Test
    fun paddingIsIdempotentAcrossRepeatedInsetPasses() {
        var top = 0
        var bottom = 6
        repeat(3) {
            top = ShellInsets.appBarTopPadding(0, 128)
            bottom = ShellInsets.bottomNavigationBottomPadding(6, 80)
        }
        assertEquals(128, top)
        assertEquals(86, bottom)
    }
}