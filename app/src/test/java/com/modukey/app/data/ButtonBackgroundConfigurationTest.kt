package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ButtonBackgroundConfigurationTest {
    @Test fun stylesAndBackgroundsRoundTripIndependentlyAcrossPanelsShortcutsAndActions() {
        val buttons = listOf(IconContainer.CONTAINER, IconContainer.STANDALONE).flatMap { style ->
            ButtonBackgroundMode.entries.map { mode -> PanelButton(iconContainer = style, background = ButtonBackground(mode, 0xFF7941AF)) }
        }
        val panel = SavedPanel(settings = PanelConfig(buttons = buttons))
        val shortcuts = buttons.map { HomeShortcut(button = it.copy(id = newId())) }
        val direct = PanelButton(background = ButtonBackground(ButtonBackgroundMode.CUSTOM, 0xFF336699))
        val source = AppConfiguration(panels = listOf(panel), shortcuts = shortcuts,
            handles = listOf(HandleConfig(bindings = mapOf(GestureSlot.HOLD to GestureTarget.direct(direct))))).normalized()
        assertEquals(source, ConfigurationCodec.decode(ConfigurationCodec.encode(source)))
        val changed = source.copy(shortcuts = source.shortcuts.mapIndexed { index, shortcut ->
            if (index == 0) shortcut.copy(button = shortcut.button.copy(background = ButtonBackground(ButtonBackgroundMode.CUSTOM, 0xFF123456))) else shortcut
        })
        val restored = ConfigurationCodec.decode(ConfigurationCodec.encode(changed))
        assertEquals(source.shortcuts.drop(1), restored.shortcuts.drop(1))
        assertEquals(source.panels, restored.panels)
        assertEquals(source.handles, restored.handles)
        assertEquals(source.shortcuts.map { it.id }, restored.shortcuts.map { it.id })
        assertEquals(0xFF123456, restored.shortcuts.first().button.background.color)
    }

    @Test fun schema32DefaultsResolveAgainstTheOwnerAndPreserveStandaloneAndEmptyCells() {
        fun panel(background: Boolean) = SavedPanel(settings = PanelConfig(iconBackground = background,
            buttons = IconContainer.entries.map { PanelButton(iconContainer = it) } +
                PanelButton(action = ButtonAction.EMPTY, emptyAppearance = EmptyAppearance.NO_ICON, iconContainer = IconContainer.STANDALONE)))
        val source = AppConfiguration(panels = listOf(panel(true), panel(false)),
            shortcuts = IconContainer.entries.map { HomeShortcut(button = PanelButton(iconContainer = it)) })
        val legacy = JSONObject(ConfigurationCodec.encode(source)).put("schema", 32)
        val panels = legacy.getJSONArray("panels")
        for (index in 0 until panels.length()) {
            val buttons = panels.getJSONObject(index).getJSONArray("buttons")
            for (button in 0 until buttons.length()) buttons.getJSONObject(button).remove("background")
        }
        val shortcuts = legacy.getJSONArray("shortcuts")
        for (index in 0 until shortcuts.length()) shortcuts.getJSONObject(index).getJSONObject("button").remove("background")
        val restored = ConfigurationCodec.decode(legacy.toString())
        assertEquals(IconContainer.CONTAINER, restored.panels[0].settings.buttons[0].iconContainer)
        assertEquals(ButtonBackgroundMode.AUTOMATIC, restored.panels[0].settings.buttons[0].background.mode)
        assertEquals(IconContainer.STANDALONE, restored.panels[1].settings.buttons[0].iconContainer)
        assertEquals(ButtonBackgroundMode.TRANSPARENT, restored.panels[1].settings.buttons[0].background.mode)
        for (savedPanel in restored.panels) {
            assertEquals(ButtonBackgroundMode.AUTOMATIC, savedPanel.settings.buttons[1].background.mode)
            assertEquals(ButtonBackgroundMode.TRANSPARENT, savedPanel.settings.buttons[2].background.mode)
            assertEquals(ButtonBackgroundMode.AUTOMATIC, savedPanel.settings.buttons[3].background.mode)
        }
        assertEquals(listOf(IconContainer.STANDALONE, IconContainer.CONTAINER, IconContainer.STANDALONE), restored.shortcuts.map { it.button.iconContainer })
        assertEquals(listOf(ButtonBackgroundMode.TRANSPARENT, ButtonBackgroundMode.AUTOMATIC, ButtonBackgroundMode.TRANSPARENT), restored.shortcuts.map { it.button.background.mode })
        val upgraded = ConfigurationCodec.upgrade(legacy.toString())
        assertEquals(33, JSONObject(upgraded).getInt("schema"))
        assertEquals(upgraded, ConfigurationCodec.upgrade(upgraded))
        assertEquals(restored, ConfigurationCodec.decode(upgraded))
        assertEquals(restored, restored.normalized())
    }

    @Test fun customColorsRemainSavedWhenModesChangeAndNormalizeWithoutChangingTransparency() {
        val custom = ButtonBackground(ButtonBackgroundMode.CUSTOM, 0x123456)
        for (mode in ButtonBackgroundMode.entries) {
            val button = PanelButton(background = custom.copy(mode = mode))
            val shortcut = HomeShortcut(button = button)
            val restored = ConfigurationCodec.decode(ConfigurationCodec.encode(AppConfiguration(shortcuts = listOf(shortcut)))).shortcuts.single().button
            assertEquals(mode, restored.background.mode)
            assertEquals(0xFF123456, restored.background.color)
            assertEquals(button.id, restored.id)
        }
    }

    @Test fun duplicatedButtonsKeepTheirBackgroundAndOlderConfigsRemainImportable() {
        val button = PanelButton(iconContainer = IconContainer.STANDALONE, background = ButtonBackground(ButtonBackgroundMode.CUSTOM, 0xFF3759A0))
        val source = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(button))))).normalized()
        val duplicated = AppConfiguration().addCopies(source).panels.single().settings.buttons.single()
        assertNotEquals(button.id, duplicated.id)
        assertEquals(button.background, duplicated.background)
        assertEquals(button.iconContainer, duplicated.iconContainer)
        val legacy = javaClass.classLoader!!.getResource("configuration-v1.json")!!.readText()
        assertTrue(ConfigurationCodec.decode(legacy).panels.isNotEmpty())
    }
}
