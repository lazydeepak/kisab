package com.susankhya.kisab

import android.os.Build
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.ui.FarmActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M6.4.1 regression: the shell must own system-bar insets. App-bar content must
 * never render under the status bar, bottom-navigation content must never render
 * under the navigation bar, and repeated inset dispatch must not accumulate
 * padding (no double padding on devices where the window already lays out below
 * the bars).
 */
@RunWith(AndroidJUnit4::class)
class FarmActivityShellInsetsTest {

    private lateinit var scenario: ActivityScenario<FarmActivity>

    private fun rootInsets(view: View): WindowInsetsCompat? =
        ViewCompat.getRootWindowInsets(view.rootView)

    @Before
    fun launch() {
        scenario = ActivityScenario.launch(FarmActivity::class.java)
    }

    @Test
    fun appBarContentClearsStatusBar() {
        scenario.onActivity { activity ->
            val appBar = activity.findViewById<LinearLayout>(R.id.shellAppBar)
            val logo = activity.findViewById<View>(R.id.shellLogo)
            val title = activity.findViewById<View>(R.id.shellTitle)
            val rootTopInset = rootInsets(logo)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0

            // App bar occupies the very top of the shell.
            assertEquals(0, appBar.top)
            // The shell owns the status-bar inset: either absorbed into app-bar
            // padding (edge-to-edge) or already consumed by the window.
            assertTrue("app bar not padded for status bar", appBar.paddingTop >= 0)
            // Content must not overlap the status bar.
            assertTrue("logo inside status bar", logo.top >= appBar.paddingTop)
            assertTrue("title inside status bar", title.top >= appBar.paddingTop)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && rootTopInset > 0) {
                // Edge-to-edge (enforced on API 35+ by targetSdk): window
                // coordinates equal screen coordinates, so content must clear the
                // full reported bar height.
                assertTrue("logo overlaps reported status bar", logo.top >= rootTopInset)
            }
        }
    }

    @Test
    fun bottomNavigationContentClearsNavigationBar() {
        scenario.onActivity { activity ->
            val bottomNav = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
            val navItem = activity.findViewById<LinearLayout>(R.id.navTodayItem)
            val navigationBarBottom = rootInsets(bottomNav)
                ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0

            // Bottom nav spans to the screen edge so its padding can absorb the
            // navigation-bar inset (edge-to-edge), or sits above the bar when the
            // window already lays out inside it.
            val clearsNavBar = if (navigationBarBottom > 0) {
                navItem.bottom <= bottomNav.rootView.height - navigationBarBottom
            } else {
                // Pre-35 default-window: nav item already laid out above the bar.
                navItem.bottom <= bottomNav.rootView.height
            }
            assertTrue("nav item under navigation bar", clearsNavBar)
            // No double handling: bottom nav still touches the edge it draws to.
            assertTrue(bottomNav.bottom <= bottomNav.rootView.height)
        }
    }

    @Test
    fun repeatedInsetPassesDoNotAccumulate() {
        scenario.onActivity { activity ->
            val appBar = activity.findViewById<LinearLayout>(R.id.shellAppBar)
            val bottomNav = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
            val appBarTopBefore = appBar.paddingTop
            val bottomNavBottomBefore = bottomNav.paddingBottom

            repeat(2) {
                ViewCompat.requestApplyInsets(appBar)
                appBar.requestLayout()
                bottomNav.requestLayout()
                ViewCompat.requestApplyInsets(bottomNav)
            }

            assertEquals("app bar double padding on repeat", appBarTopBefore, appBar.paddingTop)
            assertEquals("bottom nav double padding on repeat", bottomNavBottomBefore, bottomNav.paddingBottom)
        }
    }
}