package com.modukey.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import com.modukey.app.core.GestureFeedback
import kotlin.math.roundToInt

/** One finite pulse per completed gesture. No vibration callback or idle work. */
class GestureHaptics(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    @Suppress("DEPRECATION")
    fun perform(feedback: GestureFeedback): Boolean {
        val s = feedback.normalized()
        if (!s.enabled || vibrator?.hasVibrator() != true) return false
        return try {
            val amplitude = if (vibrator.hasAmplitudeControl()) (s.strength * 255).roundToInt().coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
            val effect = VibrationEffect.createOneShot((12 + s.strength * 28).toLong(), amplitude)
            if (Build.VERSION.SDK_INT >= 33) vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY))
            else vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).build())
            true
        } catch (_: RuntimeException) { false }
    }
}
