package com.modukey.app.platform

import android.media.AudioManager

sealed interface VolumeResult {
    data object Requested : VolumeResult
    data class Failed(val message: String) : VolumeResult
}

interface VolumeController { fun show(): VolumeResult }
fun interface AudioPort { fun adjust(direction: Int, suggestedStream: Int, flags: Int) }

class NativeVolumeController(private val audio: AudioPort) : VolumeController {
    constructor(manager: AudioManager) : this(AudioPort(manager::adjustSuggestedStreamVolume))

    override fun show(): VolumeResult = try {
        audio.adjust(AudioManager.ADJUST_SAME, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI)
        VolumeResult.Requested
    } catch (_: SecurityException) {
        VolumeResult.Failed("HyperOS blocked the volume request. Open Help and check permissions.")
    } catch (_: IllegalStateException) {
        VolumeResult.Failed("The system audio service is unavailable. Try again after unlocking your phone.")
    }
}
