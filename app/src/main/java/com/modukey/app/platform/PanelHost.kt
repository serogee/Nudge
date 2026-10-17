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

data class PanelSpec(val panel: PanelConfig, val layout: PanelLayout, val geometry: PanelGeometryResult, val editing: Boolean)
interface PanelHost { fun show(spec: PanelSpec); fun hide(); fun refreshSound() }

/** Owns its composition and event observers only while an overlay is attached. */
class AccessibilityPanelHost(private val context: Context, private val manager: WindowManager, private val actions: ActionExecutor,
    private val onDismiss: () -> Unit, private val handleAt: (Float, Float) -> Boolean, private val onAction: (PanelButton) -> Unit) : PanelHost {
    private var root: PanelRoot? = null
    private var composition: ComposeView? = null
    private var owner: Owners? = null
    private var last: WindowManager.LayoutParams? = null
    private var current by mutableStateOf<PanelSpec?>(null)
    private var sound by mutableStateOf(SoundState())
    private var registered = false
    private var observing = false
    private var backDispatcher: android.window.OnBackInvokedDispatcher? = null
    private val back = if (Build.VERSION.SDK_INT >= 33) android.window.OnBackInvokedCallback { onDismiss() } else null
    private val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { refreshSound() } }
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { refreshSound() } }
    override fun show(spec: PanelSpec) {
        current = spec
        if (root == null) attach(spec)
        else {
            val p = params(spec)
            val previous = last
            if (previous?.x != p.x || previous.y != p.y || previous.width != p.width || previous.height != p.height || previous.flags != p.flags) {
                manager.updateViewLayout(root, p); last = p
            }
            configureInput(spec)
        }
    }
    private fun attach(spec: PanelSpec) {
        val owners = Owners()
        val container = PanelRoot(context) { current?.let { it.editing to it.panel.outsideTouch } }.apply {
            dismiss = onDismiss; isHandle = handleAt
            setViewTreeLifecycleOwner(owners); setViewTreeSavedStateRegistryOwner(owners)
        }
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { NudgeTheme { current?.let { p -> PanelContent(p.panel, p.layout, p.geometry, sound, !p.editing, onAction) } } }
        }
        container.addView(compose, FrameLayout.LayoutParams(-1, -1))
        root = container; composition = compose; owner = owners
        val p = params(spec)
        try { manager.addView(container, p); last = p; owners.start(); configureInput(spec) }
        catch (e: RuntimeException) { hide(); throw e }
    }
    private fun configureInput(spec: PanelSpec) {
        if (!spec.editing) {
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
                backDispatcher = root?.findOnBackInvokedDispatcher()
                back?.let { backDispatcher?.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, it) }
            }
        } else stopObservers()
    }
    override fun refreshSound() { if (current?.editing == false) sound = actions.snapshot() }
    @SuppressLint("RtlHardcoded")
    private fun params(spec: PanelSpec) = WindowManager.LayoutParams(spec.geometry.bounds.width.roundToInt().coerceAtLeast(1),
        spec.geometry.bounds.height.roundToInt().coerceAtLeast(1), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (spec.editing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
            (if (spec.panel.outsideTouch == OutsideTouch.PASS_THROUGH) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH else 0),
        PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.LEFT; x = spec.geometry.bounds.x.roundToInt(); y = spec.geometry.bounds.y.roundToInt()
        setFitInsetsTypes(0); layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        title = "Nudge panel"
    }
    private fun stopObservers() {
        if (registered) { registered = false; try { context.unregisterReceiver(receiver) } catch (_: RuntimeException) { } }
        if (observing) { observing = false; try { context.contentResolver.unregisterContentObserver(observer) } catch (_: RuntimeException) { } }
        if (Build.VERSION.SDK_INT >= 33) { try { back?.let { backDispatcher?.unregisterOnBackInvokedCallback(it) } } catch (_: RuntimeException) { }; backDispatcher = null }
    }
    override fun hide() {
        val view = root; val compose = composition; val owners = owner
        root = null; last = null; composition = null; owner = null; current = null
        stopObservers()
        var failure: RuntimeException? = null
        fun cleanup(block: () -> Unit) { try { block() } catch (e: RuntimeException) { if (failure == null) failure = e } }
        cleanup { compose?.disposeComposition() }
        cleanup { owners?.stop() }
        cleanup { if (view?.isAttachedToWindow == true) manager.removeViewImmediate(view) }
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
    var dismiss: () -> Unit = {}
    var isHandle: (Float, Float) -> Boolean = { _, _ -> false }
    private var outside = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val p = policy() ?: return false
        if (p.first) return false
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            if (p.second == OutsideTouch.PASS_THROUGH && !isHandle(event.rawX, event.rawY)) dismiss()
            return false
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
