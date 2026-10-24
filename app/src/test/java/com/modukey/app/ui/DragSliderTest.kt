package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h900dp-mdpi")
class DragSliderTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun trackTapsAndTrackSwipesNeverSeekButThumbDragsAdjustRelatively() {
        val value = mutableStateOf(.5f)
        var finishes = 0
        rule.activity.setContent { NudgeTheme {
            DragSlider(listOf(value.value), 0f..1f, listOf("test"), finish = { finishes++ }) { _, v -> value.value = v }
        } }
        val slider = rule.onNodeWithContentDescription("test")
        slider.performTouchInput { click(Offset(width * .8f, centerY)) }
        slider.performTouchInput { swipe(Offset(width * .8f, centerY), Offset(width * .95f, centerY)) }
        rule.runOnIdle { assertEquals(.5f, value.value); assertEquals(0, finishes) }
        // Press slightly off-center on the thumb. The initial press and a tap must not move it.
        slider.performTouchInput { down(center + Offset(8f, 0f)) }
        rule.runOnIdle { assertEquals(.5f, value.value) }
        slider.performTouchInput { moveBy(Offset(60f, 0f)); up() }
        rule.runOnIdle { assertTrue(value.value > .5f); assertTrue(value.value < .7f); assertEquals(1, finishes) }
    }
    @Test fun rangeThumbsRequireDirectDragsAndNeverCross() {
        val values = mutableStateOf(listOf(100f, 200f))
        rule.activity.setContent { NudgeTheme {
            DragSlider(values.value, 0f..300f, listOf("inner", "outer"),
                limits = { if (it == 0) 0f..(values.value[1] - 48f) else (values.value[0] + 48f)..300f }, finish = {}) { i, v ->
                values.value = values.value.toMutableList().apply { set(i, v) }
            }
        } }
        rule.onRoot().performTouchInput { click(Offset(400f, 24f)) }
        rule.runOnIdle { assertEquals(listOf(100f, 200f), values.value) }
        rule.onNodeWithContentDescription("inner").performTouchInput { down(center) }
        rule.runOnIdle { assertEquals(listOf(100f, 200f), values.value) }
        rule.onNodeWithContentDescription("inner").performTouchInput { moveBy(Offset(400f, 0f)); up() }
        rule.runOnIdle { assertEquals(listOf(152f, 200f), values.value) }
        rule.onNodeWithContentDescription("outer").performTouchInput { swipe(center, center - Offset(400f, 0f)) }
        rule.runOnIdle { assertEquals(listOf(152f, 200f), values.value) }
    }
    @Test fun disabledSlidersIgnoreTouchAndRtlThumbsFollowTheDrag() {
        val value = mutableStateOf(.5f)
        val enabled = mutableStateOf(false)
        rule.activity.setContent { NudgeTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            DragSlider(listOf(value.value), 0f..1f, listOf("test"), enabled.value, finish = {}) { _, v -> value.value = v }
        } } }
        val slider = rule.onNodeWithContentDescription("test")
        slider.performTouchInput { swipe(center, center + Offset(60f, 0f)) }
        rule.runOnIdle { assertEquals(.5f, value.value); enabled.value = true }
        slider.performTouchInput { swipe(center, center + Offset(60f, 0f)) }
        rule.runOnIdle { assertTrue(value.value < .5f) }
    }
}
