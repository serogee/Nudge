package com.modukey.app.ui

import android.content.pm.ShortcutManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import com.modukey.app.platform.PinnedShortcuts
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ButtonBackgroundInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }

    private fun ready(): NudgeViewModel {
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null || app.configuration.error.value != null } }
        rule.runOnIdle { assertNull(app.configuration.error.value); app.configuration.update { AppConfiguration() } }
        return NudgeViewModel(app)
    }

    private fun backgroundMode(current: String, next: String) {
        rule.onNodeWithText(current).performScrollTo().performClick()
        rule.onNodeWithText(next).performClick()
    }

    private fun chooseColor(hex: String) {
        rule.onNodeWithContentDescription("Edit Background color").performScrollTo().performClick()
        rule.onNode(hasText("Hex color") and hasSetTextAction()).performTextClearance()
        rule.onNode(hasText("Hex color") and hasSetTextAction()).performTextInput(hex)
        rule.onNodeWithText("Done").performClick()
    }

    @Test fun editorKeepsCustomColorAcrossModesAndSavesTheBackgroundWithoutChangingArtwork() {
        val vm = ready()
        val original = PanelButton(icon = IconReference(builtIn = BuiltInIcon.NUDGE))
        var saved: PanelButton? = null
        rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, {}) { saved = it } } }
        rule.onNodeWithText("Panel default").assertDoesNotExist()
        rule.onNodeWithText("With Container").performScrollTo().performClick()
        rule.onNodeWithText("Standalone").performClick()
        backgroundMode("Adaptive", "Choose color")
        chooseColor("3579AB")
        backgroundMode("Choose color", "Transparent")
        backgroundMode("Transparent", "Choose color")
        rule.runOnIdle { assertNull(saved); assertEquals(ButtonBackground(), original.background) }
        rule.onNodeWithContentDescription("Edit Background color").performScrollTo().performClick()
        rule.onNode(hasText("Hex color") and hasSetTextAction()).assertTextContains("3579AB")
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            assertEquals(original.copy(iconContainer = IconContainer.STANDALONE,
                background = ButtonBackground(ButtonBackgroundMode.CUSTOM, 0xFF3579AB)), saved)
        }
    }

    @Test fun transparentIsAvailableForBothStylesAndCancelLeavesTheOriginalUntouched() {
        val vm = ready()
        for (style in listOf(IconContainer.CONTAINER, IconContainer.STANDALONE)) {
            val original = PanelButton(iconContainer = style)
            var saved: PanelButton? = null
            var cancelled = false
            rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, { cancelled = true }) { saved = it } } }
            backgroundMode("Adaptive", "Transparent")
            rule.onNodeWithText("Cancel").performClick()
            rule.runOnIdle { assertTrue(cancelled); assertNull(saved); assertEquals(ButtonBackground(), original.background) }
        }
    }

    @Test fun homeShortcutEditorOffersTheSameBackgroundModes() {
        val vm = ready()
        val original = PanelButton()
        var saved: PanelButton? = null
        rule.activity.setContent { NudgeTheme { ButtonEditor(original, com.modukey.app.platform.ShortcutIconRenderer.panel,
            vm, {}, includeEmpty = false, homeShortcut = true) { saved = it } } }
        backgroundMode("Adaptive", "Transparent")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(original.copy(background = ButtonBackground(ButtonBackgroundMode.TRANSPARENT)), saved) }
    }

    @Test fun editingSavedShortcutChangesOnlyItsBackgroundAndRefreshesTheSamePinnedId() {
        val vm = ready()
        val manager = app.getSystemService(ShortcutManager::class.java)
        shadowOf(manager).setIsRequestPinShortcutSupported(true)
        val first = HomeShortcut(name = "First", button = PanelButton())
        val second = HomeShortcut(name = "Second", button = PanelButton())
        rule.runOnIdle { app.configuration.update { it.copy(shortcuts = listOf(first, second)) } }
        rule.activity.lifecycleScope.launch {
            val shortcuts = PinnedShortcuts(app, app.icons)
            shortcuts.pin(first, first.name); shortcuts.pin(second, second.name)
        }
        rule.waitUntil(10_000) { rule.runOnIdle { manager.pinnedShortcuts.size == 2 } }
        rule.activity.setContent {
            val config by app.configuration.state.collectAsStateWithLifecycle()
            NudgeTheme { HomeShortcutsSection(checkNotNull(config), vm) }
        }
        rule.onAllNodesWithContentDescription("Edit shortcut").onFirst().performClick()
        backgroundMode("Adaptive", "Choose color")
        chooseColor("3579AB")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(10_000) { rule.runOnIdle { !vm.shortcutBusy.value && app.configuration.state.value!!.shortcuts.first().button.background.mode == ButtonBackgroundMode.CUSTOM } }
        rule.runOnIdle {
            val stored = app.configuration.state.value!!.shortcuts
            assertEquals(first.copy(button = first.button.copy(background = ButtonBackground(ButtonBackgroundMode.CUSTOM, 0xFF3579AB))), stored[0])
            assertEquals(second, stored[1])
            assertEquals(setOf(first.id, second.id), manager.pinnedShortcuts.map { it.id }.toSet())
            assertNull(app.configuration.error.value)
        }
    }

    @Test fun staleShortcutEditsCannotOverwriteNewerSettingsOrRecreateDeletedShortcuts() {
        val vm = ready()
        shadowOf(app.getSystemService(ShortcutManager::class.java)).setIsRequestPinShortcutSupported(true)
        val original = HomeShortcut(name = "Original")
        val newer = original.copy(name = "Newer")
        val changed = original.copy(button = original.button.copy(background = ButtonBackground(ButtonBackgroundMode.TRANSPARENT)))
        rule.runOnIdle { app.configuration.update { it.copy(shortcuts = listOf(newer)) }; vm.updateShortcut(original, changed) }
        rule.waitUntil(10_000) { rule.runOnIdle { !vm.shortcutBusy.value } }
        rule.runOnIdle { assertEquals(listOf(newer), app.configuration.state.value!!.shortcuts) }
        rule.runOnIdle { app.configuration.update { it.copy(shortcuts = emptyList()) }; vm.updateShortcut(original, changed) }
        rule.waitUntil(10_000) { rule.runOnIdle { !vm.shortcutBusy.value } }
        rule.runOnIdle { assertTrue(app.configuration.state.value!!.shortcuts.isEmpty()) }
    }
}
