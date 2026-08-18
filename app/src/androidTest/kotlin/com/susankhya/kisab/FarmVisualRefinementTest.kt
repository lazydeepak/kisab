package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmVisualRefinementTest {

    private lateinit var context: Context
    private lateinit var service: FarmSliceService
    private var testFarmId: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        service = FarmSliceService(SharedPreferencesFarmStore(context))
        val farm = service.createFarm("Visual Polish Dairy Farm", currencyCode = "NPR")
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
    fun testVisualHierarchy_acrossAllFiveDestinations() {
        val farmId = testFarmId!!
        service.addProduct(farmId, "Buffalo Milk", ProductUnit.LITRE)
        service.addParty(farmId, PartyDraft("Hari Bahadur", PartyRole.CUSTOMER, "9841000002"))

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // 1. Shell and App Bar
            onView(withId(R.id.shellAppBar)).check(matches(isDisplayed()))
            onView(withId(R.id.shellTitle)).check(matches(withText("Visual Polish Dairy Farm")))
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))

            // 2. Today Dashboard
            onView(withId(R.id.todayHeaderBar)).check(matches(isDisplayed()))
            onView(withId(R.id.todayDateText)).check(matches(isDisplayed()))

            // 3. Khata Tab
            onView(withId(R.id.navKhataItem)).perform(click())
            waitForIdle()
            onView(withId(R.id.khataOverviewContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.khataSearchInput)).check(matches(isDisplayed()))
            onView(withId(R.id.khataFilterRadioGroup)).check(matches(isDisplayed()))

            // 4. Farm Work Tab
            onView(withId(R.id.navFarmWorkItem)).perform(click())
            waitForIdle()
            onView(withId(R.id.farmWorkScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.farmWorkProductionSectionLabel)).check(matches(isDisplayed()))

            // 5. More Tab
            onView(withId(R.id.navMoreItem)).perform(click())
            waitForIdle()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.moreHisabButton)).check(matches(isDisplayed()))
            onView(withId(R.id.moreFarmsButton)).check(matches(isDisplayed()))
            onView(withId(R.id.moreSettingsButton)).check(matches(isDisplayed()))

            // 6. Return to Today
            onView(withId(R.id.navTodayItem)).perform(click())
            waitForIdle()
            onView(withId(R.id.todayHeroTitle)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}
