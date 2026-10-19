package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.view.Choreographer

/** Remove Nudge's panel before requesting a system shade. Only two finite frame callbacks are used. */
internal class NativeSystemPanelRequest(private val choreographer: Choreographer, private val eligible: () -> Boolean,
    private val prepare: () -> Unit, private val execute: (Int) -> Boolean, private val failed: () -> Unit) {
    private var pending: Int? = null
    private var remaining = 0
    private val frame = Choreographer.FrameCallback { advance() }
    fun request(action: Int): Boolean {
        if (action !in setOf(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) || !eligible()) return false
        if (pending == action) return true
        cancel(); prepare(); pending = action; remaining = 2
        choreographer.postFrameCallback(frame)
        return true
    }
    private fun advance() {
        val action = pending ?: return
        if (!eligible()) { cancel(); return }
        remaining--
        if (remaining > 0) choreographer.postFrameCallback(frame)
        else { pending = null; if (!try { execute(action) } catch (_: RuntimeException) { false }) failed() }
    }
    fun cancel() { pending = null; remaining = 0; choreographer.removeFrameCallback(frame) }
}
