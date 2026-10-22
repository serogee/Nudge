package com.modukey.app.platform

import android.media.AudioManager
import org.junit.Assert.*
import org.junit.Test

class VolumeControllerTest {
    @Test fun `requests native UI with no volume adjustment sound or ringer-mode flag`() {
        val calls = mutableListOf<Triple<Int, Int, Int>>()
        val controller = NativeVolumeController { direction, stream, flags -> calls.add(Triple(direction, stream, flags)); Unit }
        assertEquals(VolumeResult.Requested, controller.show())
        assertEquals(listOf(Triple(AudioManager.ADJUST_SAME, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI)), calls)
    }

    @Test fun `blocked API is reported without a custom panel or volume nudge fallback`() {
        var attempts = 0
        val result = NativeVolumeController { _, _, _ -> attempts++; throw SecurityException() }.show()
        assertTrue(result is VolumeResult.Failed)
        assertEquals(1, attempts)
    }
    @Test fun continuousMediaUsesDeviceBoundsAndReadsBackSystemClamps() {
        val writes = mutableListOf<Int>(); var level = 8; var popups = 0
        val access = object : VolumeAccess {
            override fun read() = MediaVolume(3, 13, level)
            override fun set(index: Int) { writes += index; level = index.coerceAtMost(11) }
            override fun show() { popups++ }
        }
        val controller = NativeVolumeController(AudioPort { _, _, _ -> fail() }, access)
        assertEquals(.5f, controller.read()!!.fraction, .0001f)
        assertEquals(.5f, controller.setFraction(.5f)!!.fraction, .0001f); assertTrue(writes.isEmpty())
        assertEquals(.8f, controller.setFraction(1f)!!.fraction, .0001f); assertEquals(listOf(13), writes)
        assertEquals(VolumeResult.Requested, controller.showMedia()); assertEquals(1, popups)
        assertNull(controller.setFraction(Float.NaN))
    }
    @Test fun unavailableOrFixedVolumeCannotCaptureInput() {
        val controller = NativeVolumeController(AudioPort { _, _, _ -> }, object : VolumeAccess {
            override fun read() = MediaVolume(5, 5, 5)
            override fun set(index: Int) { fail() }
            override fun show() { throw SecurityException() }
        })
        assertNull(controller.read()); assertNull(controller.setFraction(.5f))
        assertTrue(controller.showMedia() is VolumeResult.Failed)
    }
}
