package com.modukey.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.Lifecycle
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.R
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
@Config(sdk = [36], qualifiers = "mdpi")
class EditorSaveInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    private fun ready(): NudgeApplication {
        val app = rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null || app.configuration.error.value != null } }
        rule.runOnIdle { assertNull(app.configuration.error.value); app.configuration.update { AppConfiguration() } }
        rule.waitForIdle()
        return app
    }
    private fun openHandle() {
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
    }
    private fun openPanel() {
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
    }

    @Test fun `thickness slider uses dp and saves independently of touch depth and length`() {
        val app = ready()
        rule.runOnIdle { app.configuration.update { it.editHandle("first-handle") { h -> h.copy(trigger = h.trigger.copy(
            indicatorVisible = true, depthDp = 24f, indicatorThicknessDp = 6f, indicatorLengthFraction = .73f)) } } }
        openHandle()
        val thickness = rule.onNodeWithContentDescription("Handle thickness")
        thickness.performScrollTo().assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(6f, 0f..32f)))
        thickness.performSemanticsAction(SemanticsActions.SetProgress) { it(18f) }
        rule.onNodeWithText(rule.activity.getString(R.string.dp_value, 18)).assertExists()
        rule.onNodeWithContentDescription("Touch depth").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(8f) }
        rule.runOnIdle {
            val draft = app.configuration.preview.value!!.handles.single().trigger
            assertEquals(18f, draft.indicatorThicknessDp); assertEquals(8f, draft.depthDp); assertEquals(.73f, draft.indicatorLengthFraction)
            assertEquals(6f, app.configuration.state.value!!.handles.single().trigger.indicatorThicknessDp)
        }
        rule.onNodeWithContentDescription("Save").performClick()
        rule.onNodeWithContentDescription("Touch depth").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(32f) }
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.onNodeWithContentDescription("Reset Handle thickness").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Save").performClick()
        rule.runOnIdle {
            val saved = app.configuration.state.value!!.handles.single().trigger
            assertEquals(4f, saved.indicatorThicknessDp); assertEquals(8f, saved.depthDp); assertEquals(.73f, saved.indicatorLengthFraction)
        }
    }

    @Test fun `handle changes replace rename with save and cancel and apply only on save`() {
        val app = ready(); openHandle()
        val original = rule.runOnIdle { app.configuration.state.value }
        rule.onNodeWithContentDescription("Save").assertDoesNotExist()
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Rename handle").assertDoesNotExist()
        rule.onNodeWithContentDescription("Save").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cancel").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(original, app.configuration.state.value)
            assertTrue(app.configuration.preview.value!!.handles.single().trigger.indicatorVisible)
        }
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.onNodeWithContentDescription("Rename handle").assertIsDisplayed()
        rule.runOnIdle { assertEquals(original, app.configuration.preview.value) }
        rule.onNodeWithText("Make handle visible").performClick()
        rule.onNodeWithContentDescription("Save").performClick()
        rule.onNodeWithContentDescription("Rename handle").assertIsDisplayed()
        rule.runOnIdle {
            assertTrue(app.configuration.state.value!!.handles.single().trigger.indicatorVisible)
            assertEquals(app.configuration.state.value, app.configuration.preview.value)
        }
    }

    @Test fun `panel rename and nested button edits cancel together or save together`() {
        val app = ready()
        val panel = SavedPanel(name = "Tools")
        rule.runOnIdle { app.configuration.update { it.copy(panels = listOf(panel)) } }
        openPanel()
        fun changePanel() {
            rule.onNodeWithContentDescription("Rename panel").performClick()
            rule.onNode(hasText("Name") and hasSetTextAction()).performTextReplacement("Quick tools")
            rule.onNodeWithText("Save").performClick()
            rule.onNodeWithText("Add button").performScrollTo().performClick()
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.LOCK.ordinal)
            rule.onNodeWithText("Lock screen").performClick()
            rule.onNodeWithText("Save").performClick()
        }
        changePanel()
        rule.runOnIdle {
            assertEquals(panel.normalized(), app.configuration.state.value!!.panel(panel.id))
            assertEquals("Quick tools", app.configuration.preview.value!!.panel(panel.id)!!.name)
            assertEquals(ButtonAction.LOCK, app.configuration.preview.value!!.panel(panel.id)!!.settings.buttons.single().action)
        }
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.onNodeWithContentDescription("Rename panel").assertIsDisplayed()
        rule.runOnIdle { assertEquals(panel.normalized(), app.configuration.preview.value!!.panel(panel.id)) }
        changePanel()
        rule.onNodeWithContentDescription("Save").performClick()
        rule.runOnIdle {
            val saved = app.configuration.state.value!!.panel(panel.id)!!
            assertEquals("Quick tools", saved.name)
            assertEquals(ButtonAction.LOCK, saved.settings.buttons.single().action)
        }
    }

    @Test fun `navigation offers save discard or keep editing without silently applying changes`() {
        val app = ready(); openHandle()
        val original = rule.runOnIdle { app.configuration.state.value }
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        rule.onNode(hasText("Info") and hasClickAction()).performClick()
        rule.onNodeWithText("Save changes?").assertIsDisplayed()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithContentDescription("Save").assertIsDisplayed()
        rule.runOnIdle { assertEquals(original, app.configuration.state.value) }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Discard").performClick()
        rule.onNodeWithText("Manage Handles").assertExists()
        rule.runOnIdle { assertEquals(original, app.configuration.preview.value); assertNull(app.configuration.draft.value) }
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Save changes?").assertIsDisplayed()
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Manage Handles").assertExists()
        rule.runOnIdle { assertTrue(app.configuration.state.value!!.handles.single().trigger.indicatorVisible) }
    }

    @Test fun `pending edits survive recreation and background without becoming approved settings`() {
        val app = ready(); openHandle()
        val original = rule.runOnIdle { app.configuration.state.value }
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        rule.activityRule.scenario.recreate(); rule.waitForIdle()
        rule.onNodeWithContentDescription("Save").assertIsDisplayed()
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        rule.runOnIdle { assertNull(app.editor.state.value); assertEquals(original, app.configuration.state.value) }
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED); rule.waitForIdle()
        rule.onNodeWithContentDescription("Cancel").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(original, app.configuration.preview.value) }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `long handle and panel titles ellipsize while header actions stay visible`() {
        val app = ready()
        val name = "Wide title " + "W".repeat(69)
        rule.runOnIdle { app.configuration.update { it.editHandle("first-handle") { h -> h.copy(name = name) }
            .copy(panels = listOf(SavedPanel(name = name))) } }
        fun checkTitle(pending: Boolean, rename: String) {
            val layouts = mutableListOf<TextLayoutResult>()
            val title = rule.onNodeWithTag("editor-title")
            title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            assertTrue("Long title must be ellipsized", layouts.single().isLineEllipsized(0))
            val action = rule.onNodeWithContentDescription(if (pending) "Cancel" else rename).assertIsDisplayed()
            assertTrue(title.fetchSemanticsNode().boundsInRoot.right <= action.fetchSemanticsNode().boundsInRoot.left)
            if (pending) rule.onNodeWithContentDescription("Save").assertIsDisplayed()
        }
        openHandle(); checkTitle(false, "Rename handle")
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        checkTitle(true, "Rename handle")
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        openPanel(); checkTitle(false, "Rename panel")
        rule.onNodeWithText("Close automatically").performScrollTo().performClick()
        checkTitle(true, "Rename panel")
    }
}
