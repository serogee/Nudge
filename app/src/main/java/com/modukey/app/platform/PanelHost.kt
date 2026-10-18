package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.*
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.modukey.app.core.*
import com.modukey.app.ui.NudgeTheme
import com.modukey.app.ui.PanelContent
import kotlin.math.roundToInt

data class PanelSpec(val panel: PanelConfig, val layout: PanelLayout, val geometry: PanelGeometryResult, val editing: Boolean, val edge: Edge = Edge.RIGHT)
interface PanelHost { fun show(spec: PanelSpec); fun hide(animate: Boolean = true); fun refreshSound() }

/** At most one active window and one inert window completing its finite exit. */
class AccessibilityPanelHost(private val context: Context, private val manager: WindowManager, private val actions: ActionExecutor,
    private val onDismiss: () -> Unit, private val handleAt: (Float, Float) -> Boolean, private val onAction: (PanelButton) -> Unit,
    private val onTouchState: (Boolean) -> Unit = {}, private val onError: (RuntimeException) -> Unit = {}) : PanelHost {
    private var active: AttachedPanel? = null
    private var closing: AttachedPanel? = null
    private var registered = false
    private var observing = false
    private var backDispatcher: android.window.OnBackInvokedDispatcher? = null
    private val back = if (Build.VERSION.SDK_INT >= 33) android.window.OnBackInvokedCallback { onDismiss() } else null
    private val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { refreshSound() } }
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { refreshSound() } }

    override fun show(spec: PanelSpec) {
        val window = active
        if (window == null) attach(spec)
        else {
            window.spec = spec
            val p = params(spec)
            val previous = window.layout
            if (previous.x != p.x || previous.y != p.y || previous.width != p.width || previous.height != p.height || previous.flags != p.flags) {
                manager.updateViewLayout(window.root, p); window.layout = p
            }
            configureInput(window)
        }
    }
    private fun attach(spec: PanelSpec) {
        val window = AttachedPanel(spec)
        active = window
        try {
            manager.addView(window.root, window.layout); window.added = true; window.owners.start(); configureInput(window)
            PanelWindowAnimations.enter(window.root, spec)
        } catch (e: RuntimeException) {
            active = null; stopObservers()
            try { window.dispose() } catch (cleanup: RuntimeException) { e.addSuppressed(cleanup) }
            throw e
        }
    }
    private inner class AttachedPanel(initial: PanelSpec) {
        var spec by mutableStateOf(initial)
        var sound by mutableStateOf(SoundState())
        var layout = params(initial)
        val owners = Owners()
        var exiting = false
        var added = false
        private var disposed = false
        val root = PanelRoot(context) { (spec.editing || exiting) to spec.panel.outsideTouch }.apply {
            dismiss = { if (active === this@AttachedPanel) onDismiss() }
            isHandle = handleAt
            touchState = { if (active === this@AttachedPanel) onTouchState(it) }
            setViewTreeLifecycleOwner(owners); setViewTreeSavedStateRegistryOwner(owners)
        }
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            // These states belong to this window. Clearing or replacing active does not blank an exit frame.
            setContent { NudgeTheme {
                val p = spec
                PanelContent(p.panel, p.layout, p.geometry, sound, !p.editing) { if (active === this@AttachedPanel) onAction(it) }
            } }
        }
        private val detached = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) { complete(this@AttachedPanel, detached = true) }
        }
        init {
            root.addView(compose, FrameLayout.LayoutParams(-1, -1))
            root.addOnAttachStateChangeListener(detached)
        }
        fun disableInput() {
            exiting = true
            root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            layout = WindowManager.LayoutParams().apply {
                copyFrom(this@AttachedPanel.layout)
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            manager.updateViewLayout(root, layout)
        }
        fun dispose(removeWindow: Boolean = true) {
            if (disposed) return
            disposed = true
            val registered = added || root.isAttachedToWindow
            added = false
            root.removeOnAttachStateChangeListener(detached)
            var failure: RuntimeException? = null
            fun cleanup(block: () -> Unit) { try { block() } catch (e: RuntimeException) { if (failure == null) failure = e } }
            cleanup { PanelWindowAnimations.cancel(root) }
            cleanup { if (removeWindow && registered) manager.removeViewImmediate(root) }
            cleanup { compose.disposeComposition() }
            cleanup { owners.stop() }
            failure?.let { throw it }
        }
    }
    private fun configureInput(window: AttachedPanel) {
        if (!window.spec.editing) {
            refreshSound()
            if (!registered) {
                ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
                    addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                    addAction(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                    addAction(android.app.NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
                }, ContextCompat.RECEIVER_NOT_EXPORTED)
                registered = true
            }
            if (!observing) { context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer); observing = true }
            if (Build.VERSION.SDK_INT >= 33 && backDispatcher == null) {
                backDispatcher = window.root.findOnBackInvokedDispatcher()
                back?.let { backDispatcher?.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, it) }
            }
        } else stopObservers()
    }
    override fun refreshSound() { active?.takeIf { !it.spec.editing }?.let { it.sound = actions.snapshot() } }
    @SuppressLint("RtlHardcoded")
    private fun params(spec: PanelSpec) = WindowManager.LayoutParams(spec.geometry.bounds.width.roundToInt().coerceAtLeast(1),
        spec.geometry.bounds.height.roundToInt().coerceAtLeast(1), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (spec.editing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
            (if (spec.panel.outsideTouch != OutsideTouch.BLOCK) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH else 0),
        PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.LEFT; x = spec.geometry.bounds.x.roundToInt(); y = spec.geometry.bounds.y.roundToInt()
        setFitInsetsTypes(0); layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        title = "Nudge panel"
        windowAnimations = 0
    }
    private fun stopObservers() {
        if (registered) { registered = false; try { context.unregisterReceiver(receiver) } catch (_: RuntimeException) { } }
        if (observing) { observing = false; try { context.contentResolver.unregisterContentObserver(observer) } catch (_: RuntimeException) { } }
        if (Build.VERSION.SDK_INT >= 33) { try { back?.let { backDispatcher?.unregisterOnBackInvokedCallback(it) } } catch (_: RuntimeException) { }; backDispatcher = null }
    }
    private fun complete(window: AttachedPanel, detached: Boolean = false) {
        if (active === window) { active = null; stopObservers() }
        if (closing === window) closing = null
        try { window.dispose(removeWindow = !detached) } catch (e: RuntimeException) { onError(e) }
    }
    override fun hide(animate: Boolean) {
        val window = active
        if (window == null && animate) return
        active = null; stopObservers()
        val previous = closing; closing = null
        var failure: RuntimeException? = null
        try { previous?.dispose() } catch (e: RuntimeException) { failure = e }
        if (window != null) {
            if (animate && window.root.isAttachedToWindow && PanelWindowAnimations.enabled(window.spec, closing = true)) {
                closing = window
                try {
                    window.disableInput()
                    PanelWindowAnimations.exit(window.root, window.spec) { complete(window) }
                } catch (e: RuntimeException) { complete(window); if (failure == null) failure = e }
            } else try { window.dispose() } catch (e: RuntimeException) { if (failure == null) failure = e }
        }
        failure?.let { throw it }
    }
    private class Owners : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
        init { saved.performAttach(); saved.performRestore(null); registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE) }
        fun start() { registry.handleLifecycleEvent(Lifecycle.Event.ON_START); registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        fun stop() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}

internal class PanelRoot(context: Context, private val policy: () -> Pair<Boolean, OutsideTouch>?) : FrameLayout(context) {
    constructor(context: Context) : this(context, { false to OutsideTouch.BLOCK })
    init { isClickable = true }
    var dismiss: () -> Unit = {}
    var isHandle: (Float, Float) -> Boolean = { _, _ -> false }
    var touchState: (Boolean) -> Unit = {}
    private var outside = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val p = policy() ?: return false
        if (p.first) return false
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            if (p.second == OutsideTouch.PASS_THROUGH && !isHandle(event.rawX, event.rawY)) dismiss()
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> touchState(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touchState(false)
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN) outside = event.x < 0 || event.x >= width || event.y < 0 || event.y >= height
        if (outside) {
            if (event.actionMasked == MotionEvent.ACTION_UP) { outside = false; dismiss() }
            else if (event.actionMasked == MotionEvent.ACTION_CANCEL) outside = false
            return true
        }
        return super.dispatchTouchEvent(event)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (policy()?.first == false && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) dismiss()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
