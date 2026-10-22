package com.modukey.app.platform

import android.media.AudioManager
import com.modukey.app.core.SliderApplied
import kotlin.math.roundToInt

sealed interface VolumeResult {
    data object Requested : VolumeResult
    data class Failed(val message: String) : VolumeResult
}

interface VolumeController {
    fun show(): VolumeResult
    fun showMedia(): VolumeResult = show()
    fun read(): MediaVolume? = null
    fun setFraction(fraction: Float): SliderApplied? = null
}
fun interface AudioPort { fun adjust(direction: Int, suggestedStream: Int, flags: Int) }
data class MediaVolume(val minimum: Int, val maximum: Int, val level: Int) {
    val fraction get() = ((level - minimum).toFloat() / (maximum - minimum).coerceAtLeast(1)).coerceIn(0f, 1f)
    val quantum get() = 1f / (maximum - minimum).coerceAtLeast(1)
}
interface VolumeAccess { fun read(): MediaVolume?; fun set(index: Int); fun show() }

class NativeVolumeController(private val audio: AudioPort, private val access: VolumeAccess?) : VolumeController {
    constructor(audio: AudioPort) : this(audio, null)
    constructor(manager: AudioManager) : this(AudioPort(manager::adjustSuggestedStreamVolume), object : VolumeAccess {
        override fun read() = if (manager.isVolumeFixed) null else MediaVolume(manager.getStreamMinVolume(AudioManager.STREAM_MUSIC),
            manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), manager.getStreamVolume(AudioManager.STREAM_MUSIC))
        override fun set(index: Int) { manager.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_SHOW_UI) }
        override fun show() { manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI) }
    })

    override fun read(): MediaVolume? = runCatching { access?.read()?.takeIf { it.maximum > it.minimum && it.level in it.minimum..it.maximum } }.getOrNull()
    override fun setFraction(fraction: Float): SliderApplied? {
        if (!fraction.isFinite()) return null
        return runCatching {
            val before = read() ?: return null
            val target = before.minimum + ((before.maximum - before.minimum) * fraction.coerceIn(0f, 1f)).roundToInt()
            if (target != before.level) access?.set(target)
            read()?.let { SliderApplied(it.fraction, it.quantum) }
        }.getOrNull()
    }
    override fun showMedia(): VolumeResult = if (access == null) show() else request { access.show() }

    override fun show(): VolumeResult = request { audio.adjust(AudioManager.ADJUST_SAME, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI) }
    private fun request(action: () -> Unit): VolumeResult = try {
        action()
        VolumeResult.Requested
    } catch (_: SecurityException) {
        VolumeResult.Failed("HyperOS blocked the volume request. Open Help and check permissions.")
    } catch (_: IllegalStateException) {
        VolumeResult.Failed("The system audio service is unavailable. Try again after unlocking your phone.")
    }
}
