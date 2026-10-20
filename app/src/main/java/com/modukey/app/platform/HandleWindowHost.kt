package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.ViewTreeObserver
import com.modukey.app.core.*
import kotlin.math.roundToInt

internal data class HandleWindowSpec(val bounds: Bounds, val settings: TriggerSettings, val edge: Edge, val editing: Boolean, val previewQuarterTurns: Int = 0,
    val hideIndicator: Boolean = false)

/** Own the registered window even before View receives its attachment callback. */
internal class HandleWindowHost(private val context: Context, private val manager: WindowManager,
    private val activate: (GestureKind) -> Unit, private val reveal: () -> Unit,
    private val interactionEnded: () -> Unit = {},
    private val cleanupError: (RuntimeException) -> Unit = {},
    private val changed: (HandleTriggerView?) -> Unit) : OverlayHost<HandleWindowSpec> {
    private var view: HandleTriggerView? = null
    private var layout: WindowManager.LayoutParams? = null
    private var added = false
    private var retiring: HandleTriggerView? = null
    private var cancelHandoff: (() -> Unit)? = null
    override fun attach(value: HandleWindowSpec) {
        check(view == null)
        val next = HandleTriggerView(context, activate, reveal, interactionEnded)
        next.configure(value.settings, value.edge, value.editing, value.previewQuarterTurns, value.hideIndicator)
        view = next; changed(next)
        try {
            val p = params(value)
            manager.addView(next, p); added = true; layout = p
        } catch (e: RuntimeException) {
            try { detach() } catch (cleanup: RuntimeException) { e.addSuppressed(cleanup) }
            throw e
        }
    }
    override fun update(value: HandleWindowSpec) {
        val current = requireNotNull(view)
        val p = params(value); val previous = layout
        current.configure(value.settings, value.edge, value.editing, value.previewQuarterTurns, value.hideIndicator)
        if (previous?.x != p.x || previous.y != p.y || previous.width != p.width || previous.height != p.height || previous.flags != p.flags) {
            current.cancelGesture(); manager.updateViewLayout(current, p); layout = p
        }
    }
    override fun raise(value: HandleWindowSpec) {
        val previous = view ?: return attach(value)
        val next = HandleTriggerView(context, activate, reveal, interactionEnded)
        next.configure(value.settings, value.edge, value.editing, value.previewQuarterTurns, value.hideIndicator)
        next.inheritVisibility(previous)
        val p = params(value)
        try { manager.addView(next, p) }
        catch (error: RuntimeException) {
            runCatching { manager.removeViewImmediate(next) }
            throw error
        }
        cancelHandoff?.invoke(); cancelHandoff = null
        view = next; layout = p; added = true; changed(next)
        previous.cancelGesture()
        // Keep one drawn bar until the new window has produced a frame.
        if (!previous.isAttachedToWindow) {
            manager.removeViewImmediate(previous)
            if (retiring == null) return
        } else if (retiring == null) retiring = previous else manager.removeViewImmediate(previous)
        val observer = next.viewTreeObserver
        var scheduled = false
        val finish = Runnable {
            cancelHandoff?.invoke(); cancelHandoff = null
            val old = retiring; retiring = null
            if (old != null) try { manager.removeViewImmediate(old) } catch (error: RuntimeException) { cleanupError(error) }
        }
        val drawn = ViewTreeObserver.OnDrawListener {
            if (!scheduled) { scheduled = true; next.postOnAnimation(finish) }
        }
        observer.addOnDrawListener(drawn)
        cancelHandoff = {
            next.removeCallbacks(finish)
            val currentObserver = next.viewTreeObserver
            if (currentObserver.isAlive) currentObserver.removeOnDrawListener(drawn)
        }
    }
    override fun detach() {
        cancelHandoff?.invoke(); cancelHandoff = null
        val old = retiring; retiring = null
        if (old != null) try { manager.removeViewImmediate(old) } catch (error: RuntimeException) { cleanupError(error) }
        val current = view ?: return
        val registered = added || current.isAttachedToWindow || current.parent != null
        view = null; layout = null; added = false
        current.cancelGesture(); changed(null)
        if (registered) manager.removeViewImmediate(current)
    }
    @SuppressLint("RtlHardcoded")
    private fun params(spec: HandleWindowSpec) = WindowManager.LayoutParams(spec.bounds.width.roundToInt().coerceAtLeast(1), spec.bounds.height.roundToInt().coerceAtLeast(1),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or (if (spec.editing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0), PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.LEFT; x = spec.bounds.x.roundToInt(); y = spec.bounds.y.roundToInt()
        setFitInsetsTypes(0); layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS; title = "Nudge handle"
        windowAnimations = 0
    }
}
