package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmHisabAndReportsRedesignTest {

    private lateinit var context: Context
    private lateinit var service: FarmSliceService
    private var testFarmId: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        service = FarmSliceService(SharedPreferencesFarmStore(context))
        val farm = service.createFarm("Hisab Test Farm", currencyCode = "NPR")
        testFarmId = farm.id
        service.setCurrentFarmId(farm.id)
    }

    @After
    fun tearDown() {
        SharedPreferencesFarmStore(context).clear()
    }

    private fun waitForIdle() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun testHisabHub_calculatorsExecutionAndResultFormatting() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // 1. Navigate More -> Hisab
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreHisabButton)).perform(scrollTo(), click())
            waitForIdle()
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 2. Arithmetic: 25 + 15 = 40
            onView(withId(R.id.arithmeticFirstInput)).perform(scrollTo(), replaceText("25"), closeSoftKeyboard())
            onView(withId(R.id.arithmeticSecondInput)).perform(scrollTo(), replaceText("15"), closeSoftKeyboard())
            onView(withId(R.id.calculateArithmeticButton)).perform(scrollTo(), click())
            onView(withId(R.id.arithmeticResultText))
                .check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(containsString("40")))))

            // 3. Profit / Loss: Cost 500, Revenue 750 -> Profit 250
            onView(withId(R.id.profitCostInput)).perform(scrollTo(), replaceText("500"), closeSoftKeyboard())
            onView(withId(R.id.profitRevenueInput)).perform(scrollTo(), replaceText("750"), closeSoftKeyboard())
            onView(withId(R.id.calculateProfitButton)).perform(scrollTo(), click())
            onView(withId(R.id.profitResultText))
                .check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(containsString("250")))))

            // 4. Simple Interest: 10,000 at 10% for 12 months -> Interest 1,000, Total 11,000
            onView(withId(R.id.interestPrincipalInput)).perform(scrollTo(), replaceText("10000"), closeSoftKeyboard())
            onView(withId(R.id.interestRateInput)).perform(scrollTo(), replaceText("10"), closeSoftKeyboard())
            onView(withId(R.id.interestMonthsInput)).perform(scrollTo(), replaceText("12"), closeSoftKeyboard())
            onView(withId(R.id.calculateInterestButton)).perform(scrollTo(), click())
            onView(withId(R.id.interestResultText))
                .check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(containsString("1,000")))))

            // 5. Land Converter: 1 Ropani -> Aana = 16
            onView(withId(R.id.landValueInput)).perform(scrollTo(), replaceText("1"), closeSoftKeyboard())
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.Spinner>(R.id.landFromUnitSpinner).setSelection(1) // Ropani
                activity.findViewById<android.widget.Spinner>(R.id.landToUnitSpinner).setSelection(2)   // Aana
            }
            onView(withId(R.id.convertLandButton)).perform(scrollTo(), click())
            onView(withId(R.id.landResultText))
                .check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(containsString("16")))))

            // 6. Return back to More
            pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testFarmPlanningCalculators_execution() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreHisabButton)).perform(scrollTo(), click())
            waitForIdle()

            // 1. Seed Calculator
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.Spinner>(R.id.farmPlanningCalculatorSpinner).setSelection(0)
            }
            waitForIdle()
            onView(withId(R.id.seedAreaInput)).perform(scrollTo(), replaceText("100"), closeSoftKeyboard())
            onView(withId(R.id.seedRateInput)).perform(scrollTo(), replaceText("2"), closeSoftKeyboard())
            onView(withId(R.id.seedPriceInput)).perform(scrollTo(), replaceText("50"), closeSoftKeyboard())
            onView(withId(R.id.calculateSeedButton)).perform(scrollTo(), click())
            onView(withId(R.id.seedResultText))
                .check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(containsString("200")))))

            // 2. Feed Calculator
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.Spinner>(R.id.farmPlanningCalculatorSpinner).setSelection(2)
            }
            waitForIdle()
            onView(withId(R.id.feedAnimalCountInput)).perform(scrollTo(), replaceText("10"), closeSoftKeyboard())
            onView(withId(R.id.feedKgPerAnimalInput)).perform(scrollTo(), replaceText("5"), closeSoftKeyboard())
            onView(withId(R.id.feedDaysInput)).perform(scrollTo(), replaceText("30"), closeSoftKeyboard())
            onView(withId(R.id.feedPriceInput)).perform(scrollTo(), replaceText("40"), closeSoftKeyboard())
            onView(withId(R.id.calculateFeedButton)).perform(scrollTo(), click())
            onView(withId(R.id.feedResultText))
                .check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(containsString("1,500")))))
        } finally {
            scenario.close()
        }
    }
}
