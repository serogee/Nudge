package com.modukey.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class HandleGesturePolicy(val swipeAtDistance: Boolean = false, val holdAtDeadline: Boolean = false,
    val swipeHoldAtDeadline: Boolean = false, val swipeHoldEnabled: Boolean = true)
enum class SliderKind { VOLUME, BRIGHTNESS }
data class SliderApplied(val fraction: Float, val tolerance: Float)

/** Incremental clamping makes reversal respond immediately after an overshoot. */
class RelativeSlider(initial: Float, startY: Float, private val travel: Float) {
    init { require(initial.isFinite() && startY.isFinite() && travel.isFinite() && travel > 0f) }
    private var lastY = startY
    var fraction = initial.coerceIn(0f, 1f); private set
    fun move(y: Float): Float {
        if (!y.isFinite()) return fraction
        fraction = (fraction + (lastY - y) / travel).coerceIn(0f, 1f)
        lastY = y
        return fraction
    }
    fun accept(actual: SliderApplied) {
        if (actual.fraction.isFinite() && abs(actual.fraction - fraction) > actual.tolerance.coerceAtLeast(0f) + .000001f)
            fraction = actual.fraction.coerceIn(0f, 1f)
    }
}

data class SliderGuideRequest(val handleId: String, val kind: SliderKind, val travelDp: Float)
class SliderGuidePreview {
    private var owner: Any? = null
    private val mutable = MutableStateFlow<SliderGuideRequest?>(null)
    val state = mutable.asStateFlow()
    fun begin(token: Any, request: SliderGuideRequest) { owner = token; mutable.value = request }
    fun end(token: Any) { if (owner === token) { owner = null; mutable.value = null } }
}
