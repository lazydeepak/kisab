package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RC-01 M6.3 Kisan Calculator Toolbox on-device battery (Moto Edge 60 Fusion,
 * API 36). Exercises each calculator through its real UI with boundary and
 * error cases: money arithmetic (all five operations), divide-by-zero, blank
 * input, profit/loss + margin/markup, negative-input guards, simple interest,
 * and Hill and Terai land conversions. No farm is required.
 *
 * Inline input errors are set on the EditText (android.R.attr.error), which
 * Espresso's `withText` does not surface, so error paths assert the field's
 * `error` property directly from the Activity.
 */
@RunWith(AndroidJUnit4::class)
class KisanToolboxDeviceBatteryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun arithmeticAllOperationsAndDivideByZero() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())
            onView(withId(R.id.kisanCalculatorToolbox)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // ADD (default selection): 10 + 2.5 = 12.5
            enterArithmetic("10", "2.5", 0)
            assertResult(R.id.arithmeticResultText, "12.5")

            // SUBTRACT: 10 - 2.5 = 7.5
            enterArithmetic("10", "2.5", 1)
            assertResult(R.id.arithmeticResultText, "7.5")

            // MULTIPLY: 4 * 3 = 12
            enterArithmetic("4", "3", 2)
            assertResult(R.id.arithmeticResultText, "12")

            // DIVIDE: 9 / 4 = 2.25
            enterArithmetic("9", "4", 3)
            assertResult(R.id.arithmeticResultText, "2.25")

            // PERCENT_OF: 200 of 10% = 20
            enterArithmetic("200", "10", 4)
            assertResult(R.id.arithmeticResultText, "20")

            // DIVIDE BY ZERO: rejected with an inline error on the second field.
            onView(withId(R.id.arithmeticFirstInput)).perform(scrollTo(), replaceText("5"), closeSoftKeyboard())
            onView(withId(R.id.arithmeticSecondInput)).perform(scrollTo(), replaceText("0"), closeSoftKeyboard())
            scenarioOperationIndex(3)
            onView(withId(R.id.calculateArithmeticButton)).perform(scrollTo(), click())
            waitForIdle()
            scenario.onActivity { activity ->
                val error = activity.findViewById<android.widget.EditText>(R.id.arithmeticSecondInput).error
                assertTrue("divide-by-zero must set an inline error, got [$error]", error != null)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun arithmeticRejectsBlankInputWithInlineError() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())
            onView(withId(R.id.arithmeticFirstInput)).perform(scrollTo(), replaceText("5"), closeSoftKeyboard())
            onView(withId(R.id.arithmeticSecondInput)).perform(scrollTo(), replaceText(""), closeSoftKeyboard())
            onView(withId(R.id.calculateArithmeticButton)).perform(scrollTo(), click())
            waitForIdle()
            scenario.onActivity { activity ->
                val error = activity.findViewById<android.widget.EditText>(R.id.arithmeticSecondInput).error
                assertTrue("blank input must set an inline error, got [$error]", error != null)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun profitLossMarginAndMarkupAndNegativeGuard() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())

            // Profit: cost 40, revenue 100 -> profit 60, markup 150%
            onView(withId(R.id.profitCostInput)).perform(scrollTo(), replaceText("40"), closeSoftKeyboard())
            onView(withId(R.id.profitRevenueInput)).perform(scrollTo(), replaceText("100"), closeSoftKeyboard())
            onView(withId(R.id.calculateProfitButton)).perform(scrollTo(), click())
            onView(withId(R.id.profitResultText))
                .check(matches(allOf(
                    withEffectiveVisibility(Visibility.VISIBLE),
                    withText(containsString(context.getString(R.string.profit_label))),
                    withText(containsString("60")),
                    withText(containsString("150"))
                )))

            // Loss: cost 100, revenue 40 -> loss 60
            onView(withId(R.id.profitCostInput)).perform(scrollTo(), replaceText("100"), closeSoftKeyboard())
            onView(withId(R.id.profitRevenueInput)).perform(scrollTo(), replaceText("40"), closeSoftKeyboard())
            onView(withId(R.id.calculateProfitButton)).perform(scrollTo(), click())
            onView(withId(R.id.profitResultText))
                .check(matches(allOf(
                    withEffectiveVisibility(Visibility.VISIBLE),
                    withText(containsString(context.getString(R.string.loss_label))),
                    withText(containsString("60"))
                )))

            // Negative cost is rejected: the unsigned cost input must not
            // accept a valid negative value (inline error is set).
            onView(withId(R.id.profitCostInput)).perform(scrollTo(), replaceText("-5"), closeSoftKeyboard())
            onView(withId(R.id.profitRevenueInput)).perform(scrollTo(), replaceText("10"), closeSoftKeyboard())
            onView(withId(R.id.calculateProfitButton)).perform(scrollTo(), click())
            waitForIdle()
            scenario.onActivity { activity ->
                val error = activity.findViewById<android.widget.EditText>(R.id.profitCostInput).error
                assertTrue("negative cost must set an inline error, got [$error]", error != null)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun simpleInterestComputesInterestAndTotal() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())
            // Principal 10000, rate 12%/yr, 6 months -> interest 600, total 10600.
            onView(withId(R.id.interestPrincipalInput)).perform(scrollTo(), replaceText("10000"), closeSoftKeyboard())
            onView(withId(R.id.interestRateInput)).perform(scrollTo(), replaceText("12"), closeSoftKeyboard())
            onView(withId(R.id.interestMonthsInput)).perform(scrollTo(), replaceText("6"), closeSoftKeyboard())
            onView(withId(R.id.calculateInterestButton)).perform(scrollTo(), click())
            onView(withId(R.id.interestResultText))
                .check(matches(allOf(
                    withEffectiveVisibility(Visibility.VISIBLE),
                    withText(containsString("600")),
                    withText(containsString("10,600"))
                )))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun landConversionsHillAndTeraiAndNegativeAreaRejected() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navHisabItem)).perform(click())

            // Hill: 1 Ropani -> Aana = 16
            convertLand("1", 1, 2)
            assertResult(R.id.landResultText, "16")

            // Terai: 1 Bigha -> Kattha = 20
            convertLand("1", 5, 6)
            assertResult(R.id.landResultText, "20")

            // Negative area rejected with an inline error on the area field.
            onView(withId(R.id.landValueInput)).perform(scrollTo(), replaceText("-1"), closeSoftKeyboard())
            onView(withId(R.id.convertLandButton)).perform(scrollTo(), click())
            waitForIdle()
            scenario.onActivity { activity ->
                val error = activity.findViewById<android.widget.EditText>(R.id.landValueInput).error
                assertTrue("negative area must set an inline error, got [$error]", error != null)
            }
        } finally {
            scenario.close()
        }
    }

    private fun assertResult(viewId: Int, expected: String) {
        onView(withId(viewId))
            .check(matches(allOf(
                withEffectiveVisibility(Visibility.VISIBLE),
                withText(containsString(expected))
            )))
        waitForIdle()
    }

    private fun enterArithmetic(first: String, second: String, operationIndex: Int) {
        onView(withId(R.id.arithmeticFirstInput)).perform(scrollTo(), replaceText(first), closeSoftKeyboard())
        onView(withId(R.id.arithmeticSecondInput)).perform(scrollTo(), replaceText(second), closeSoftKeyboard())
        scenarioOperationIndex(operationIndex)
        onView(withId(R.id.calculateArithmeticButton)).perform(scrollTo(), click())
        waitForIdle()
    }

    private fun scenarioOperationIndex(index: Int) {
        onView(withId(R.id.arithmeticOperationSpinner)).perform(scrollTo(), click())
        androidx.test.espresso.Espresso.onData(org.hamcrest.Matchers.anything())
            .atPosition(index)
            .perform(click())
        waitForIdle()
    }

    private fun convertLand(value: String, fromIndex: Int, toIndex: Int) {
        onView(withId(R.id.landValueInput)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
        onView(withId(R.id.landFromUnitSpinner)).perform(scrollTo(), click())
        androidx.test.espresso.Espresso.onData(org.hamcrest.Matchers.anything()).atPosition(fromIndex).perform(click())
        onView(withId(R.id.landToUnitSpinner)).perform(scrollTo(), click())
        androidx.test.espresso.Espresso.onData(org.hamcrest.Matchers.anything()).atPosition(toIndex).perform(click())
        onView(withId(R.id.convertLandButton)).perform(scrollTo(), click())
        waitForIdle()
    }

    private fun waitForIdle() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}