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
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
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
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import com.susankhya.kisab.ui.FarmActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class FarmIntegratedPolishTest {

    private lateinit var context: Context
    private lateinit var service: FarmSliceService
    private var testFarmId: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedPreferencesFarmStore(context).clear()
        service = FarmSliceService(SharedPreferencesFarmStore(context))
        val farm = service.createFarm("Integrated Test Farm", currencyCode = "NPR")
        testFarmId = farm.id
        service.setCurrentFarmId(farm.id)
    }

    @After
    fun tearDown() {
        SharedPreferencesFarmStore(context).clear()
    }

    @Test
    fun testCompleteFarmerDailyJourney_endToEnd() {
        val farmId = testFarmId!!
        val milk = service.addProduct(farmId, "Cow Milk", ProductUnit.LITRE)
        val customer = service.addParty(farmId, PartyDraft("Ram Dai", PartyRole.CUSTOMER, "9841000001"))
        val feed = service.addSupply(farmId, "Poultry Feed", ProductUnit.BAG)

        val scenario = ActivityScenario.launch(FarmActivity::class.java)
        try {
            // 1. Initial on Today
            onView(withId(R.id.shellTitle)).check(matches(withText("Integrated Test Farm")))

            // 2. Record production: 50 L Morning
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetProductionButton)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.production_quantity)).inRoot(isDialog()).perform(replaceText("50"), closeSoftKeyboard())
            onView(withText(R.string.production_morning)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.production_save)).inRoot(isDialog()).perform(click())

            // 3. Verify Today production summary
            onView(withId(R.id.todayProductionContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayProductionHeadlineText)).check(matches(withText(containsString("50"))))

            // 4. Record Quick Sale: 20 L sold to Ram Dai at 100 on Credit
            onView(withId(R.id.navRecordItem)).perform(click())
            onView(withId(R.id.recordSheetSellButton)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.quick_sale_quantity)).inRoot(isDialog()).perform(replaceText("20"), closeSoftKeyboard())
            onView(withHint(R.string.quick_sale_rate)).inRoot(isDialog()).perform(replaceText("100"), closeSoftKeyboard())
            onView(withText(R.string.quick_sale_credit)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.quick_sale_save)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.action_done)).inRoot(isDialog()).perform(click())

            // 5. Verify Today attention card has To Receive = 2,000
            onView(withId(R.id.todayReceivableContainer)).perform(scrollTo()).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.todayReceivableAmountText)).check(matches(withText(containsString("2,000"))))

            // 6. Tap View Receivables -> navigates to Khata
            onView(withId(R.id.todayViewReceivablesButton)).perform(scrollTo(), click())
            onView(withId(R.id.khataOverviewContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Dai"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // 7. Open Ram Dai's Khata
            onView(allOf(isDescendantOfA(withId(R.id.partiesContainer)), withText(containsString("Ram Dai"))))
                .perform(scrollTo(), click())
            onView(withId(R.id.partyKhataHeadlineText)).check(matches(withText(containsString("2,000"))))

            // 8. Tap Received Money shortcut inside Ram Dai's Khata
            onView(withId(R.id.khataContextualReceiveButton)).perform(scrollTo(), click())
            onView(withText(R.string.received_money_full_amount)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.received_money_save)).inRoot(isDialog()).perform(click())

            // 9. Ram Dai's Khata is now settled
            onView(withId(R.id.partyKhataHeadlineText)).check(matches(withText(containsString("0"))))
            onView(withId(R.id.closeKhataButton)).perform(click())

            // 10. Navigate to Farm Work
            onView(withId(R.id.navFarmWorkItem)).perform(click())
            onView(allOf(isDescendantOfA(withId(R.id.farmWorkProductionContainer)), withText(containsString("Cow Milk"))))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            // 11. Navigate More -> Hisab -> Back -> Settings -> Back
            onView(withId(R.id.navMoreItem)).perform(click())
            onView(withId(R.id.moreHisabButton)).perform(scrollTo(), click())
            onView(withId(R.id.hisabScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            pressBack()
            onView(withId(R.id.moreScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.moreSettingsButton)).perform(scrollTo(), click())
            onView(withId(R.id.settingsScreen)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            pressBack()

            // 12. Return to Today
            onView(withId(R.id.navTodayItem)).perform(click())
            onView(withId(R.id.todayHeroTitle)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}
