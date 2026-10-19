package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
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
@Config(sdk = [36], qualifiers = "mdpi")
class ReorderScrollInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }

    @Test fun `one held pointer can reorder down and back up through offscreen rows without dropping`() = verifyHeldReorder(false)
    @Test fun aScrollableHeaderDoesNotChangeReorderIndicesOrReleaseTheHeldItem() = verifyHeldReorder(true)
    private fun verifyHeldReorder(withHeader: Boolean) {
        val values = mutableStateOf(List(20) { "Item $it" })
        var finishes = 0
        rule.activity.setContent { NudgeTheme {
            ReorderableList(values.value, { it }, { it }, Modifier.height(260.dp).fillMaxWidth().testTag("list"),
                moveTo = { key, target -> values.value = values.value.toMutableList().apply { add(target, removeAt(indexOf(key))) } },
                finish = { finishes++ }, header = if (withHeader) ({ Text("Controls", Modifier.height(110.dp)) }) else null) { item, _ -> Text(item, Modifier.height(72.dp)) }
        } }
        val list = rule.onNodeWithTag("list")
        val bounds = list.fetchSemanticsNode().boundsInRoot
        val start = rule.onNodeWithContentDescription("Drag to reorder Item 0").fetchSemanticsNode().boundsInRoot.center - bounds.topLeft
        rule.mainClock.autoAdvance = false
        list.performTouchInput { down(start); moveTo(Offset(start.x, bounds.height - 6f), delayMillis = 300) }
        repeat(80) { rule.mainClock.advanceTimeByFrame(); rule.waitForIdle() }
        val lower = rule.runOnIdle { assertEquals(0, finishes); values.value.indexOf("Item 0") }
        assertTrue("Must continue past the initial viewport: $lower", lower >= 6)
        list.performTouchInput { moveTo(Offset(start.x, 6f), delayMillis = 300) }
        repeat(80) { rule.mainClock.advanceTimeByFrame(); rule.waitForIdle() }
        rule.runOnIdle { assertEquals(0, finishes); assertTrue(values.value.indexOf("Item 0") < lower) }
        list.performTouchInput { up() }
        val released = rule.runOnIdle { assertEquals(1, finishes); values.value }
        rule.mainClock.advanceTimeBy(2_000); rule.waitForIdle()
        rule.runOnIdle { assertEquals(released, values.value); assertEquals(1, finishes); assertEquals(20, values.value.toSet().size) }
    }
    @Test fun `scrolling from content does not start a reorder and cancellation finishes a captured drag once`() {
        val original = List(15) { "Item $it" }
        val values = mutableStateOf(original)
        var finishes = 0
        rule.activity.setContent { NudgeTheme {
            ReorderableList(values.value, { it }, { it }, Modifier.height(260.dp).fillMaxWidth().testTag("list"),
                moveTo = { key, target -> values.value = values.value.toMutableList().apply { add(target, removeAt(indexOf(key))) } },
                finish = { finishes++ }) { item, _ -> Text(item, Modifier.height(72.dp).fillMaxWidth()) }
        } }
        val list = rule.onNodeWithTag("list")
        list.performTouchInput { swipeUp() }
        rule.runOnIdle { assertEquals(original, values.value); assertEquals(0, finishes) }
        list.performScrollToIndex(0)
        val bounds = list.fetchSemanticsNode().boundsInRoot
        val start = rule.onNodeWithContentDescription("Drag to reorder Item 0").fetchSemanticsNode().boundsInRoot.center - bounds.topLeft
        list.performTouchInput { down(start); moveBy(Offset(0f, 30f)); cancel() }
        rule.runOnIdle { assertEquals(1, finishes) }
    }
}
