package com.modukey.app.platform

import com.modukey.app.core.Bounds
import org.junit.Assert.*
import org.junit.Test

class NativeBrightnessTouchCompletionTest {
    private val screen = Bounds(0f, 0f, 1080f, 2400f)
    private val control = NativeBrightnessControl("native", 0f, 2047f, 1024f, true,
        Bounds(100f, 200f, 120f, 600f), requiresTouch = true)
    @Test fun relativeSliderCompletionStaysAtItsCurrentValueWithAStationaryCenterTouch() {
        assertEquals(160f to 500f, NativeBrightnessTouchCompletion.completionPoint(control, screen))
    }
    @Test fun noTouchIsAllowedForUnsupportedOrClippedControls() {
        assertNull(NativeBrightnessTouchCompletion.completionPoint(control.copy(requiresTouch = false), screen))
        assertNull(NativeBrightnessTouchCompletion.completionPoint(control.copy(touchBounds = null), screen))
        for (bounds in listOf(Bounds(-1f, 0f, 120f, 600f), Bounds(1000f, 0f, 120f, 600f),
            Bounds(0f, 2350f, 120f, 600f), Bounds(0f, 0f, 1f, 100f), Bounds(Float.NaN, 0f, 120f, 600f))) {
            assertNull(NativeBrightnessTouchCompletion.completionPoint(control.copy(touchBounds = bounds), screen))
        }
    }
    @Test fun aHigherWindowCannotReceiveTheNativeSliderTouch() {
        val native = NativeBrightnessTouchCompletion.Companion.WindowRegion(1, 4, screen)
        val other = NativeBrightnessTouchCompletion.Companion.WindowRegion(2, 5, Bounds(100f, 200f, 120f, 600f))
        val point = 160f to 500f
        assertTrue(NativeBrightnessTouchCompletion.unobscured(point, 1, listOf(native)))
        assertFalse(NativeBrightnessTouchCompletion.unobscured(point, 1, listOf(native, other)))
        assertTrue(NativeBrightnessTouchCompletion.unobscured(point, 1, listOf(native, other.copy(layer = 3))))
        assertTrue(NativeBrightnessTouchCompletion.unobscured(point, 1, listOf(native, other.copy(bounds = Bounds(0f, 2200f, 1080f, 200f)))))
        assertFalse(NativeBrightnessTouchCompletion.unobscured(point, -1, listOf(native)))
        assertFalse(NativeBrightnessTouchCompletion.unobscured(point, 3, listOf(native)))
    }
}
