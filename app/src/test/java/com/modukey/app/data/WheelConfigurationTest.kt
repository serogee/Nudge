package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WheelConfigurationTest {
    @Test fun selectedSizeDefaultsTo110PercentAndPreservesExplicitValues() {
        assertEquals(1.1f, WheelLayoutSettings().selectedScale)
        assertEquals(1.1f, WheelLayoutSettings(selectedScale = Float.NaN).normalized().selectedScale)
        val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(selectedScale = 1.25f, autoFit = false))
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(portrait = layout))))
        val json = JSONObject(ConfigurationCodec.encode(config))
        assertEquals(1.25f, ConfigurationCodec.decode(json.toString()).panels.single().settings.portrait.wheel.selectedScale)
        json.getJSONArray("panels").getJSONObject(0).getJSONObject("portrait").getJSONObject("wheel").remove("selectedScale")
        assertEquals(1.1f, ConfigurationCodec.decode(json.toString()).panels.single().settings.portrait.wheel.selectedScale)
    }
    @Test fun autofitChoicesPersistAndOlderWheelsRetainTheirSizesWithAutofitOff() {
        assertTrue(WheelLayoutSettings().autoFit)
        for (enabled in listOf(true, false)) {
            val portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(innerRadiusDp = 40f, outerRadiusDp = 160f, autoFit = enabled))
            val landscape = portrait.copy(wheel = portrait.wheel.copy(autoFit = !enabled, innerRadiusDp = 60f))
            val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(portrait = portrait, landscape = landscape, landscapeOverride = true)))).normalized()
            assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
            val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 25)
            val panel = old.getJSONArray("panels").getJSONObject(0)
            for (orientation in listOf("portrait", "landscape")) panel.getJSONObject(orientation).getJSONObject("wheel").remove("autoFit")
            val restored = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString())).panels.single().settings
            assertFalse(restored.portrait.wheel.autoFit); assertFalse(restored.landscape.wheel.autoFit)
            assertEquals(portrait.wheel.radii(56f), restored.portrait.wheel.radii(56f))
            assertEquals(landscape.wheel.radii(56f), restored.landscape.wheel.radii(56f))
        }
    }
    private fun configuration(): AppConfiguration {
        val panel = SavedPanel(name = "Tools", settings = PanelConfig(buttons = List(9) { PanelButton() },
            portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(200f, false, 1.4f)),
            landscape = PanelLayout(mode = PanelMode.FIXED), landscapeOverride = true))
        val target = GestureTarget.panel(panel.id).copy(wheelPick = WheelPickSettings(true, 60f, 300f, true, 350))
        return AppConfiguration(panels = listOf(panel), handles = listOf(HandleConfig(bindings = mapOf(
            GestureSlot.SWIPE_1 to target, GestureSlot.HOLD to target))))
    }
    @Test fun wheelLayoutsBindingsAndOverflowRoundTrip() {
        val original = configuration().normalized()
        val encoded = ConfigurationCodec.encode(original)
        assertEquals(ConfigurationCodec.SCHEMA, JSONObject(encoded).getInt("schema"))
        assertEquals(original, ConfigurationCodec.decode(encoded))
        assertEquals(9, ConfigurationCodec.decode(encoded).panels.single().settings.buttons.size)
    }
    @Test fun olderConfigurationsGetInactiveWheelDefaults() {
        val json = JSONObject(ConfigurationCodec.encode(AppConfiguration(panels = listOf(SavedPanel())))).put("schema", 20)
        val settings = json.getJSONArray("panels").getJSONObject(0)
        settings.getJSONObject("portrait").remove("wheel"); settings.getJSONObject("landscape").remove("wheel")
        val bindings = json.getJSONArray("handles").getJSONObject(0).getJSONObject("bindings")
        GestureSlot.entries.forEach { bindings.getJSONObject(it.name).remove("wheelPick") }
        val migrated = ConfigurationCodec.decode(json.toString())
        assertEquals(PanelMode.FIXED, migrated.panels.single().settings.portrait.mode)
        assertFalse(migrated.panels.single().isWheel)
        assertTrue(migrated.handles.single().bindings.values.none { it.wheelPick.enabled })
        assertEquals(ConfigurationCodec.SCHEMA, JSONObject(ConfigurationCodec.upgrade(json.toString())).getInt("schema"))
    }
    @Test fun compactWheelsAndAnimationChoicesRoundTripAndOldWheelsKeepTheirRadius() {
        val original = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(
            portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(40f, animationsEnabled = false))))))
        val encoded = ConfigurationCodec.encode(original)
        val restored = ConfigurationCodec.decode(encoded).panels.single().settings.portrait.wheel
        assertEquals(40f, restored.radiusDp); assertFalse(restored.animationsEnabled)
        val legacy = JSONObject(ConfigurationCodec.encode(configuration())).put("schema", 21)
        legacy.getJSONArray("panels").getJSONObject(0).getJSONObject("portrait").getJSONObject("wheel").remove("animationsEnabled")
        val migrated = ConfigurationCodec.decode(legacy.toString()).panels.single().settings.portrait.wheel
        assertEquals(200f, migrated.radiusDp); assertTrue(migrated.animationsEnabled)
    }
    @Test fun focusTransitionChoicesPersistAndLegacyDocumentsUseSmoothMotion() {
        for (animation in WheelFocusAnimation.entries) {
            val wheel = WheelLayoutSettings(focusAnimation = animation, focusDurationMs = 280)
            val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(
                portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = wheel)))))
            assertEquals(wheel, ConfigurationCodec.decode(ConfigurationCodec.encode(config)).panels.single().settings.portrait.wheel)
            val legacy = JSONObject(ConfigurationCodec.encode(config)).put("schema", 23)
            val saved = legacy.getJSONArray("panels").getJSONObject(0).getJSONObject("portrait").getJSONObject("wheel")
            saved.remove("focusAnimation"); saved.remove("focusDurationMs")
            val restored = ConfigurationCodec.decode(legacy.toString()).panels.single().settings.portrait.wheel
            assertEquals(WheelFocusAnimation.SMOOTH, restored.focusAnimation); assertEquals(140, restored.focusDurationMs)
        }
    }
    @Test fun independentRadiiRoundTripAndLegacyWidthsRemainUnchanged() {
        val wheel = WheelLayoutSettings(innerRadiusDp = 64f, outerRadiusDp = 180f)
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(
            portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = wheel)))))
        assertEquals(wheel, ConfigurationCodec.decode(ConfigurationCodec.encode(config)).panels.single().settings.portrait.wheel)
        for (size in listOf(48f, 56f, 96f)) {
            val legacy = JSONObject(ConfigurationCodec.encode(config)).put("schema", 22)
            val layout = legacy.getJSONArray("panels").getJSONObject(0).getJSONObject("portrait")
            layout.put("buttonSize", size)
            layout.put("wheel", JSONObject().put("radiusDp", 200f))
            val restored = ConfigurationCodec.decode(legacy.toString()).panels.single().settings.portrait
            assertEquals(200f - size / 2f to 200f + size / 2f, restored.wheel.radii(size))
        }
    }
    @Test fun oneDpInnerRadiusSurvivesBackupsWithoutChangingLegacyDefaults() {
        val settings = WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 49f)
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(
            portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = settings)))))
        assertEquals(settings, ConfigurationCodec.decode(ConfigurationCodec.encode(config)).panels.single().settings.portrait.wheel)
        assertEquals(100f to 156f, WheelLayoutSettings().radii(56f))
        assertEquals(12f to 68f, WheelLayoutSettings(40f).radii(56f))
    }
    @Test fun inactiveLegacyWheelModesCannotConvertAPanelWhenEnablingLandscape() {
        val oldPanel = PanelConfig(landscape = PanelLayout(mode = PanelMode.WHEEL))
        assertFalse(oldPanel.isWheel)
        val normalized = oldPanel.normalized()
        assertEquals(PanelMode.FIXED, normalized.landscape.mode)
        val edited = oldPanel.copy(landscapeOverride = true).retainKind(oldPanel)
        assertFalse(edited.isWheel)
    }
    @Test fun importedCopiesRetainSharedLinksAndUseFreshIds() {
        val original = configuration()
        val copies = AppConfiguration(handles = emptyList()).addCopies(original)
        val panel = copies.panels.single(); val handle = copies.handles.single()
        assertNotEquals(original.panels.single().id, panel.id)
        assertEquals(panel.id, handle.target(GestureSlot.HOLD).panelId)
        assertEquals(panel.id, handle.target(GestureSlot.SWIPE_1).panelId)
        assertEquals(original.handles.single().target(GestureSlot.HOLD).wheelPick, handle.target(GestureSlot.HOLD).wheelPick)
        assertFalse(handle.enabled)
        assertTrue(panel.settings.buttons.map { it.id }.intersect(original.panels.single().settings.buttons.map { it.id }.toSet()).isEmpty())
    }
    @Test fun tapAndSlidersCannotCaptureTheSameWheelTouch() {
        val original = configuration()
        val target = original.handles.single().target(GestureSlot.SWIPE_1).copy(volume = true, slideToAdjust = true)
        assertFalse(target.normalized().slideToAdjust)
        val handle = HandleConfig(bindings = mapOf(GestureSlot.TAP to target)).normalized()
        assertFalse(handle.target(GestureSlot.TAP).wheelPick.enabled)
        assertFalse(handle.target(GestureSlot.TAP).slideToAdjust)
    }
    @Test fun effectiveRoutingPreservesStoredBindingsAndKeepsWheelsInBothOrientations() {
        val original = configuration()
        val target = original.handles.single().target(GestureSlot.SWIPE_1).copy(wheelPick = WheelPickSettings(enabled = true))
        val handle = HandleConfig(bindings = mapOf(GestureSlot.SWIPE_1 to target, GestureSlot.SWIPE_2 to GestureTarget.VOLUME,
            GestureSlot.SWIPE_HOLD to GestureTarget.VOLUME))
        val effective = handle.effectiveGestures(original, false)
        assertTrue(effective.target(GestureSlot.SWIPE_2).isNone)
        assertTrue(effective.target(GestureSlot.SWIPE_HOLD).isNone)
        assertFalse(handle.target(GestureSlot.SWIPE_2).isNone)
        assertTrue(handle.effectiveGestures(original, true).target(GestureSlot.SWIPE_2).isNone)
        assertEquals(PanelMode.WHEEL, original.normalized().panels.single().settings.landscape.mode)
        val allowed = handle.copy(bindings = handle.bindings + (GestureSlot.SWIPE_1 to target.copy(wheelPick = target.wheelPick.copy(allowLaterSwipes = true))))
        assertEquals(GestureTarget.VOLUME, allowed.effectiveGestures(original, false).target(GestureSlot.SWIPE_2))
        assertTrue(allowed.effectiveGestures(original, false).target(GestureSlot.SWIPE_HOLD).isNone)
    }
}
