package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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
class WheelFocusTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun smoothFocusInterpolatesAndRetargetsWithoutMovingTheLogicalSelection() {
        val selected = mutableStateOf<Int?>(0)
        var weights = emptyList<Float>()
        rule.activity.setContent { weights = wheelFocusWeights(WheelLayoutSettings(focusDurationMs = 200), 3, selected.value) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { selected.value = 1 }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(80)
        rule.runOnIdle {
            assertTrue("weights=$weights enabled=${android.animation.ValueAnimator.areAnimatorsEnabled()}", weights[0] in .01f.. .99f); assertTrue("weights=$weights", weights[1] in .01f.. .99f)
            assertEquals(1, selected.value)
            selected.value = 2
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(80)
        rule.runOnIdle { assertTrue(weights[2] in .01f.. .99f); assertEquals(2, selected.value) }
        rule.mainClock.advanceTimeBy(400)
        rule.runOnIdle { assertEquals(listOf(0f, 0f, 1f), weights) }
    }
    @Test fun noneAndDisabledAnimationsSwitchImmediatelyEvenWhenExpansionIsOff() {
        val selected = mutableStateOf<Int?>(0)
        val settings = mutableStateOf(WheelLayoutSettings(expandSelection = false, focusAnimation = WheelFocusAnimation.NONE))
        var weights = emptyList<Float>()
        rule.activity.setContent { weights = wheelFocusWeights(settings.value, 3, selected.value) }
        rule.runOnIdle { selected.value = 1 }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(listOf(0f, 1f, 0f), weights)
            settings.value = settings.value.copy(animationsEnabled = false, focusAnimation = WheelFocusAnimation.SPRING)
            selected.value = 2
        }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(listOf(0f, 0f, 1f), weights) }
    }
}
