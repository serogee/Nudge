package com.modukey.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Leave moves available to scrolling; reject the release of a drag before clickable sees it. */
internal fun Modifier.rejectDraggedClick(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var canceled = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pointer = event.changes.firstOrNull { it.id == down.id }
            if (event.changes.size > 1 || pointer == null) canceled = true
            pointer?.let {
                if ((it.position - down.position).getDistance() > viewConfiguration.touchSlop ||
                    it.position.x < 0 || it.position.y < 0 || it.position.x >= size.width || it.position.y >= size.height) canceled = true
            }
            if (canceled) event.changes.filter { !it.pressed }.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}
