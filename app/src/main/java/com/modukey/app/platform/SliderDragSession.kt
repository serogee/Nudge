package com.modukey.app.platform

import android.os.Handler
import android.os.Looper
import android.view.View
import com.modukey.app.core.*

/** Captured handle input drives values. Only held native volume needs a periodic UI request. */
internal class SliderDragSession(private val view: View, initial: Float, startY: Float, travelPx: Float,
    private val write: (Float) -> SliderApplied?, private val keepOpen: (() -> Boolean)? = null, private val failed: () -> Unit) {
    private val slider = RelativeSlider(initial, startY, travelPx)
    private var lastInputY = startY
    private var active = true
    private var pending: Float? = null
    private val handler = Handler(Looper.getMainLooper())
    private val frame: Runnable = Runnable { frames.onFrame() }
    private val frames: FrameCoalescer = FrameCoalescer({ view.postOnAnimation(frame) }, { view.removeCallbacks(frame) }, ::flush)
    private val heartbeat = object : Runnable {
        override fun run() {
            if (!active) return
            if (keepOpen?.invoke() != true) abort() else handler.postDelayed(this, 1_000)
        }
    }
    fun start() { if (active && keepOpen != null) handler.postDelayed(heartbeat, 1_000) }
    fun move(y: Float) {
        if (!active || !y.isFinite() || y == lastInputY) return
        lastInputY = y; pending = slider.move(y); frames.request()
    }
    private fun flush() {
        val value = pending ?: return
        pending = null
        if (!active) return
        val actual = write(value)
        if (actual == null) abort() else slider.accept(actual)
    }
    private fun abort() { if (active) { finish(true); failed() } }
    fun finish(cancelled: Boolean): Boolean {
        if (!active) return false
        frames.stop(); handler.removeCallbacks(heartbeat)
        if (!cancelled) flush()
        val completed = active && !cancelled
        active = false; pending = null
        return completed
    }
}
