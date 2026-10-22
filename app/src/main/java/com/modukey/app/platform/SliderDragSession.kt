package com.modukey.app.platform

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import com.modukey.app.core.*

/** Captured handle input drives values. Only held native volume needs a periodic UI request. */
internal class SliderDragSession(private val view: View, initial: Float, startY: Float, travelPx: Float,
    private val write: (Float) -> SliderApplied?, private val keepOpen: (() -> Boolean)? = null, private val failed: () -> Unit,
    private val startX: Float = 0f, holdMs: Long = 0, slopPx: Float = 8f,
    private val readOnArm: () -> Float? = { initial }, private val onArmed: () -> Unit = {}) {
    private val travel = travelPx
    private val gate = if (holdMs > 0) SliderHoldGate(startX, startY, SystemClock.uptimeMillis(), holdMs, slopPx) else null
    private var slider = if (gate == null) RelativeSlider(initial, startY, travelPx) else null
    private var lastInputY = startY
    private var active = true
    private var pending: Float? = null
    private val handler = Handler(Looper.getMainLooper())
    private val frame: Runnable = Runnable { frames.onFrame() }
    private val frames: FrameCoalescer = FrameCoalescer({ view.postOnAnimation(frame) }, { view.removeCallbacks(frame) }, ::flush)
    private val armDeadline = Runnable {
        if (active && gate?.poll(SystemClock.uptimeMillis()) == true) arm()
    }
    private val heartbeat = object : Runnable {
        override fun run() {
            if (!active) return
            if (keepOpen?.invoke() != true) abort() else handler.postDelayed(this, 1_000)
        }
    }
    fun start() {
        if (!active) return
        if (keepOpen != null) handler.postDelayed(heartbeat, 1_000)
        scheduleArm()
    }
    fun move(y: Float) = move(startX, y)
    fun move(x: Float, y: Float, allowActivation: Boolean = true) {
        if (!active || !x.isFinite() || !y.isFinite()) return
        // A release can finish an armed drag, but cannot activate a waiting drag.
        if (!allowActivation && slider == null) return
        val input = if (gate == null) y else gate.move(x, y, SystemClock.uptimeMillis()).also {
            if (gate.armed && slider == null) arm() else if (!gate.armed) scheduleArm()
        }
        if (!active || input == null || input == lastInputY) return
        lastInputY = input; pending = requireNotNull(slider).move(input); frames.request()
    }
    private fun scheduleArm() {
        handler.removeCallbacks(armDeadline)
        if (active) gate?.deadline?.let { handler.postAtTime(armDeadline, it) }
    }
    private fun arm() {
        handler.removeCallbacks(armDeadline)
        val current = readOnArm()?.takeIf { it.isFinite() }
        if (current == null) { abort(); return }
        lastInputY = requireNotNull(gate).armedY
        slider = RelativeSlider(current, lastInputY, travel)
        onArmed()
    }
    private fun flush() {
        val value = pending ?: return
        pending = null
        if (!active) return
        val actual = write(value)
        if (actual == null) abort() else slider?.accept(actual)
    }
    private fun abort() { if (active) { finish(true); failed() } }
    fun finish(cancelled: Boolean): Boolean {
        if (!active) return false
        frames.stop(); handler.removeCallbacks(heartbeat); handler.removeCallbacks(armDeadline)
        if (!cancelled) flush()
        val completed = active && !cancelled
        active = false; pending = null
        return completed
    }
}
