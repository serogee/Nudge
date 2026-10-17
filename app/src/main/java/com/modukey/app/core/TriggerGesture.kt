package com.modukey.app.core

import kotlin.math.abs
import kotlin.math.hypot

/** Coordinates are screen coordinates: the volume window may appear after this view's UP. */
class TriggerGesture(
    private val edge: Edge,
    private val activation: Activation,
    private val distancePx: Float,
    private val timeoutMs: Long,
    private val slopPx: Float,
) {
    private var started = false
    private var cancelled = false
    private var x = 0f
    private var y = 0f
    private var time = 0L

    fun down(x: Float, y: Float, time: Long) {
        started = true
        cancelled = false
        this.x = x
        this.y = y
        this.time = time
    }

    fun cancel() { cancelled = true }

    fun move(x: Float, y: Float) {
        if (!started) return
        val dx = x - this.x
        val dy = y - this.y
        if (activation != Activation.SWIPE) {
            if (hypot(dx, dy) > slopPx) cancelled = true
        } else {
            val (inward, parallel) = components(dx, dy)
            if (inward < -slopPx || (abs(parallel) > slopPx && abs(parallel) > inward)) cancelled = true
        }
    }

    fun up(x: Float, y: Float, time: Long): Boolean {
        if (!started) return false
        move(x, y)
        started = false
        if (cancelled || time < this.time) return false
        val duration = time - this.time
        val (inward, parallel) = components(x - this.x, y - this.y)
        return when (activation) {
            Activation.SWIPE -> duration <= timeoutMs && inward >= distancePx && inward > abs(parallel)
            Activation.TAP -> duration <= 250
            Activation.LONG_PRESS -> duration >= 500
        }
    }

    private fun components(dx: Float, dy: Float): Pair<Float, Float> = when (edge) {
        Edge.LEFT -> dx to dy
        Edge.RIGHT -> -dx to dy
        Edge.BOTTOM -> -dy to dx
    }
}
