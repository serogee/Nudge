package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HandleGlobalControlsInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }
    private fun ready(enabled: Boolean = true) {
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null } }
        rule.runOnIdle { app.configuration.update { AppConfiguration(enabled = enabled, handles = listOf(
            HandleConfig(id = "a", enabled = true, trigger = TriggerSettings(indicatorVisible = true)),
            HandleConfig(id = "b", enabled = true), HandleConfig(id = "c", enabled = false))) } }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
    }
    @Test fun switchesPrecedeAddHandleAndLeaveSavedHandleSettingsUntouched() {
        ready()
        val before = app.configuration.state.value!!.handles
        val pause = rule.onNodeWithContentDescription("Pause handles")
        val hide = rule.onNodeWithContentDescription("Hide visible handles")
        assertTrue(pause.fetchSemanticsNode().boundsInRoot.top < hide.fetchSemanticsNode().boundsInRoot.top)
        assertTrue(hide.fetchSemanticsNode().boundsInRoot.top < rule.onNodeWithText("Add handle").fetchSemanticsNode().boundsInRoot.top)
        pause.assertIsOff().performClick()
        rule.runOnIdle { assertEquals(setOf("a", "b"), app.handlePauses.state.value); assertEquals(before, app.configuration.state.value!!.handles) }
        pause.assertIsOn().performClick()
        rule.runOnIdle { assertTrue(app.handlePauses.state.value.isEmpty()) }
        hide.assertIsOff().performClick()
        rule.runOnIdle { assertTrue(app.configuration.state.value!!.hideHandleIndicators); assertEquals(before, app.configuration.state.value!!.handles) }
        hide.assertIsOn().performClick()
        rule.runOnIdle { assertFalse(app.configuration.state.value!!.hideHandleIndicators); assertEquals(before, app.configuration.state.value!!.handles) }
        rule.onNodeWithText("Resume handles").assertDoesNotExist()
    }
    @Test fun partialPausesCanBeResumedAndMasterOffDisablesOnlyPause() {
        ready()
        rule.runOnIdle { app.handlePauses.toggle(HandleToggleScope.CURRENT, "a", app.configuration.state.value!!) }
        rule.onNodeWithContentDescription("Pause handles").assertIsOn().performClick()
        rule.runOnIdle { assertTrue(app.handlePauses.state.value.isEmpty()); app.configuration.update { it.copy(enabled = false) } }
        rule.onNodeWithContentDescription("Pause handles").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Hide visible handles").assertIsEnabled().performClick()
        rule.runOnIdle { assertTrue(app.configuration.state.value!!.hideHandleIndicators) }
    }
    @Test fun pickerKeepsBothHandleActionsAfterClosePanels() {
        var selected: ButtonAction? = null
        rule.activity.setContent { NudgeTheme { ActionPicker({}, includeEmpty = false) { selected = it } } }
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.CLOSE_PANELS.ordinal)
        rule.onNodeWithText("Toggle handles").assertIsDisplayed()
        rule.onNodeWithText("Toggle handle visibility").performClick()
        rule.runOnIdle { assertEquals(ButtonAction.TOGGLE_HANDLE_VISIBILITY, selected) }
    }
    @Test fun visibilityActionShowsItsCurrentOnAndOffState() {
        val state = mutableStateOf(SoundState())
        rule.activity.setContent { NudgeTheme { PanelButtonFace(PanelButton(action = ButtonAction.TOGGLE_HANDLE_VISIBILITY), PanelConfig(), 56.dp,
            state.value, true) { state.value = state.value.copy(handleIndicatorsHidden = !state.value.handleIndicatorsHidden) } } }
        rule.onNodeWithContentDescription("Toggle handle visibility. On").assertIsSelected().performClick()
        rule.onNodeWithContentDescription("Toggle handle visibility. Off").assertIsNotSelected().performClick()
        rule.onNodeWithContentDescription("Toggle handle visibility. On").assertIsSelected()
    }
}
