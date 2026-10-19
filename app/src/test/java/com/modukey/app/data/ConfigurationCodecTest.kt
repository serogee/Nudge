package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ConfigurationCodecTest {
    @Test fun sliderShortcutsAndIndependentFlagsRoundTrip() {
        val action = PanelButton(action = ButtonAction.BRIGHTNESS_DOWN)
        val h = HandleConfig(bindings = mapOf(GestureSlot.TAP to GestureTarget(volume = true, brightness = true)))
        val popup = BrightnessPopupSettings(side = BrightnessSide.CUSTOM, widthDp = 88f, heightDp = 310f, x = .2f, y = .7f, fillOpacity = .4f)
        val c = AppConfiguration(handles = listOf(h), brightnessPopup = popup, shortcuts = listOf(HomeShortcut(name = "Dim", button = action)))
        assertEquals(c.normalized(), ConfigurationCodec.decode(ConfigurationCodec.encode(c)))
        assertFalse(h.target(GestureSlot.TAP).isNone)
        val legacy = JSONObject(ConfigurationCodec.encode(c)).put("schema", 6).apply { remove("brightnessPopup"); remove("shortcuts") }
        assertEquals(BrightnessPopupSettings(), ConfigurationCodec.decode(legacy.toString()).brightnessPopup)
        assertTrue(ConfigurationCodec.decode(legacy.toString()).shortcuts.isEmpty())
    }

    @Test fun `new actions and per-button handle scopes survive backup duplication and schema five upgrade`() {
        val buttons = listOf(PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM), PanelButton(action = ButtonAction.BRIGHTNESS_MINIMUM),
            PanelButton(action = ButtonAction.NOTIFICATIONS), PanelButton(action = ButtonAction.CONTROL_PANEL)) +
            HandleToggleScope.entries.map { PanelButton(action = ButtonAction.TOGGLE_HANDLES, handleScope = it) }
        val panel = SavedPanel(settings = PanelConfig(buttons = buttons))
        val handle = HandleConfig(bindings = mapOf(GestureSlot.HOLD to GestureTarget.direct(buttons.last().copy(id = newId()))))
        val config = AppConfiguration(handles = listOf(handle), panels = listOf(panel)).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val copy = AppConfiguration().addCopies(config)
        assertEquals(buttons.map { it.handleScope }, copy.panels.single().settings.buttons.map { it.handleScope })
        assertEquals(HandleToggleScope.OTHERS, copy.handles.last().target(GestureSlot.HOLD).action!!.handleScope)
        val old = JSONObject(ConfigurationCodec.encode(AppConfiguration())).put("schema", 5).toString()
        assertEquals(AppConfiguration().normalized(), ConfigurationCodec.decode(old))
        assertEquals(ConfigurationCodec.SCHEMA, JSONObject(ConfigurationCodec.upgrade(old)).getInt("schema"))
    }
    private fun legacy() = checkNotNull(javaClass.classLoader!!.getResourceAsStream("configuration-v1.json")).bufferedReader().use { it.readText() }

    @Test fun `fresh configuration has no panels and handle duplication shares assigned panels`() {
        assertTrue(AppConfiguration().panels.isEmpty())
        assertTrue(AppConfiguration.fromLegacy(TriggerSettings(enabled = true)).panels.isEmpty())
        val panel = SavedPanel(name = "Tools")
        val h = HandleConfig(bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id, volume = true)))
        assertEquals(h.bindings, h.duplicate().bindings)
        assertFalse(h.duplicate().enabled)
    }

    @Test fun `schema one migration preserves configured and assigned panels and omits unused placeholders`() {
        val source = legacy()
        val value = ConfigurationCodec.decode(source)
        assertTrue(value.enabled); assertTrue(value.includePrereleases)
        assertEquals(2, value.panels.size)
        val h = value.handles.single()
        val first = value.panels.first()
        assertEquals(GestureTarget.panel(first.id), h.target(GestureSlot.SWIPE_1))
        assertEquals(GestureTarget.VOLUME, h.target(GestureSlot.SWIPE_2))
        assertEquals(GestureTarget.panel(value.panels.last().id), h.target(GestureSlot.HOLD))
        assertEquals(12, first.settings.buttons.size)
        assertEquals(PanelLayout(crossCount = 2, maximumLines = 3, extentFraction = .75f, buttonSizeDp = 64f, followHandle = false,
            x = .2f, y = .8f, offsetXDp = -22f, offsetYDp = -14f), first.settings.portrait)
        assertEquals(PanelAnimation.SCALE, first.settings.animation)
        assertEquals(PanelAnimation.SCALE, first.settings.closeAnimation)
        assertEquals(IconShape.SQUIRCLE, first.settings.iconShape)
        assertEquals(OutsideTouch.PASS_THROUGH, first.settings.outsideTouch)
        assertEquals(IconReference(builtIn = BuiltInIcon.CAMERA), first.settings.buttons.first().icon)
        assertTrue(first.settings.buttons.first().showVolumePopup)
        assertEquals(IconContainer.STANDALONE, first.settings.buttons.first().iconContainer)
        assertFalse(first.settings.iconBackground); assertFalse(first.settings.automaticButtonColor); assertFalse(first.settings.automaticIconColor)
        assertFalse(first.settings.autoCloseEnabled); assertEquals(3_000L, first.settings.autoCloseMs)
        assertEquals("Side / 1", first.name)
        assertEquals("legacy-side", h.id)
        assertEquals(Placement(Edge.BOTTOM, .23f), h.trigger.portrait)
        assertTrue(h.trigger.lockScreenEnabled); assertTrue(h.trigger.landscapeOverrideEnabled)
        assertEquals(setOf("game.example"), h.trigger.excludedPackages)
        assertEquals(value, ConfigurationCodec.decode(source))
        assertEquals(value, ConfigurationCodec.decode(ConfigurationCodec.upgrade(source)))
        assertEquals(ConfigurationCodec.upgrade(source), ConfigurationCodec.upgrade(ConfigurationCodec.upgrade(source)))
    }

    @Test fun `schema one unused custom empty panels are retained and localized names can be supplied`() {
        val source = JSONObject(legacy())
        source.getJSONArray("handles").getJSONObject(0).getJSONArray("panels").getJSONObject(1).put("corner", 13)
        val result = ConfigurationCodec.decode(source.toString(), migratedName = { name, _, slot -> "$name #${slot + 1}" })
        assertEquals(3, result.panels.size)
        assertEquals("Side #2", result.panels[1].name)
        assertEquals(13f, result.panels[1].settings.cornerDp)
    }

    @Test fun `backup roundtrip retains global sharing overflow display options icons and channels`() {
        val p = SavedPanel(name = "Tools", settings = PanelConfig(buttons = List(12) {
            PanelButton(action = ButtonAction.OPEN_APP, appPackage = "app.$it", icon = IconReference("pack", "icon_$it"), showVolumePopup = true, iconContainer = IconContainer.STANDALONE)
        }, portrait = PanelLayout(crossCount = 2, maximumLines = 3, offsetXDp = -36f, offsetYDp = 21f), landscapeOverride = true,
            landscape = PanelLayout(mode = PanelMode.SCROLLABLE, direction = PanelDirection.HORIZONTAL, crossCount = 3, offsetXDp = 52f, offsetYDp = -32f),
            outsideTouch = OutsideTouch.PASS_THROUGH, animation = PanelAnimation.SCALE, iconBackground = false, automaticButtonColor = false,
            buttonColor = 0xFF6F6F6F, automaticIconColor = false, iconColor = 0xFF202223, autoCloseEnabled = true, autoCloseMs = 7_500, closeAnimation = PanelAnimation.FADE))
        val h = HandleConfig(name = "Side", enabled = true, bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(p.id, true)))
        val c = AppConfiguration(enabled = true, includePrereleases = true, handles = listOf(h, h.duplicate()), panels = listOf(p, SavedPanel(name = "Unused"))).normalized()
        assertEquals(c, ConfigurationCodec.decode(ConfigurationCodec.encode(c)))
        val copies = AppConfiguration().addCopies(c)
        assertEquals(2, copies.panels.size)
        assertNotEquals(p.id, copies.panels.first().id)
        assertEquals(copies.panels.first().id, copies.handles[1].target(GestureSlot.SWIPE_1).panelId)
        assertEquals(copies.handles[1].bindings, copies.handles[2].bindings)
        assertNotEquals(p.settings.buttons.first().id, copies.panels.first().settings.buttons.first().id)
        assertTrue(copies.handles.drop(1).all { !it.enabled })
        assertFalse(copies.enabled); assertFalse(copies.includePrereleases)
        assertTrue(copies.panels.first().settings.autoCloseEnabled)
        assertEquals(7_500L, copies.panels.first().settings.autoCloseMs)
        assertEquals(c, ConfigurationCodec.decode(ConfigurationCodec.encode(c)))
    }

    @Test fun `panel removal clears panel links atomically and retains volume and other panels`() {
        val p = SavedPanel(); val other = SavedPanel(name = "Other")
        val h = HandleConfig(bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(p.id), GestureSlot.TAP to GestureTarget.panel(p.id, true),
            GestureSlot.HOLD to GestureTarget.panel(other.id)))
        val c = AppConfiguration(handles = listOf(h, h.duplicate()), panels = listOf(p, other)).normalized().removePanel(p.id)
        for (handle in c.handles) {
            assertEquals(GestureTarget.NONE, handle.target(GestureSlot.SWIPE_1))
            assertEquals(GestureTarget.VOLUME, handle.target(GestureSlot.TAP))
            assertEquals(GestureTarget.panel(other.id), handle.target(GestureSlot.HOLD))
        }
        assertEquals(listOf(other), c.panels)
        assertEquals(c, ConfigurationCodec.decode(ConfigurationCodec.encode(c)))
    }

    @Test fun `invalid future duplicate orphan and oversized documents are rejected`() {
        val p = SavedPanel()
        val duplicateButton = PanelButton(id = "same")
        val bad = listOf("{}", "{\"schema\":999}", "x".repeat(ConfigurationCodec.MAX_BYTES + 1),
            ConfigurationCodec.encode(AppConfiguration(handles = listOf(HandleConfig(id = "same"), HandleConfig(id = "same")))),
            ConfigurationCodec.encode(AppConfiguration(panels = listOf(p, p))),
            ConfigurationCodec.encode(AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(duplicateButton, duplicateButton)))))),
            ConfigurationCodec.encode(AppConfiguration(handles = listOf(HandleConfig(bindings = mapOf(GestureSlot.TAP to GestureTarget.panel("missing")))))))
        for (s in bad) try { ConfigurationCodec.decode(s); fail("accepted invalid document") } catch (_: IOException) { }
    }
    @Test fun `panel timeout imports use safe bounds without changing other settings`() {
        val one=SavedPanel(settings=PanelConfig(autoCloseEnabled=true,autoCloseMs=Long.MAX_VALUE,cornerDp=17f))
        val two=SavedPanel(settings=PanelConfig(autoCloseMs=-1,cornerDp=13f))
        val value=ConfigurationCodec.decode(ConfigurationCodec.encode(AppConfiguration(panels=listOf(one,two))))
        assertEquals(one.copy(settings=one.settings.copy(autoCloseMs=60_000)),value.panels.first())
        assertEquals(two.copy(settings=two.settings.copy(autoCloseMs=1_000)),value.panels.last())
    }
    @Test fun `missing close effect inherits opening once and later changes remain independent`() {
        val p=SavedPanel(settings=PanelConfig(animation=PanelAnimation.SLIDE))
        val document=JSONObject(ConfigurationCodec.encode(AppConfiguration(panels=listOf(p))))
        document.getJSONArray("panels").getJSONObject(0).remove("closeAnimation")
        val migrated=ConfigurationCodec.decode(document.toString())
        assertEquals(PanelAnimation.SLIDE,migrated.panels.single().settings.closeAnimation)
        val edited=migrated.editPanel(p.id) { it.copy(settings=it.settings.copy(animation=PanelAnimation.FADE)) }
        val reopened=ConfigurationCodec.decode(ConfigurationCodec.encode(edited))
        assertEquals(PanelAnimation.FADE,reopened.panels.single().settings.animation)
        assertEquals(PanelAnimation.SLIDE,reopened.panels.single().settings.closeAnimation)
    }
    @Test fun `all direct actions roundtrip and copies own independent action ids`() {
        val handles=ButtonAction.entries.filter { it != ButtonAction.EMPTY }.map { action -> HandleConfig(enabled=true,bindings=mapOf(
            GestureSlot.SWIPE_1 to GestureTarget.direct(PanelButton(action=action,appPackage="test.camera",appActivity="test.camera.Main",appLabel="Camera",showVolumePopup=true)))) }
        val original=AppConfiguration(enabled=true,handles=handles).normalized()
        assertEquals(original,ConfigurationCodec.decode(ConfigurationCodec.encode(original)))
        val copies=AppConfiguration(handles=emptyList()).addCopies(original)
        assertEquals(original.handles.size,copies.handles.size)
        original.handles.zip(copies.handles).forEach { (old,copy) ->
            val action=old.target(GestureSlot.SWIPE_1).action!!
            val copied=copy.target(GestureSlot.SWIPE_1).action!!
            assertFalse(copy.enabled); assertNotEquals(old.id,copy.id); assertNotEquals(action.id,copied.id)
            assertEquals(action.copy(id=copied.id),copied)
        }
        assertEquals(copies,ConfigurationCodec.decode(ConfigurationCodec.encode(copies)))
        val duplicate=handles.first().duplicate()
        assertNotEquals(handles.first().target(GestureSlot.SWIPE_1).action!!.id,duplicate.target(GestureSlot.SWIPE_1).action!!.id)
    }
    @Test fun `invalid direct targets are rejected before import and panel deletion keeps direct actions`() {
        val button=PanelButton(action=ButtonAction.LOCK)
        val panel=SavedPanel(settings=PanelConfig(buttons=listOf(button)))
        val target=GestureTarget.direct(button)
        val h=HandleConfig(bindings=mapOf(GestureSlot.HOLD to target))
        val original=AppConfiguration(handles=listOf(h),panels=listOf(SavedPanel()))
        assertEquals(target,original.removePanel(original.panels.single().id).handles.single().target(GestureSlot.HOLD))
        val invalid=listOf(
            ConfigurationCodec.encode(original.copy(panels=listOf(panel))),
            ConfigurationCodec.encode(original.copy(handles=listOf(h.copy(bindings=mapOf(GestureSlot.HOLD to target.copy(panelId=original.panels.single().id)))))),
            JSONObject(ConfigurationCodec.encode(original)).apply {
                getJSONArray("handles").getJSONObject(0).getJSONObject("bindings").getJSONObject("HOLD").put("action","LOCK")
            }.toString())
        for (text in invalid) try { ConfigurationCodec.decode(text); fail("Accepted invalid direct action") } catch (_:IOException) { }
    }
}
