package com.modukey.app.platform

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
@Suppress("DEPRECATION")
class NativeBrightnessNodeTest {
    private fun node(id: String, pkg: String = NativeBrightnessNode.SYSTEM_UI) = AccessibilityNodeInfo.obtain().apply {
        packageName = pkg; viewIdResourceName = "$pkg:id/$id"; isVisibleToUser = true; isEnabled = true
        setBoundsInScreen(Rect(10, 20, 210, 60))
    }
    private fun slider(id: String = "slider", action: ((Int, android.os.Bundle?) -> Boolean)? = null) = node(id).apply {
        className = "android.widget.SeekBar"
        rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0f, 65535f, 32768f)
        addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
        if (action != null) shadowOf(this).setOnPerformActionListener { code, args -> action(code, args) }
    }
    private fun root(parent: String = "brightness_slider", action: ((Int, android.os.Bundle?) -> Boolean)? = null) = node(parent).apply {
        shadowOf(this).addChild(slider(action = action))
    }
    @Test fun nativeProgressUsesTheActualRangeAndAnExplicitBrightnessAncestor() {
        val original = NativeBrightnessNode.read(listOf(root()))!!
        val actions = mutableListOf<Pair<Int, Float>>()
        assertTrue(NativeBrightnessNode.move(listOf(root(action = { code, args ->
            actions += code to args!!.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE); true
        })), original, .25f))
        assertEquals(listOf(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id to 16384f), actions)
    }
    @Test fun genericVolumeSlidersAndOtherPackagesCannotBeSelected() {
        assertNull(NativeBrightnessNode.read(listOf(root("volume_slider"))))
        val other = node("brightness_slider", "other.app")
        shadowOf(other).addChild(slider())
        assertNull(NativeBrightnessNode.read(listOf(other)))
        val textOnly = root("container").apply { contentDescription = "Brightness"; text = "Brightness" }
        assertNull(NativeBrightnessNode.read(listOf(textOnly)))
    }
    @Test fun pluginResourceNamesAreAcceptedOnlyForSystemUiOwnedBrightnessControls() {
        val actions = mutableListOf<Float>()
        fun plugin() = slider("brightness_slider", action = { _, args ->
            actions += args!!.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE); true
        }).apply { viewIdResourceName = "miui.systemui.plugin:id/brightness_slider" }
        val control = NativeBrightnessNode.read(listOf(plugin()))!!
        assertTrue(NativeBrightnessNode.move(listOf(plugin()), control, .25f))
        assertEquals(listOf(16384f), actions)
        assertNull(NativeBrightnessNode.read(listOf(plugin().apply { packageName = "other.app" })))
    }
    @Test fun relatedColorAndDimmingControlsCannotInheritBrightnessIdentity() {
        assertNull(NativeBrightnessNode.read(listOf(slider("brightness_temperature"))))
        assertNull(NativeBrightnessNode.read(listOf(slider("reduce_brightness_slider"))))
        val brightness = node("brightness_container")
        val color = node("color_temperature")
        shadowOf(color).addChild(slider()); shadowOf(brightness).addChild(color)
        assertNull(NativeBrightnessNode.read(listOf(brightness)))
    }
    @Test fun brightnessIdentityCrossesKnownSliderWrappersButDoesNotReachASiblingVolumeControl() {
        val brightness = node("brightness_container")
        val wrapper = node("toggle_slider"); val inner = node("toggle_slider_inner")
        shadowOf(inner).addChild(slider()); shadowOf(wrapper).addChild(inner); shadowOf(brightness).addChild(wrapper)
        val window = node("control_center")
        shadowOf(window).addChild(brightness); shadowOf(window).addChild(root("volume_slider"))
        assertNotNull(NativeBrightnessNode.read(listOf(window)))
        assertNull(NativeBrightnessNode.read(listOf(node("toggle_slider").apply { shadowOf(this).addChild(slider()) })))
    }
    @Test fun pluginOwnershipRequiresTheServicesExplicitOwnerSetAtEveryLevel() {
        val owners = setOf(NativeBrightnessNode.SYSTEM_UI, NativeBrightnessNode.MIUI_PLUGIN)
        fun plugin() = node("brightness_slider", NativeBrightnessNode.MIUI_PLUGIN).apply {
            shadowOf(this).addChild(slider().apply { packageName = NativeBrightnessNode.MIUI_PLUGIN })
        }
        assertNull(NativeBrightnessNode.read(listOf(plugin())))
        val expected = NativeBrightnessNode.read(listOf(plugin()), owners)!!
        assertTrue(NativeBrightnessNode.move(listOf(plugin()), expected, .5f, owners))
        val untrusted = node("root", "other.app").apply { shadowOf(this).addChild(plugin()) }
        assertNull(NativeBrightnessNode.read(listOf(untrusted), owners))
    }
    @Test fun discoveryReportsDistinguishMissingIdentityAndMissingProgressWithoutReadingText() {
        val reports = mutableListOf<String>()
        val generic = slider().apply { text = "private text"; contentDescription = "private description" }
        assertNull(NativeBrightnessNode.read(listOf(generic), report = reports::add))
        assertTrue(reports.single().contains("brightness=0; ranges=1; candidates=0"))
        assertTrue(reports.single().contains("progress=true"))
        assertFalse(reports.single().contains("private"))
        assertFalse(reports.single().contains("32768"))
        reports.clear()
        val readOnly = slider("brightness_slider").apply { removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS) }
        assertNull(NativeBrightnessNode.read(listOf(readOnly), report = reports::add))
        assertTrue(reports.single().contains("brightness=1; ranges=1; candidates=0"))
        assertTrue(reports.single().contains("progress=false"))
        reports.clear()
        val wrapper = root("unknown_slider_wrapper")
        val brightness = node("brightness_container").apply { shadowOf(this).addChild(wrapper) }
        assertNull(NativeBrightnessNode.read(listOf(brightness), report = reports::add))
        assertTrue(reports.single().contains("path=/brightness_container/unknown_slider_wrapper/slider"))
    }
    @Test fun exactNativeLabelsDistinguishTheTwoHyperOsPluginSliders() {
        val owners = setOf(NativeBrightnessNode.SYSTEM_UI, NativeBrightnessNode.MIUI_PLUGIN)
        val labels = setOf("Display brightness")
        val writes = mutableListOf<String>()
        fun panel() = node("main_panel", NativeBrightnessNode.MIUI_PLUGIN).apply {
            for (label in listOf("Display brightness", "Media")) {
                shadowOf(this).addChild(slider(action = { _, _ -> writes += label; true }).apply {
                    packageName = NativeBrightnessNode.MIUI_PLUGIN
                    viewIdResourceName = "${NativeBrightnessNode.MIUI_PLUGIN}:id/slider"; contentDescription = label
                })
            }
        }
        assertNull(NativeBrightnessNode.read(listOf(panel()), owners))
        val control = NativeBrightnessNode.read(listOf(panel()), owners, labels)!!
        assertTrue(NativeBrightnessNode.move(listOf(panel()), control, .25f, owners, labels))
        assertEquals(listOf("Display brightness"), writes)
    }
    @Test fun nativeLabelMatchingIsExactLocalizedAndNeverUsesTextOrAnUntrustedOwner() {
        val labels = setOf("Bildschirmhelligkeit")
        assertNotNull(NativeBrightnessNode.read(listOf(slider().apply { contentDescription = "Bildschirmhelligkeit" }), labels = labels))
        for (label in listOf("Media", "Bildschirmhelligkeit extra", "")) {
            assertNull(NativeBrightnessNode.read(listOf(slider().apply { contentDescription = label; text = "Bildschirmhelligkeit" }), labels = labels))
        }
        assertNull(NativeBrightnessNode.read(listOf(slider().apply { contentDescription = "Bildschirmhelligkeit"; packageName = "other.app" }), labels = labels))
        assertNull(NativeBrightnessNode.read(listOf(slider("other_slider").apply { contentDescription = "Bildschirmhelligkeit" }), labels = labels))
        assertNull(NativeBrightnessNode.read(List(2) { slider().apply { contentDescription = "Bildschirmhelligkeit" } }, labels = labels))
    }
    @Test fun discoveryReportsAmbiguityAndTraversalLimitsWithoutSelectingAControl() {
        val reports = mutableListOf<String>()
        assertNull(NativeBrightnessNode.read(listOf(root(), root()), report = reports::add))
        assertTrue(reports.single().contains("result=ambiguous"))
        reports.clear()
        val huge = node("container").apply { repeat(512) { shadowOf(this).addChild(node("child")) } }
        assertNull(NativeBrightnessNode.read(listOf(huge), report = reports::add))
        assertTrue(reports.single().contains("result=node_limit"))
    }
    @Test fun anAlreadySelectedPositionDoesNotNeedAnotherNativeAction() {
        val control = NativeBrightnessNode.read(listOf(root()))!!
        assertTrue(NativeBrightnessNode.move(listOf(root(action = { _, _ ->
            fail("An unchanged native position must not require a write"); false
        })), control, .5f))
    }
    @Test fun ambiguousHiddenAndReadOnlyControlsAreRejected() {
        val ambiguous = node("brightness_container")
        shadowOf(ambiguous).addChild(slider("slider")); shadowOf(ambiguous).addChild(slider("seekbar"))
        assertNull(NativeBrightnessNode.read(listOf(ambiguous)))
        assertNull(NativeBrightnessNode.read(listOf(slider("brightness_slider").apply { isVisibleToUser = false })))
        val readOnly = node("brightness_slider").apply { rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(0, 0f, 100f, 50f) }
        assertNull(NativeBrightnessNode.read(listOf(readOnly)))
        val invalid = slider("brightness_slider").apply { rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(0, 1f, 1f, 1f) }
        assertNull(NativeBrightnessNode.read(listOf(invalid)))
    }
}
