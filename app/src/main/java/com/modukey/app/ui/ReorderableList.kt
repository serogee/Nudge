package com.modukey.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.modukey.app.R
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

/** Only the left grip starts a drag. Autoscroll schedules frames only during a drag. */
@Composable fun <T> ReorderableList(items: List<T>, id: (T) -> String, label: (T) -> String, modifier: Modifier = Modifier,
    moveTo: (String, Int) -> Unit, finish: () -> Unit, content: @Composable RowScope.(T, Int) -> Unit) {
    val state = rememberLazyListState()
    val currentItems by rememberUpdatedState(items)
    val currentId by rememberUpdatedState(id)
    val currentMove by rememberUpdatedState(moveTo)
    val currentFinish by rememberUpdatedState(finish)
    var dragging by remember { mutableStateOf<String?>(null) }
    var center by remember { mutableFloatStateOf(0f) }
    var finger by remember { mutableFloatStateOf(0f) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val grips = remember { mutableMapOf<String, Rect>() }
    val context = LocalContext.current
    val margin = with(LocalDensity.current) { 64.dp.toPx() }
    fun reorder() {
        val key = dragging ?: return
        val from = currentItems.indexOfFirst { currentId(it) == key }
        val entries = state.layoutInfo.visibleItemsInfo
        val selected = entries.firstOrNull { it.key == key }
        // Wait for the last move to reach layout before requesting another move.
        if (selected != null && selected.index != from) return
        val target = if (selected == null) entries.minByOrNull { abs(center - it.offset - it.size / 2f) }?.index
        else if (center > selected.offset + selected.size / 2f)
            entries.lastOrNull { it.index > from && center >= it.offset + it.size / 2f - .5f }?.index
        else entries.firstOrNull { it.index < from && center <= it.offset + it.size / 2f + .5f }?.index
        if (target == null) return
        if (from >= 0 && target != from) currentMove(key, target)
    }
    val speed by remember { derivedStateOf {
        if (dragging == null) 0f else {
            val info = state.layoutInfo
            val zone = minOf(margin, (info.viewportEndOffset - info.viewportStartOffset) / 3f).coerceAtLeast(1f)
            when {
                finger < info.viewportStartOffset + zone -> -((info.viewportStartOffset + zone - finger) / zone).coerceIn(0f, 1f)
                finger > info.viewportEndOffset - zone -> ((finger - info.viewportEndOffset + zone) / zone).coerceIn(0f, 1f)
                else -> 0f
            } * margin * 7f
        }
    } }
    LaunchedEffect(dragging, speed != 0f) {
        if (dragging == null || speed == 0f) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive && dragging != null && speed != 0f) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(.05f); last = now
            state.scrollBy(speed * dt); reorder()
        }
    }
    // This input node remains attached when a row moves or leaves the viewport.
    val input = Modifier.onGloballyPositioned { coordinates = it }.pointerInput(Unit) {
        fun finishDrag() { if (dragging != null) { dragging = null; currentFinish() } }
        try { awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val point = coordinates?.takeIf { it.isAttached }?.localToRoot(down.position) ?: return@awaitEachGesture
            val key = grips.entries.firstOrNull { it.value.contains(point) }?.key ?: return@awaitEachGesture
            val entry = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return@awaitEachGesture
            val grabOffset = entry.offset + entry.size / 2f - down.position.y
            down.consume()
            var cancelled = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (event.changes.size > 1 || change == null || currentItems.none { currentId(it) == key }) cancelled = true
                event.changes.forEach { it.consume() }
                if (cancelled) finishDrag()
                else if (change != null && change.pressed) {
                    if (dragging == null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragging = key
                    if (dragging != null) { finger = change.position.y; center = finger + grabOffset; reorder() }
                }
            } while (event.changes.any { it.pressed })
            finishDrag()
        } } finally { finishDrag() }
    }
    LazyColumn(modifier.then(input), state = state, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        itemsIndexed(items, key = { _, item -> id(item) }) { index, item ->
            val key = id(item); val active = key == dragging
            DisposableEffect(key) { onDispose { grips.remove(key) } }
            Row(Modifier.fillMaxWidth().zIndex(if (active) 1f else 0f)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val entry = if (active) state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } else null
                        val offset = if (entry != null) center - entry.offset - entry.size / 2f else 0f
                        placeable.placeRelative(0, offset.roundToInt())
                    }
                },
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(40.dp).heightIn(min = 64.dp).onGloballyPositioned { grips[key] = it.boundsInRoot() }.semantics {
                    contentDescription = context.getString(R.string.reorder_item, label(item))
                    customActions = buildList {
                        if (index > 0) add(CustomAccessibilityAction(context.getString(R.string.move_up)) { currentMove(key, index - 1); currentFinish(); true })
                        if (index < items.lastIndex) add(CustomAccessibilityAction(context.getString(R.string.move_down)) { currentMove(key, index + 1); currentFinish(); true })
                    }
                }, contentAlignment = Alignment.Center) { Icon(AppIcons.Drag, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                content(item, index)
            }
        }
    }
}
