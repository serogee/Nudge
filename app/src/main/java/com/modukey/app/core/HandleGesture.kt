package com.modukey.app.core

import kotlin.math.abs
import kotlin.math.hypot

/** Release-only recognition. No delayed callback or idle timer is needed. */
class HandleGesture(private val edge: Edge, private val distance: Float, private val timeout: Long, private val slop: Float,
    private val holdTime: Long = 500, private val swipeHoldTime: Long = 500) {
    private var active = false
    private var cancelled = false
    private var x = 0f; private var y = 0f; private var start = 0L
    private var stableX = 0f; private var stableY = 0f; private var stableAt = 0L
    private var crossed = false
    private var moved = false
    fun down(x: Float, y: Float, time: Long) {
        active = true; cancelled = false; crossed = false; moved = false
        this.x = x; this.y = y; start = time; stableX = x; stableY = y; stableAt = time
    }
    fun cancel() { active = false; cancelled = true }
    fun move(x: Float, y: Float, time: Long) {
        if (!active) return
        val dx = x - this.x; val dy = y - this.y
        if (hypot(dx, dy) > slop) moved = true
        val (inward, parallel) = components(dx, dy)
        if (moved && (inward < -slop || (abs(parallel) > slop && abs(parallel) > inward))) cancelled = true
        if (!crossed && inward >= distance && inward > abs(parallel) && time - start in 0..timeout) {
            crossed = true; stableX = x; stableY = y; stableAt = time
        } else if (crossed && hypot(x - stableX, y - stableY) > slop) {
            stableX = x; stableY = y; stableAt = time
        }
    }
    fun up(x: Float, y: Float, time: Long): GestureKind? {
        if (!active) return null
        move(x, y, time); active = false
        if (cancelled || time < start) return null
        val (inward, parallel) = components(x - this.x, y - this.y)
        if (!moved) return when { time - start <= 250 -> GestureKind.TAP; time - start >= holdTime -> GestureKind.HOLD; else -> null }
        if (!crossed || inward < distance || inward <= abs(parallel)) return null
        return when { time - stableAt >= swipeHoldTime -> GestureKind.SWIPE_HOLD; time - start <= timeout -> GestureKind.SWIPE; else -> null }
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
