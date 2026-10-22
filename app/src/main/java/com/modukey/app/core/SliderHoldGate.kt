package com.modukey.app.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

/** A stationary dwell followed by a one-time dead zone excludes the opening swipe. */
internal class SliderHoldGate(startX: Float, startY: Float, now: Long, private val holdMs: Long, private val slopPx: Float) {
    init { require(startX.isFinite() && startY.isFinite() && holdMs > 0 && slopPx.isFinite() && slopPx > 0) }
    private var stableX = startX
    private var stableY = startY
    private var stableSince = now
    private var latestY = startY
    private var offset: Float? = null
    var armed = false; private set
    var armedY = startY; private set
    val deadline: Long? get() = if (armed) null else stableSince + holdMs

    fun move(x: Float, y: Float, now: Long): Float? {
        if (!x.isFinite() || !y.isFinite()) return null
        latestY = y
        if (!armed) {
            if (hypot(x - stableX, y - stableY) > slopPx) {
                stableX = x; stableY = y; stableSince = now
            }
            poll(now)
            return null
        }
        if (offset == null) {
            val delta = y - armedY
            if (abs(delta) <= slopPx) return null
            offset = -sign(delta) * slopPx
        }
        return y + requireNotNull(offset)
    }

    fun poll(now: Long): Boolean {
        if (armed || now < requireNotNull(deadline)) return false
        armed = true; armedY = latestY
        return true
    }
}
