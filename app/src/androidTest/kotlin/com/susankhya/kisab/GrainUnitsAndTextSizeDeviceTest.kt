package com.susankhya.kisab

import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.persistence.SharedPreferencesAppTextSizePreferences
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.os.SystemClock

/**
 * M9 device battery (Moto Edge 60 Fusion, API 36): proves the Nepali
 * traditional grain units (Mana/Pathi/Muri) are selectable through the real
 * product and supply creation dialogs and persist through the service, and
 * that the new 24sp NORMAL default renders in Settings while an explicitly
 * saved text size survives relaunch unchanged.
 */
@RunWith(AndroidJUnit4::class)
class GrainUnitsAndTextSizeDeviceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        SharedPreferencesAppTextSizePreferences(context).save(24)
    }

    @Test
    fun textSizeSettingsShowTwentyFourDefaultAndSavedSizeSurvivesRelaunch() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // Open Settings: default install must show 24 px.
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreSettingsButton)).perform(click())
            onView(withId(R.id.settingsScreen)).check(matches(isDisplayed()))
            onView(withId(R.id.settingsTextSizeValueText))
                .check(matches(withText(context.getString(R.string.text_size_value_format, 24))))
        } finally {
            scenario.close()
        }

        // Save an explicit smaller size (16 px) through the seekbar.
        val scenario2 = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreSettingsButton)).perform(click())
            onView(withId(R.id.settingsScreen)).check(matches(isDisplayed()))
            // progress = selected - MIN(14), so 16 -> 2. A real touch event
            // drives onProgressChanged(fromUser=true) so the save path runs.
            onView(withId(R.id.settingsTextSizeSeekBar)).perform(setProgressByTouch(2, 22))
            onView(withId(R.id.settingsTextSizeValueText))
                .check(matches(withText(context.getString(R.string.text_size_value_format, 16))))
        } finally {
            scenario2.close()
        }

        // Relaunch: the saved 16 must be retained, never reset to 24.
        val scenario3 = ActivityScenario.launch(FarmActivity::class.java)
        try {
            assertEquals(16, SharedPreferencesAppTextSizePreferences(context).load())
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreSettingsButton)).perform(click())
            onView(withId(R.id.settingsTextSizeValueText))
                .check(matches(withText(context.getString(R.string.text_size_value_format, 16))))
        } finally {
            scenario3.close()
        }

        // Restore the default for the remainder of the suite.
        SharedPreferencesAppTextSizePreferences(context).save(24)
    }

    @Test
    fun supplyCreationDialogOffersGrainUnitsAndPersistsSelection() {
        seedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // Farm Work -> Buy supplies routes to supply creation when empty.
            onView(withId(R.id.navFarmWorkItem)).perform(click())
            onView(withId(R.id.farmWorkBuySupplyButton)).perform(click())
            onView(withText(R.string.supply_add)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withHint(R.string.supply_name_hint)).inRoot(isDialog()).perform(replaceText("Paddy"), closeSoftKeyboard())
            // Pick Mana from the unit spinner (index 6 of KILOGRAM..MURI).
            selectSpinnerPosition(6)
            onView(withText(R.string.action_ok)).inRoot(isDialog()).perform(click())

            val service = FarmSliceService(SharedPreferencesFarmStore(context))
            val supplies = service.supplies(service.currentFarmId()!!)
            assertTrue("expected one supply", supplies.size == 1)
            assertEquals("expected MANA unit", ProductUnit.MANA, supplies[0].unit)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun productCreationDialogOffersGrainUnitsAndPersistsSelection() {
        seedFarm()
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // Farm Work -> Production routes to product creation when empty.
            onView(withId(R.id.navFarmWorkItem)).perform(click())
            onView(withId(R.id.farmWorkProductionButton)).perform(click())
            onView(withText(R.string.quick_sale_add_product)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withHint(R.string.quick_sale_product_name)).inRoot(isDialog()).perform(replaceText("Maize"))
            onView(withHint(R.string.quick_sale_product_name)).inRoot(isDialog()).perform(closeSoftKeyboard())
            // Pick Pathi (index 7 of LITRE..MURI in the product dialog).
            selectSpinnerPosition(7)
            onView(withText(R.string.action_ok)).inRoot(isDialog()).perform(click())

            val service = FarmSliceService(SharedPreferencesFarmStore(context))
            val products = service.products(service.currentFarmId()!!)
            assertTrue("expected one product", products.size == 1)
            assertEquals("expected PATHI unit", ProductUnit.PATHI, products[0].defaultUnit)
        } finally {
            scenario.close()
        }
    }

    private fun setProgressByTouch(targetProgress: Int, maxProgress: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = instanceOf(SeekBar::class.java)

        override fun getDescription(): String = "set seekbar progress via touch"

        override fun perform(uiController: UiController, view: View) {
            val seekBar = view as SeekBar
            seekBar.max = maxProgress
            val thumb = seekBar.thumb?.intrinsicWidth ?: 0
            val left = view.paddingLeft + thumb / 2
            val right = view.width - view.paddingRight - thumb / 2
            val trackWidth = right - left
            val fraction = targetProgress.toFloat() / maxProgress.toFloat()
            val x = left + (trackWidth * fraction).toInt()
            val y = view.height / 2
            val down = MotionEvent.obtain(
                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat(), 0
            )
            val up = MotionEvent.obtain(
                SystemClock.uptimeMillis() + 50, SystemClock.uptimeMillis() + 50,
                MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), 0
            )
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun selectSpinnerPosition(position: Int) {
        // Click the creation dialog's unit spinner (the only Spinner present),
        // then pick the item at the requested position from the dropdown.
        onView(allOf(
            instanceOf(android.widget.Spinner::class.java),
            isDisplayed()
        )).inRoot(isDialog()).perform(click())
        // The dropdown opens as a popup window (not the dialog root).
        onData(anything()).inRoot(isPlatformPopup()).atPosition(position).perform(click())
    }

    private fun seedFarm() {
        val service = FarmSliceService(SharedPreferencesFarmStore(context))
        val farm = service.createFarm("Grain Test Farm", currencyCode = "NPR")
        service.setCurrentFarmId(farm.id)
    }
}