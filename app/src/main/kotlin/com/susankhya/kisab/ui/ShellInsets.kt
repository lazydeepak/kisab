package com.susankhya.kisab.ui

object ShellInsets {

    /** App bar top padding when the delivered status-bar inset is applied. */
    fun appBarTopPadding(baseTopPadding: Int, statusBarTopInset: Int): Int =
        if (statusBarTopInset > 0) baseTopPadding + statusBarTopInset else baseTopPadding

    /** Bottom navigation bottom padding when the delivered navigation-bar inset is applied. */
    fun bottomNavigationBottomPadding(
        baseBottomPadding: Int,
        navigationBarBottomInset: Int,
    ): Int = if (navigationBarBottomInset > 0) baseBottomPadding + navigationBarBottomInset else baseBottomPadding
}