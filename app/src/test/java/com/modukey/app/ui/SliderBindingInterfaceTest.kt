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
