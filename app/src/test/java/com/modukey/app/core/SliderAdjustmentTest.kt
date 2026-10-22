package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class SliderAdjustmentTest {
    @Test fun anchoredMovementIsProportionalAndReversesImmediatelyAtLimits() {
        val slider = RelativeSlider(.5f, 400f, 240f)
        assertEquals(.5f, slider.move(400f), .0001f)
        assertEquals(.75f, slider.move(340f), .0001f)
        assertEquals(.25f, slider.move(460f), .0001f)
        assertEquals(1f, slider.move(-400f), .0001f)
        assertEquals(.9f, slider.move(-376f), .0001f)
        assertEquals(.9f, slider.move(Float.NaN), .0001f)
    }
    @Test fun subStepMotionAccumulatesButARealSystemClampRebasesTheValue() {
        val slider = RelativeSlider(.5f, 0f, 100f)
        repeat(4) { n -> slider.move(-(n+1).toFloat()); slider.accept(SliderApplied(.5f, .1f)) }
        assertEquals(.54f, slider.fraction, .0001f)
        slider.accept(SliderApplied(.2f, .1f))
        assertEquals(.21f, slider.move(-5f), .0001f)
    }
    @Test fun continuousSwipeEmitsBeforeReleaseAndExcludesSwipeHold() {
        val gesture = HandleGesture(Edge.LEFT, 24f, 600, 8f, swipeImmediately = true, swipeHoldEnabled = false)
        gesture.down(0f, 0f, 0); gesture.move(30f, 0f, 100)
        assertEquals(GestureKind.SWIPE, gesture.poll(100))
        assertNull(gesture.poll(800)); assertNull(gesture.up(30f, -500f, 1000))
    }
    @Test fun conflictKeepsSavedHoldAssignmentAndHeldSequenceStartsExpiryOnRelease() {
        val held = GestureTarget(brightness = true, slideToAdjust = true)
        val saved = GestureTarget.VOLUME
        val handle = HandleConfig(bindings = mapOf(GestureSlot.SWIPE_1 to held, GestureSlot.SWIPE_2 to saved, GestureSlot.SWIPE_HOLD to saved))
        assertTrue(handle.hasContinuousSwipe)
        assertEquals(saved, handle.target(GestureSlot.SWIPE_HOLD))
        assertTrue(handle.runtimeTarget(GestureSlot.SWIPE_HOLD).isNone)
        val sessions = GestureSessions()
        sessions.submitted(handle, 0, held, 0); sessions.keepWhileTouch(handle.id)
        assertEquals(1, sessions.next(handle, GestureKind.SWIPE, 20_000).second)
        sessions.submitted(handle, 0, held, 20_000)
        assertEquals(1, sessions.next(handle, GestureKind.SWIPE, 22_999).second)
        assertEquals(0, sessions.next(handle, GestureKind.SWIPE, 23_000).second)
    }
    @Test fun previewOwnershipDoesNotAllowAnOlderEditorToClearItsSuccessor() {
        val state = SliderGuidePreview(); val a = Any(); val b = Any()
        val request = SliderGuideRequest("handle", SliderKind.VOLUME, 240f)
        state.begin(a, request); state.begin(b, request.copy(travelDp = 300f)); state.end(a)
        assertEquals(300f, state.state.value!!.travelDp)
        state.end(b); assertNull(state.state.value)
    }
}
