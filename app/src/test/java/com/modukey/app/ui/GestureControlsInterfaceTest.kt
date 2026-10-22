package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GestureControlsInterfaceTest {
    @Test fun ordinarySwipeDirectionCanChangeWithoutChangingSwipeHold() {
        val app = ready()
        editGestures()
        rule.onNodeWithContentDescription("Show Swipe settings").performScrollTo().performClick()
        rule.onNodeWithText("Inward only").performScrollTo().performClick()
        rule.onNodeWithText("Any direction").performClick()
        rule.runOnIdle {
            val settings = app.configuration.preview.value!!.handles.single().trigger
            assertEquals(SwipeDistanceMode.ANY_DIRECTION, settings.swipeDistanceMode)
            assertEquals(SwipeHoldDistanceMode.ANY_DIRECTION, settings.swipeHoldDistanceMode)
        }
        rule.onNodeWithContentDescription("Reset Distance direction").performScrollTo().performClick()
        rule.runOnIdle {
            val settings = app.configuration.preview.value!!.handles.single().trigger
            assertEquals(SwipeDistanceMode.INWARD_ONLY, settings.swipeDistanceMode)
            assertEquals(SwipeHoldDistanceMode.ANY_DIRECTION, settings.swipeHoldDistanceMode)
        }
    }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }

    private fun ready(): NudgeApplication {
        val app = rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.preview.value != null || app.configuration.error.value != null } }
        rule.runOnIdle {
            assertNull(app.configuration.error.value)
            app.configuration.update { AppConfiguration() }
        }
        rule.waitForIdle()
        return app
    }

    private fun editGestures() {
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Edit"))
        rule.onNodeWithContentDescription("Edit").performScrollTo().performClick()
        rule.onNodeWithText("Gestures").performClick()
    }

    @Test fun gestureGroupsKeepDistanceTimingAndFeedbackIndependent() {
        val app = ready(); editGestures()
        rule.onNodeWithText("Swipe settings").assertExists()
        rule.onNodeWithText("Swipe and hold settings").assertExists()
        rule.onNodeWithText("Hold settings").assertExists()
        rule.onNodeWithText("Tap settings").assertExists()
        rule.onNodeWithContentDescription("Swipe distance").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show Swipe settings").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Swipe haptic feedback").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Show Swipe and hold settings").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Swipe and hold distance").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(45f) }
        rule.onAllNodesWithText("After release").onFirst().performScrollTo().performClick()
        rule.onNodeWithText("When hold time ends").performClick()
        rule.runOnIdle {
            val s = app.configuration.preview.value!!.handles.single().trigger
            assertTrue(s.swipeFeedback.enabled); assertFalse(s.swipeHoldFeedback.enabled)
            assertFalse(s.holdFeedback.enabled); assertFalse(s.tapFeedback.enabled)
            assertEquals(45f, s.swipeHoldDistanceDp); assertEquals(24f, s.swipeDistanceDp)
            assertEquals(HoldExecution.AFTER_TIME, s.swipeHoldExecution)
            assertEquals(HoldExecution.ON_RELEASE, s.holdExecution)
        }
    }
    @Test fun `gesture types have prefixes and panel options have separate touch rows`() {
        val app = ready()
        val tools = SavedPanel(name = "Tools")
        val media = SavedPanel(name = "Media")
        rule.runOnIdle { app.configuration.update { it.copy(panels = listOf(tools, media)) } }
        editGestures()
        rule.onNodeWithText("Volume popup").performScrollTo().performClick()
        val none = rule.onNode(hasText("No action") and hasAnyAncestor(isDialog())).fetchSemanticsNode().boundsInRoot
        val panel = rule.onNodeWithText("Panel: Select panel").fetchSemanticsNode().boundsInRoot
        val action = rule.onNodeWithText("Action: Direct action").fetchSemanticsNode().boundsInRoot
        assertTrue(none.top < panel.top)
        assertTrue(panel.top < action.top)
        rule.onNodeWithText("Panel: Select panel").performClick()
        val toolsRow = rule.onNode(hasText("Tools") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val mediaRow = rule.onNode(hasText("Media") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertTrue("Panel options must have a gap", mediaRow.top > toolsRow.bottom)
        rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("Media")
        rule.onNodeWithText("Tools").assertDoesNotExist()
        rule.onNode(hasText("Media") and hasClickAction() and !hasSetTextAction()).performClick()
        rule.onNodeWithText("Panel: Media").assertExists()
        rule.runOnIdle { assertEquals(GestureTarget.VOLUME, app.configuration.preview.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Panel: Media + volume popup").assertExists()
        rule.runOnIdle { assertEquals(GestureTarget.panel(media.id, true), app.configuration.preview.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
    }

    @Test fun `hold controls change and reset only their own delay`() {
        val app = ready()
        val original = TriggerSettings(holdTimeMs = 1_200, swipeHoldTimeMs = 900, depthDp = 22f)
        rule.runOnIdle { app.configuration.update { it.editHandle(it.handles.single().id) { h -> h.copy(trigger = original) } } }
        editGestures()
        rule.onNodeWithContentDescription("Show Hold settings").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Hold time").performScrollTo()
            .performTextReplacement("1500")
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 1_500), app.configuration.preview.value!!.handles.single().trigger) }
        rule.onNodeWithContentDescription("Show Swipe and hold settings").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Swipe and hold time").performScrollTo()
            .performTextReplacement("1700")
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 1_500, swipeHoldTimeMs = 1_700), app.configuration.preview.value!!.handles.single().trigger) }
        rule.onNodeWithContentDescription("Reset Hold time").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 500, swipeHoldTimeMs = 1_700), app.configuration.preview.value!!.handles.single().trigger) }
        rule.onNodeWithContentDescription("Reset Swipe and hold time").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 500, swipeHoldTimeMs = 500), app.configuration.preview.value!!.handles.single().trigger) }
        val hold = rule.onNodeWithContentDescription("Hold time").performScrollTo()
        hold.performTextReplacement("299")
        rule.runOnIdle { assertEquals(500L, app.configuration.preview.value!!.handles.single().trigger.holdTimeMs) }
        hold.performImeAction()
        rule.runOnIdle { assertEquals(300L, app.configuration.preview.value!!.handles.single().trigger.holdTimeMs) }
        hold.performClick().performTextReplacement("3001")
        hold.performImeAction()
        rule.runOnIdle { assertEquals(3000L, app.configuration.preview.value!!.handles.single().trigger.holdTimeMs) }
        hold.performClick().performTextClearance()
        hold.performImeAction()
        rule.runOnIdle { assertEquals(3000L, app.configuration.preview.value!!.handles.single().trigger.holdTimeMs) }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `wrapped action labels stay centered and Input Method remains selectable`() {
        ready()
        var chosen: ButtonAction? = null
        rule.runOnIdle { rule.activity.setContent { NudgeTheme {
            MaterialTheme(typography = MaterialTheme.typography.copy(labelLarge = MaterialTheme.typography.labelLarge.copy(fontSize = 24.sp))) {
                ActionPicker({}, includeEmpty = false) { chosen = it }
            }
        } } }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("Phone settings", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Use a wrapped label to check alignment: ${layouts.single().layoutInput}", layouts.single().lineCount > 1)
        assertEquals(TextAlign.Center, layouts.single().layoutInput.style.textAlign)
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.INPUT_METHOD_PICKER.ordinal)
        rule.onNodeWithText("Input Method").performClick()
        rule.onNodeWithText("Input method picker").assertDoesNotExist()
        rule.runOnIdle { assertEquals(ButtonAction.INPUT_METHOD_PICKER, chosen) }
    }

    @Test fun `Nudge mark can be found and selected without an icon pack`() {
        ready()
        var chosen: IconReference? = null
        rule.runOnIdle { rule.activity.setContent { NudgeTheme { IconPicker(emptyList(), {}) { chosen = it } } } }
        rule.onNode(hasSetTextAction()).performTextInput("Nudge")
        rule.onNodeWithText("Camera").assertDoesNotExist()
        rule.onNode(hasText("Nudge") and hasClickAction() and !hasSetTextAction()).performClick()
        rule.runOnIdle { assertEquals(IconReference(builtIn = BuiltInIcon.NUDGE), chosen) }
    }

    @Test fun `both editors identify visual previews as non-interactive`() {
        val app = ready()
        rule.runOnIdle { app.configuration.update { it.copy(panels = listOf(SavedPanel(name = "Tools"))) } }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Edit"))
        rule.onNodeWithContentDescription("Edit").performScrollTo().performClick()
        rule.onAllNodesWithText("Visual Preview (Non-interactive)").assertCountEquals(2)
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
        rule.onAllNodesWithText("Visual Preview (Non-interactive)").assertCountEquals(2)
    }
    @Test fun `utility actions can be selected directly and survive a configuration backup`() {
        val app = ready()
        val buttons = listOf(PanelButton(action = ButtonAction.CLOSE_PANELS), PanelButton(action = ButtonAction.SCREENSHOT),
            PanelButton(action = ButtonAction.NOTIFICATIONS), PanelButton(action = ButtonAction.CONTROL_PANEL))
        rule.runOnIdle { app.configuration.update { it.copy(panels = listOf(SavedPanel(settings = PanelConfig(buttons = buttons)))) } }
        editGestures()
        rule.onNodeWithText("Volume popup").performScrollTo().performClick()
        rule.onNodeWithText("Action: Direct action").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.CLOSE_PANELS.ordinal)
        rule.onNodeWithText("Close panels").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Action: Close panels + volume popup").performClick()
        rule.onNode(hasText("Action: Close panels") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.SCREENSHOT.ordinal)
        rule.onNodeWithText("Screenshot").performClick()
        rule.onNodeWithText("Save").performClick()
        var previous = "Screenshot"
        for (action in listOf(ButtonAction.NOTIFICATIONS, ButtonAction.CONTROL_PANEL)) {
            rule.onNodeWithText("Action: $previous + volume popup").performClick()
            rule.onNode(hasText("Action: $previous") and hasAnyAncestor(isDialog())).performClick()
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(action.ordinal)
            previous = rule.activity.getString(label(action))
            rule.onNodeWithText(previous).performClick()
            rule.onNodeWithText("Save").performClick()
        }
        rule.onNodeWithContentDescription("Save").performClick()
        rule.runOnIdle {
            val saved = app.configuration.state.value!!
            assertEquals(ButtonAction.CONTROL_PANEL, saved.handles.single().target(GestureSlot.SWIPE_1).action!!.action)
            assertEquals(buttons, saved.panels.single().settings.buttons)
            assertEquals(saved, com.modukey.app.data.ConfigurationCodec.decode(com.modukey.app.data.ConfigurationCodec.encode(saved)))
        }
    }
    @Test fun `orientation tabs update both live editors and survive recreation without changing settings`() {
        val app = ready()
        rule.runOnIdle {
            app.configuration.update { it.copy(panels = listOf(SavedPanel(name = "Tools"))) }
            app.status.report(com.modukey.app.platform.ServiceState(connected = true))
        }
        val saved = rule.runOnIdle { app.configuration.preview.value }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Edit"))
        rule.onNodeWithContentDescription("Edit").performScrollTo().performClick()
        rule.onNodeWithText("Landscape").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(true, app.editor.state.value?.landscape); assertEquals(saved, app.configuration.preview.value) }
        rule.activityRule.scenario.recreate(); rule.waitForIdle()
        rule.onNodeWithText("Landscape").assertIsSelected()
        rule.runOnIdle { assertEquals(true, app.editor.state.value?.landscape) }
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
        rule.onNodeWithText("Landscape").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(true, app.editor.state.value?.landscape); assertEquals(saved, app.configuration.preview.value) }
        rule.onNodeWithText("Portrait").performClick()
        rule.runOnIdle { assertEquals(false, app.editor.state.value?.landscape); assertEquals(saved, app.configuration.preview.value) }
    }
}
