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
