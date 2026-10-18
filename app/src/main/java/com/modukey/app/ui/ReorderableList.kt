package com.modukey.app.ui

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.layout.layout
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
    val currentMove by rememberUpdatedState(moveTo)
    val currentFinish by rememberUpdatedState(finish)
    var dragging by remember { mutableStateOf<String?>(null) }
    var center by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val margin = with(LocalDensity.current) { 64.dp.toPx() }
    fun reorder() {
        val key = dragging ?: return
        val from = currentItems.indexOfFirst { id(it) == key }
        val target = state.layoutInfo.visibleItemsInfo.minByOrNull { abs(center - it.offset - it.size / 2f) }?.index ?: return
        if (from >= 0 && target != from) currentMove(key, target)
    }
    LaunchedEffect(dragging) {
        if (dragging == null) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive && dragging != null) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(.05f); last = now
            val info = state.layoutInfo
            val direction = when {
                center < info.viewportStartOffset + margin -> -1
                center > info.viewportEndOffset - margin -> 1
                else -> 0
            }
            if (direction != 0) { state.scrollBy(direction * margin * 5f * dt); reorder() }
        }
    }
    LazyColumn(modifier, state = state, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        itemsIndexed(items, key = { _, item -> id(item) }) { index, item ->
            val key = id(item); val active = key == dragging
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
                Box(Modifier.width(40.dp).heightIn(min = 64.dp).semantics {
                    contentDescription = context.getString(R.string.reorder_item, label(item))
                    customActions = buildList {
                        if (index > 0) add(CustomAccessibilityAction(context.getString(R.string.move_up)) { currentMove(key, index - 1); currentFinish(); true })
                        if (index < items.lastIndex) add(CustomAccessibilityAction(context.getString(R.string.move_down)) { currentMove(key, index + 1); currentFinish(); true })
                    }
                }.pointerInput(key) {
                    detectDragGestures(onDragStart = {
                        val entry = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
                        if (entry != null) { center = entry.offset + entry.size / 2f; dragging = key }
                    }, onDragEnd = { dragging = null; currentFinish() }, onDragCancel = { dragging = null; currentFinish() }) { change, delta ->
                        change.consume(); center += delta.y; reorder()
                    }
                }, contentAlignment = Alignment.Center) { Icon(AppIcons.Drag, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                content(item, index)
            }
        }
    }
}
