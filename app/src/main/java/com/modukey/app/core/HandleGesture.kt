package com.modukey.app.core

import kotlin.math.abs
import kotlin.math.hypot

/** One result per touch. Only immediate hold modes need a bounded deadline callback. */
class HandleGesture(private val edge: Edge, private val distance: Float, private val timeout: Long, private val slop: Float,
    private val holdTime: Long = 500, private val swipeHoldTime: Long = 500,
    private val holdDistance: Float = distance, private val holdSwipeTimeout: Long = timeout,
    private val holdImmediately: Boolean = false, private val swipeHoldImmediately: Boolean = false) {
    private var active = false
    private var cancelled = false
    var emitted = false; private set
    private var x = 0f; private var y = 0f; private var start = 0L
    private var stableX = 0f; private var stableY = 0f; private var stableAt = 0L
    private var crossed = false; private var holdCrossed = false; private var holdReady = false
    private var moved = false; private var inward = 0f; private var parallel = 0f
    val pullDistance get() = if (active && !cancelled && moved) inward.coerceAtLeast(0f) else 0f
    val prefersHold get() = holdCrossed
    fun down(x: Float, y: Float, time: Long) {
        active = true; cancelled = false; emitted = false; crossed = false; holdCrossed = false; holdReady = false; moved = false
        this.x = x; this.y = y; start = time; stableX = x; stableY = y; stableAt = time; inward = 0f; parallel = 0f
    }
    fun cancel() { active = false; cancelled = true }
    fun move(x: Float, y: Float, time: Long) = update(x, y, time, releasing = false)
    private fun update(x: Float, y: Float, time: Long, releasing: Boolean) {
        if (!active || cancelled || time < start) return
        // Latch a completed dwell before processing release jitter or a later move.
        if (holdCrossed && time - stableAt >= swipeHoldTime) holdReady = true
        val dx = x - this.x; val dy = y - this.y
        if (hypot(dx, dy) > slop) moved = true
        val c = components(dx, dy); inward = c.first; parallel = c.second
        val parallelLimit = maxOf(slop * 2f, minOf(distance, holdDistance))
        if (moved && (inward < -slop || (abs(parallel) > parallelLimit && abs(parallel) > inward.coerceAtLeast(0f) * 1.5f))) cancelled = true
        if (cancelled) return
        if (!crossed && inward >= distance && inward > abs(parallel) && time - start <= timeout) crossed = true
        if (!holdCrossed && inward >= holdDistance && inward > abs(parallel) && time - start <= holdSwipeTimeout) {
            holdCrossed = true; stableX = x; stableY = y; stableAt = time
        } else if (holdCrossed && !holdReady && !releasing && hypot(x - stableX, y - stableY) > slop * 2f) {
            stableX = x; stableY = y; stableAt = time
        }
    }
    fun nextDeadline(): Long? = when {
        !active || cancelled || emitted -> null
        !moved && holdImmediately -> start + holdTime
        holdCrossed && swipeHoldImmediately -> stableAt + swipeHoldTime
        else -> null
    }
    fun poll(time: Long): GestureKind? {
        if (!active || cancelled || emitted || time < start) return null
        val kind = when {
            !moved && holdImmediately && time - start >= holdTime -> GestureKind.HOLD
            validHold() && swipeHoldImmediately && time - stableAt >= swipeHoldTime -> GestureKind.SWIPE_HOLD
            else -> null
        }
        if (kind != null) emitted = true
        return kind
    }
    private fun validHold() = holdCrossed && inward + slop >= holdDistance && inward > abs(parallel)
    fun up(x: Float, y: Float, time: Long): GestureKind? {
        if (!active) return null
        update(x, y, time, releasing = true); active = false
        if (cancelled || emitted || time < start) return null
        val kind = if (!moved) when { time - start <= 250 -> GestureKind.TAP; time - start >= holdTime -> GestureKind.HOLD; else -> null }
        else when {
            validHold() && (holdReady || time - stableAt >= swipeHoldTime) -> GestureKind.SWIPE_HOLD
            crossed && inward >= distance && inward > abs(parallel) && time - start <= timeout -> GestureKind.SWIPE
            else -> null
        }
        emitted = kind != null
        return kind
    }
    private fun components(dx: Float, dy: Float) = when (edge) { Edge.LEFT -> dx to dy; Edge.RIGHT -> -dx to dy; Edge.BOTTOM -> -dy to dx }
}

/** Actions without a panel have an expiry; an owned panel has an explicit lifetime. */
class GestureSessions {
    private data class Session(val stage: Int, val expires: Long?)
    private val sessions = mutableMapOf<String, Session>()
    fun reset(id: String) { sessions.remove(id) }
    fun clear() = sessions.clear()
    fun retain(ids: Set<String>) { sessions.keys.retainAll(ids) }
    fun next(handle: HandleConfig, kind: GestureKind, now: Long): Pair<GestureTarget, Int?> {
        if (kind != GestureKind.SWIPE) {
            reset(handle.id)
            return handle.target(when (kind) { GestureKind.TAP -> GestureSlot.TAP; GestureKind.HOLD -> GestureSlot.HOLD; else -> GestureSlot.SWIPE_HOLD }) to null
        }
        val old = sessions[handle.id]?.takeIf { it.expires == null || now < it.expires }
        val start = old?.stage?.plus(1) ?: 0
        for (offset in 0..2) {
            val stage = (start + offset) % 3
            val target = handle.target(GestureSlot.entries[stage])
            if (!target.isNone) return target to stage
        }
        reset(handle.id)
        return GestureTarget.NONE to null
    }
    fun submitted(handle: HandleConfig, stage: Int?, target: GestureTarget, now: Long) {
        if (stage == null) { reset(handle.id); return }
        sessions[handle.id] = Session(stage, if (target.panelId == null) now + handle.volumeSequenceMs else null)
    }
}
