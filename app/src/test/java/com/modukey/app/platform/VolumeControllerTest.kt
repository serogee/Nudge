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
}
