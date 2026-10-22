package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SliderBindingInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun pickerOffersCaptureForOnePopupAndSavesItWithoutButtonOverrides() {
        val app = rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null } }
        val vm = NudgeViewModel(app)
        var saved: GestureTarget? = null
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.HOLD, GestureTarget.VOLUME, vm) { saved = it } } }
        rule.onNodeWithText("Volume popup").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Slide to adjust"))
        rule.onNodeWithContentDescription("Slide to adjust").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(GestureTarget(volume = true, slideToAdjust = true), saved) }
    }
    @Test fun disabledSwipeHoldKeepsItsSavedTargetVisible() {
        val app = rule.activity.application as NudgeApplication
        val vm = NudgeViewModel(app)
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.SWIPE_HOLD, GestureTarget.VOLUME, vm, enabled = false) { fail() } } }
        rule.onNodeWithText("Volume popup").assertIsNotEnabled()
    }
    @Test fun swipeGuardDurationCanBeEditedResetAndRetainedWhenTheTargetChanges() {
        val app = rule.activity.application as NudgeApplication
        val vm = NudgeViewModel(app)
        val panel = SavedPanel(name = "Tools")
        var saved: GestureTarget? = null
        val target = GestureTarget(volume = true, slideToAdjust = true)
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(panels = listOf(panel)), GestureSlot.SWIPE_2, target, vm) { saved = it } } }
        rule.onNodeWithText("Volume popup").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Adjustment hold time"))
        rule.onNodeWithContentDescription("Adjustment hold time").performTextReplacement("650")
        rule.onNodeWithContentDescription("Reset Adjustment hold time").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Adjustment hold time").assertTextContains("200")
        rule.onNodeWithContentDescription("Adjustment hold time").performTextReplacement("450")
        rule.onNodeWithContentDescription("Adjustment hold time").performImeAction()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Panel: Select panel"))
        rule.onNodeWithText("Panel: Select panel").performClick()
        rule.onNodeWithText("Tools").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(target.copy(panelId = panel.id, sliderHoldMs = 450), saved) }
    }
    @Test fun swipeCanUseImmediateWhileHoldDoesNotOfferAnExtraDelay() {
        val app = rule.activity.application as NudgeApplication
        val vm = NudgeViewModel(app)
        val target = GestureTarget(brightness = true, slideToAdjust = true, sliderHoldMs = 500)
        var saved: GestureTarget? = null
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.SWIPE_1, target, vm) { saved = it } } }
        rule.onNodeWithText("Brightness slider").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Start adjustment"))
        rule.onNode(hasText("After hold") and hasClickAction()).performClick()
        rule.onNodeWithText("Immediate").performClick()
        rule.onNodeWithContentDescription("Adjustment hold time").assertDoesNotExist()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(target.copy(sliderActivation = SliderActivation.IMMEDIATE), saved) }
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.HOLD, target, vm) {} } }
        rule.onNodeWithText("Brightness slider").performClick()
        rule.onNodeWithText("Start adjustment").assertDoesNotExist()
    }
    @Test fun tapAndTwoPopupBindingsDoNotOfferCapture() {
        val app = rule.activity.application as NudgeApplication
        val vm = NudgeViewModel(app)
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.TAP, GestureTarget.VOLUME, vm) {} } }
        rule.onNodeWithText("Volume popup").performClick()
        rule.onNodeWithContentDescription("Slide to adjust").assertDoesNotExist()
        rule.onNodeWithText("Cancel").performClick()
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.HOLD, GestureTarget(volume = true, brightness = true), vm) {} } }
        rule.onNodeWithText(app.getString(com.modukey.app.R.string.target_with_brightness, app.getString(com.modukey.app.R.string.volume_popup))).performClick()
        rule.onNodeWithContentDescription("Slide to adjust").assertDoesNotExist()
    }
}
