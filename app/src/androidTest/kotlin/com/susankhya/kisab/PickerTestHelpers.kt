package com.susankhya.kisab

import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher

/**
 * Drives the real platform date/time picker dialogs on-device so the M4-03
 * picker workflow is verified through actual user interactions rather than
 * test-only state setters.
 *
 * [pickerMonth] is zero-based, matching [android.widget.DatePicker] and the
 * [android.app.DatePickerDialog] callback contract.
 */
object PickerTestHelpers {

    fun pickDateTime(year: Int, pickerMonth: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int) {
        Espresso.closeSoftKeyboard()
        Espresso.onView(ViewMatchers.withId(R.id.changeDateTimeButton))
            .perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withClassName(equalTo(DatePicker::class.java.name)))
            .perform(setDate(year, pickerMonth, dayOfMonth))
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withClassName(equalTo(TimePicker::class.java.name)))
            .perform(setTime(hourOfDay, minute))
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /**
     * Selects a date in the already-visible platform DatePicker without
     * confirming the picker (used to cancel midway through the edit flow).
     * The caller must have already opened the date-time flow.
     */
    fun setDateOnly(year: Int, pickerMonth: Int, dayOfMonth: Int) {
        Espresso.onView(ViewMatchers.withClassName(equalTo(DatePicker::class.java.name)))
            .perform(setDate(year, pickerMonth, dayOfMonth))
    }

    private fun setDate(year: Int, month: Int, day: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = ViewMatchers.isDisplayed()

        override fun getDescription(): String = "set date picker values"

        override fun perform(uiController: UiController, view: View) {
            (view as DatePicker).updateDate(year, month, day)
        }
    }

    private fun setTime(hour: Int, minute: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = ViewMatchers.isDisplayed()

        override fun getDescription(): String = "set time picker values"

        override fun perform(uiController: UiController, view: View) {
            val picker = view as TimePicker
            picker.setHour(hour)
            picker.setMinute(minute)
        }
    }
}
