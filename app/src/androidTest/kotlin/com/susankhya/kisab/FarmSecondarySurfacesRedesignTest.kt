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
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
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
class FarmSecondarySurfacesRedesignTest {

    private lateinit var context: Context
    private lateinit var service: FarmSliceService
    private var farm1Id: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        service = FarmSliceService(SharedPreferencesFarmStore(context))
        val farm1 = service.createFarm("Primary Dairy Farm", currencyCode = "NPR")
        farm1Id = farm1.id
        service.setCurrentFarmId(farm1.id)
    }

    @After
    fun tearDown() {
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun testMoreHubNavigation_toAllSecondaryDestinationsAndBack() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // 1. Open More
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 2. More -> Hisab -> Back
            onView(withId(R.id.moreHisabButton)).perform(scrollTo(), click())
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 3. More -> Farms -> Back
            onView(withId(R.id.moreFarmsButton)).perform(scrollTo(), click())
            onView(withId(R.id.farmsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 4. More -> Settings -> Back
            onView(withId(R.id.moreSettingsButton)).perform(scrollTo(), click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 5. More -> About dialog
            onView(withId(R.id.moreAboutButton)).perform(scrollTo(), click())
            onView(withText(R.string.dialog_about_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testMultiFarmManagement_createSwitchAndDataIsolation() {
        val farm1 = farm1Id!!
        // Add a customer to Farm 1
        service.addParty(farm1, PartyDraft("Farm 1 Exclusive Buyer", PartyRole.CUSTOMER, "9841000000"))

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // 1. Open Farms
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreFarmsButton)).perform(scrollTo(), click())
            onView(withId(R.id.farmsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 2. Add New Farm: "Secondary Poultry Farm"
            onView(withId(R.id.addFarmButton)).perform(scrollTo(), click())
            onView(withId(R.id.addFarmScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.addFarmNameInput)).perform(replaceText("Secondary Poultry Farm"), closeSoftKeyboard())
            onView(withId(R.id.addFarmCreateButton)).perform(scrollTo(), click())

            // 3. After creation, app navigates to Today with new farm active
            onView(withId(R.id.shellTitle)).check(matches(withText("Secondary Poultry Farm")))

            // 4. Verify data isolation: Khata of Farm 2 has NO parties from Farm 1
            onView(withId(R.id.navKhataItem)).perform(click())
            onView(withId(R.id.partiesEmptyText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 5. Open Farms again, switch back to Farm 1
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreFarmsButton)).perform(scrollTo(), click())
            onView(allOf(isDescendantOfA(withId(R.id.farmsListContainer)), withText(containsString("Primary Dairy Farm"))))
                .perform(scrollTo(), click())

            onView(withId(R.id.farmDetailsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.farmDetailsSwitchButton)).perform(scrollTo(), click())

            // 6. Navigate to Khata and verify Farm 1's party is present
            onView(withId(R.id.navKhataItem)).perform(click())
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Farm 1 Exclusive Buyer"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testFarmDetailsRename_updatesTitleProperly() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreFarmsButton)).perform(scrollTo(), click())
            onView(allOf(isDescendantOfA(withId(R.id.farmsListContainer)), withText(containsString("Primary Dairy Farm"))))
                .perform(scrollTo(), click())

            onView(withId(R.id.farmDetailsRenameButton)).perform(scrollTo(), click())
            onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.EditText::class.java))
                .inRoot(isDialog())
                .perform(replaceText("Renamed Dairy Farm"), closeSoftKeyboard())
            onView(withText(R.string.settings_rename_farm_action)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.farmDetailsNameText)).check(matches(withText("Renamed Dairy Farm")))
            pressBack()
            // In Farms list, verify title is Farms
            onView(withId(R.id.shellTitle)).check(matches(withText(R.string.farms_page_title)))
            // Navigate to Today and verify renamed farm title
            onView(withId(R.id.navTodayItem)).perform(click())
            onView(withId(R.id.shellTitle)).check(matches(withText("Renamed Dairy Farm")))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun testSettingsGroups_andAppearanceToggles() {
        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreSettingsButton)).perform(scrollTo(), click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Verify Language section
            onView(withId(R.id.languageNepaliRadio)).perform(scrollTo())
            onView(withId(R.id.languageEnglishRadio)).perform(scrollTo())

            // Verify Appearance section
            onView(withId(R.id.appearanceModeLightRadio)).perform(scrollTo(), click())
            onView(withId(R.id.appearanceModeDarkRadio)).perform(scrollTo(), click())

            // Verify Number formatting
            onView(withId(R.id.numberGroupingOnRadio)).perform(scrollTo(), click())
            onView(withId(R.id.currencyDisplayOnRadio)).perform(scrollTo(), click())

            pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } finally {
            scenario.close()
        }
    }
}
