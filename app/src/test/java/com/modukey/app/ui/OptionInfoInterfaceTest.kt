package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.ServiceState
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class OptionInfoInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }
    private fun ready() {
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null } }
        rule.runOnIdle { app.configuration.update { AppConfiguration(handles = emptyList()) } }
    }
    private fun tab(name: String) { rule.onNode(hasText(name) and hasClickAction()).performClick() }
    private fun text(resource: Int) = app.getString(resource)

    @Test fun infoSwitchHidesDescriptionsAcrossTabsAndRetainsLiveStatus() {
        ready()
        tab("Info")
        val toggle = rule.onNodeWithContentDescription(text(R.string.additional_option_info))
        toggle.assertIsOn().performClick()
        rule.runOnIdle { assertFalse(app.configuration.state.value!!.showOptionInfo) }
        rule.onNodeWithText(text(R.string.additional_option_info_help)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.backup_help)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.wheel_help_setup)).assertExists()
        tab("Home")
        rule.onNodeWithText(text(R.string.master_help)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.service_disconnected)).assertIsDisplayed()
        rule.runOnIdle { app.status.report(ServiceState(connected = true)) }
        rule.onNodeWithText(text(R.string.service_connected)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.service_disconnected)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.volume_unchecked)).performScrollTo().assertIsDisplayed()
        tab("Handles")
        rule.onNodeWithText(text(R.string.pause_handles_help)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.manage_handles_help)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.no_handles)).performScrollTo().assertIsDisplayed()
        tab("Info")
        toggle.performScrollTo().assertIsOff().performClick()
        rule.runOnIdle { assertTrue(app.configuration.state.value!!.showOptionInfo) }
        rule.onNodeWithText(text(R.string.additional_option_info_help)).assertIsDisplayed()
        tab("Home")
        rule.onNodeWithText(text(R.string.master_help)).assertIsDisplayed()
    }

    @Test fun editorDialogsHideDescriptionsAndRetainValidationAndControls() {
        ready()
        val button = PanelButton(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES,
            toggleScope = HandleGroupScope.NONE, visibilityScope = HandleGroupScope.NONE)
        val vm = NudgeViewModel(app)
        rule.activity.setContent { NudgeTheme {
            CompositionLocalProvider(LocalShowOptionInfo provides false) { ButtonEditor(button, PanelConfig(), vm, {}) {} }
        } }
        rule.onNodeWithText(text(R.string.toggle_and_hide_help)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.handle_group_required)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onAllNodesWithText("None").onFirst().performScrollTo().performClick()
        rule.onNodeWithText("All handles").performClick()
        rule.onNodeWithText("Save").assertIsEnabled()
        rule.onNodeWithText(text(R.string.handle_group_required)).assertDoesNotExist()
    }

    @Test fun durationHintsRemainVisibleForInvalidEntries() {
        ready()
        val show = mutableStateOf(false)
        val value = mutableStateOf(600L)
        rule.activity.setContent { NudgeTheme {
            CompositionLocalProvider(LocalShowOptionInfo provides show.value) {
                DurationInput(R.string.swipe_time, value.value, 200L..1200L, 600) { value.value = it }
            }
        } }
        val range = app.getString(R.string.duration_range, 200, 1200)
        rule.onNodeWithText(range).assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextReplacement("50")
        rule.onNodeWithText(range).assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextReplacement("700")
        rule.onNodeWithText(range).assertDoesNotExist()
        rule.runOnIdle { show.value = true }
        rule.onNodeWithText(range).assertIsDisplayed()
    }
}
