package com.modukey.app.core

import kotlin.math.abs
import kotlin.math.hypot

/** One result per touch. Only immediate hold modes need a bounded deadline callback. */
class HandleGesture(private val edge: Edge, private val distance: Float, private val timeout: Long, private val slop: Float,
    private val holdTime: Long = 500, private val swipeHoldTime: Long = 500,
    private val holdDistance: Float = distance, private val holdSwipeTimeout: Long = timeout,
    private val holdImmediately: Boolean = false, private val swipeHoldImmediately: Boolean = false,
    private val holdDistanceMode: SwipeHoldDistanceMode = SwipeHoldDistanceMode.ANY_DIRECTION,
    private val swipeImmediately: Boolean = false, private val swipeHoldEnabled: Boolean = true,
    private val swipeDistanceMode: SwipeDistanceMode = SwipeDistanceMode.INWARD_ONLY,
    private val enabledGestures: Set<GestureKind> = GestureKind.entries.toSet()) {
    private var active = false
    private var cancelled = false
    var emitted = false; private set
    private var x = 0f; private var y = 0f; private var start = 0L
    private var stableAt = 0L
    private var holdQualified = false; private var swipeCancelled = false; private var radial = 0f
    private var crossed = false; private var holdCrossed = false; private var holdReady = false
    private var moved = false; private var inward = 0f; private var parallel = 0f
    private fun holdMeasure() = if (holdDistanceMode == SwipeHoldDistanceMode.ANY_DIRECTION) radial else inward
    private fun validSwipeDistance() = if (swipeDistanceMode == SwipeDistanceMode.ANY_DIRECTION) radial >= distance
        else inward >= distance && inward > abs(parallel)
    val pullDistance get() = if (active && !cancelled && moved &&
        (GestureKind.SWIPE in enabledGestures || GestureKind.SWIPE_HOLD in enabledGestures))
        maxOf(holdMeasure(), if (swipeDistanceMode == SwipeDistanceMode.ANY_DIRECTION) radial else inward).coerceAtLeast(0f) else 0f
    fun down(x: Float, y: Float, time: Long) {
        active = true; cancelled = false; emitted = false; crossed = false; holdCrossed = false; holdReady = false; moved = false
        this.x = x; this.y = y; start = time; stableAt = time; inward = 0f; parallel = 0f; radial = 0f
        holdQualified = false; swipeCancelled = false
    }
    fun cancel() { active = false; cancelled = true }
    fun move(x: Float, y: Float, time: Long) = update(x, y, time)
    private fun update(x: Float, y: Float, time: Long) {
        if (!active || cancelled || time < start) return
        val dx = x - this.x; val dy = y - this.y
        radial = hypot(dx, dy)
        if (radial > slop) moved = true
        val c = components(dx, dy); inward = c.first; parallel = c.second
        val parallelLimit = maxOf(slop * 2f, minOf(distance, holdDistance))
        if (swipeDistanceMode == SwipeDistanceMode.INWARD_ONLY && moved &&
            (inward < -slop || (abs(parallel) > parallelLimit && abs(parallel) > inward.coerceAtLeast(0f) * 1.5f))) swipeCancelled = true
        if (!swipeCancelled && !crossed && validSwipeDistance() && time - start <= timeout) crossed = true
        val measure = holdMeasure()
        if (!holdQualified && measure >= holdDistance && time - start <= holdSwipeTimeout) holdQualified = true
        if (holdQualified) {
            if (holdCrossed && measure + slop < holdDistance) { holdCrossed = false; holdReady = false }
            if (!holdCrossed && measure >= holdDistance) { holdCrossed = true; stableAt = time }
            holdReady = holdCrossed && time - stableAt >= swipeHoldTime
        }
    }
    fun nextDeadline(): Long? = when {
        !active || cancelled || emitted -> null
        !moved && holdImmediately && GestureKind.HOLD in enabledGestures -> start + holdTime
        swipeHoldEnabled && holdCrossed && swipeHoldImmediately && GestureKind.SWIPE_HOLD in enabledGestures -> stableAt + swipeHoldTime
        else -> null
    }
    fun poll(time: Long): GestureKind? {
        if (!active || cancelled || emitted || time < start) return null
        val kind = when {
            GestureKind.SWIPE in enabledGestures && swipeImmediately && crossed && !swipeCancelled && validSwipeDistance() -> GestureKind.SWIPE
            GestureKind.HOLD in enabledGestures && !moved && holdImmediately && time - start >= holdTime -> GestureKind.HOLD
            GestureKind.SWIPE_HOLD in enabledGestures && swipeHoldEnabled && validHold() && swipeHoldImmediately && time - stableAt >= swipeHoldTime -> GestureKind.SWIPE_HOLD
            else -> null
        }
        if (kind != null) emitted = true
        return kind
    }
    private fun validHold() = holdCrossed && holdMeasure() + slop >= holdDistance
    fun up(x: Float, y: Float, time: Long): GestureKind? {
        if (!active) return null
        update(x, y, time); active = false
        if (cancelled || emitted || time < start) return null
        val kind = (if (!moved) when { time - start <= 250 -> GestureKind.TAP; time - start >= holdTime -> GestureKind.HOLD; else -> null }
        else when {
            swipeHoldEnabled && validHold() && (holdReady || time - stableAt >= swipeHoldTime) -> GestureKind.SWIPE_HOLD
            !swipeCancelled && crossed && validSwipeDistance() && time - start <= timeout -> GestureKind.SWIPE
            else -> null
        })?.takeIf { it in enabledGestures }
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
    fun keepWhileTouch(id: String) { sessions[id]?.let { sessions[id] = it.copy(expires = null) } }
    fun clear() = sessions.clear()
    fun retain(ids: Set<String>) { sessions.keys.retainAll(ids) }
    fun next(handle: HandleConfig, kind: GestureKind, now: Long): Pair<GestureTarget, Int?> {
        if (kind != GestureKind.SWIPE) {
            val target = handle.runtimeTarget(when (kind) { GestureKind.TAP -> GestureSlot.TAP; GestureKind.HOLD -> GestureSlot.HOLD; else -> GestureSlot.SWIPE_HOLD })
            if (!target.isNone) reset(handle.id)
            return target to null
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
