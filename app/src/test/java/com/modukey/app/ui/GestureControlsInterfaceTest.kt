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
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").performClick()
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
        rule.onNode(hasSetTextAction()).performTextInput("Media")
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
        rule.onNodeWithContentDescription("Hold time").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1_500f) }
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 1_500), app.configuration.preview.value!!.handles.single().trigger) }
        rule.onNodeWithContentDescription("Swipe and hold time").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1_700f) }
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 1_500, swipeHoldTimeMs = 1_700), app.configuration.preview.value!!.handles.single().trigger) }
        rule.onNodeWithContentDescription("Reset Hold time").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 500, swipeHoldTimeMs = 1_700), app.configuration.preview.value!!.handles.single().trigger) }
        rule.onNodeWithContentDescription("Reset Swipe and hold time").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(original.copy(holdTimeMs = 500, swipeHoldTimeMs = 500), app.configuration.preview.value!!.handles.single().trigger) }
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
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onAllNodesWithText("Visual Preview (Non-interactive)").assertCountEquals(2)
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
        rule.onAllNodesWithText("Visual Preview (Non-interactive)").assertCountEquals(2)
    }
    @Test fun `utility actions can be selected directly and survive a configuration backup`() {
        val app = ready()
        val buttons = listOf(PanelButton(action = ButtonAction.CLOSE_PANELS), PanelButton(action = ButtonAction.SCREENSHOT))
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
        rule.onNodeWithContentDescription("Save").performClick()
        rule.runOnIdle {
            val saved = app.configuration.state.value!!
            assertEquals(ButtonAction.SCREENSHOT, saved.handles.single().target(GestureSlot.SWIPE_1).action!!.action)
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
        rule.onNodeWithContentDescription("Edit").performClick()
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
