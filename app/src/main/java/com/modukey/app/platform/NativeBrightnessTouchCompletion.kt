package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import com.modukey.app.core.Bounds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** A relative native slider keeps its progress during a stationary press and commits it on release. */
internal class NativeBrightnessTouchCompletion(service: AccessibilityService,
    private val dispatch: (GestureDescription, AccessibilityService.GestureResultCallback) -> Boolean = { gesture, callback ->
        service.dispatchGesture(gesture, callback, Handler(Looper.getMainLooper()))
    }) {
    var active = false; private set

    suspend fun release(control: NativeBrightnessControl, screen: Bounds): Boolean {
        val point = completionPoint(control, screen) ?: return false
        // Always wait for this finite touch to release before cancellation restores brightness.
        active = true
        return try {
            withContext(NonCancellable) {
                withTimeoutOrNull(1_000) {
                    suspendCancellableCoroutine { continuation ->
                        val path = Path().apply { moveTo(point.first, point.second) }
                        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
                        val callback = object : AccessibilityService.GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(true) }
                            override fun onCancelled(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(false) }
                        }
                        val accepted = runCatching { dispatch(gesture, callback) }.getOrDefault(false)
                        if (!accepted && continuation.isActive) continuation.resume(false)
                    }
                } ?: false
            }
        } finally { active = false }
    }

    companion object {
        data class WindowRegion(val id: Int, val layer: Int, val bounds: Bounds)
        fun unobscured(point: Pair<Float, Float>, windowId: Int, windows: List<WindowRegion>): Boolean {
            if (windowId < 0) return false
            val target = windows.singleOrNull { it.id == windowId } ?: return false
            fun contains(bounds: Bounds) = point.first >= bounds.x && point.second >= bounds.y &&
                point.first < bounds.x + bounds.width && point.second < bounds.y + bounds.height
            return contains(target.bounds) && windows.none { it.id != windowId && it.layer > target.layer && contains(it.bounds) }
        }
        fun completionPoint(control: NativeBrightnessControl, screen: Bounds): Pair<Float, Float>? {
            if (!control.requiresTouch) return null
            val b = control.touchBounds ?: return null
            if (!listOf(b.x, b.y, b.width, b.height).all { it.isFinite() } || b.width < 8f || b.height < 8f) return null
            if (b.x < screen.x || b.y < screen.y || b.x + b.width > screen.x + screen.width || b.y + b.height > screen.y + screen.height) return null
            return b.x + b.width / 2f to b.y + b.height / 2f
        }
    }
}
