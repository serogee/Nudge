package com.modukey.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.abs
import kotlin.math.roundToInt

/** Touch changes start at a thumb and use travel relative to the press, never the press position. */
@Composable internal fun DragSlider(values: List<Float>, range: ClosedFloatingPointRange<Float>, names: List<String>,
    enabled: Boolean = true, limits: (Int) -> ClosedFloatingPointRange<Float> = { range },
    finish: () -> Unit, change: (Int, Float) -> Unit) {
    val latestValues by rememberUpdatedState(values)
    val latestChange by rememberUpdatedState(change)
    val latestFinish by rememberUpdatedState(finish)
    val latestLimits by rememberUpdatedState(limits)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val hitRadius = with(LocalDensity.current) { 24.dp.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = if (rtl) -1f else 1f
    val span = range.endInclusive - range.start
    fun x(value: Float): Float {
        val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
        return hitRadius + (size.width - hitRadius * 2f).coerceAtLeast(1f) * if (rtl) 1f - fraction else fraction
    }
    fun adjust(index: Int, requested: Float): Boolean {
        if (!enabled || !requested.isFinite()) return false
        val bounded = requested.coerceIn(latestLimits(index))
        if (bounded == latestValues[index]) return false
        latestChange(index, bounded)
        latestFinish()
        return true
    }
    fun Modifier.accessible(index: Int) = semantics {
        contentDescription = names[index]
        progressBarRangeInfo = ProgressBarRangeInfo(values[index], limits(index))
        if (!enabled) disabled()
        setProgress { adjust(index, it) }
    }.onKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) false else {
            val value = latestValues[index]
            val step = span / 100f
            when (event.key) {
                Key.DirectionLeft -> { adjust(index, value - step * direction); true }
                Key.DirectionRight -> { adjust(index, value + step * direction); true }
                Key.DirectionDown -> { adjust(index, value - step); true }
                Key.DirectionUp -> { adjust(index, value + step); true }
                Key.MoveHome -> { adjust(index, latestLimits(index).start); true }
                Key.MoveEnd -> { adjust(index, latestLimits(index).endInclusive); true }
                else -> false
            }
        }
    }.focusable(enabled)
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .38f)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (enabled) 1f else .38f)
    Box(Modifier.fillMaxWidth().height(48.dp).onSizeChanged { size = it }
        .then(if (values.size == 1) Modifier.accessible(0) else Modifier)
        .pointerInput(enabled, range, rtl) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                val index = latestValues.indices.minByOrNull { abs(down.position.x - x(latestValues[it])) } ?: return@awaitEachGesture
                if (abs(down.position.x - x(latestValues[index])) > hitRadius ||
                    abs(down.position.y - size.height / 2f) > hitRadius) return@awaitEachGesture
                var current = latestValues[index]
                var previousX = down.position.x
                val travel = (size.width - hitRadius * 2f).coerceAtLeast(1f)
                var changed = false
                fun move(position: Offset) {
                    val value = (current + (position.x - previousX) / travel * span * direction).coerceIn(latestLimits(index))
                    previousX = position.x
                    current = value
                    if (value != latestValues[index]) { latestChange(index, value); changed = true }
                }
                val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { event, _ -> event.consume() }
                if (drag != null) {
                    try {
                        move(drag.position)
                        horizontalDrag(drag.id) { event -> move(event.position); event.consume() }
                    } finally { if (changed) latestFinish() }
                }
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = this.size.height / 2f
            val first = Offset(hitRadius, centerY)
            val last = Offset(this.size.width - hitRadius, centerY)
            drawLine(track, first, last, 8.dp.toPx(), StrokeCap.Round)
            val start = if (values.size == 1) x(range.start) else x(values.first())
            drawLine(primary, Offset(start, centerY), Offset(x(values.last()), centerY), 8.dp.toPx(), StrokeCap.Round)
            values.forEach { drawCircle(primary, 10.dp.toPx(), Offset(x(it), centerY)) }
        }
        if (values.size > 1) values.forEachIndexed { index, value ->
            Spacer(Modifier.offset { IntOffset((x(value) - hitRadius).roundToInt(), 0) }.size(48.dp).accessible(index))
        }
    }
}
