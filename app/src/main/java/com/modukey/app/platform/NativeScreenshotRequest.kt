package com.modukey.app.platform

import android.view.Choreographer

/** Wait for removed panel surfaces to leave the screen. No callback remains when idle. */
internal class NativeScreenshotRequest(private val choreographer: Choreographer, private val eligible: () -> Boolean,
    private val prepare: () -> Unit, private val capture: () -> Boolean, private val failed: () -> Unit) {
    private var remaining = 0
    private val frame = Choreographer.FrameCallback { advance() }
    fun request(): Boolean {
        if (!eligible()) return false
        if (remaining != 0) return true
        prepare()
        remaining = 2
        choreographer.postFrameCallback(frame)
        return true
    }
    private fun advance() {
        if (remaining == 0) return
        if (!eligible()) { cancel(); return }
        remaining--
        if (remaining > 0) choreographer.postFrameCallback(frame)
        else if (!try { capture() } catch (_: RuntimeException) { false }) failed()
    }
    fun cancel() { remaining = 0; choreographer.removeFrameCallback(frame) }
}
