package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.graphics.*
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.SeekBar
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

/** One bounded window. Observers and callbacks exist only while this window is visible. */
internal class BrightnessPopupHost(private val context: Context, private val manager: WindowManager,
    private val brightness: BrightnessController, private val dismissed: () -> Unit, private val failure: (Int) -> Unit) {
    private var view: BrightnessPopupView? = null
    private var layout: WindowManager.LayoutParams? = null
    private var preview = false
    private var observing = false
    private var timeoutMs = 3_000L
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { dismissed() }
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { view?.updateLevel(brightness.fraction()) }
    }
    val visible get() = view != null
    fun show(settings: BrightnessPopupSettings, screen: ScreenSpace, editing: Boolean): Boolean {
        if (view != null && preview != editing) hide()
        preview = editing; timeoutMs = settings.timeoutMs
        val b = BrightnessPopupGeometry.calculate(screen.usable, screen.density, settings)
        val p = params(b, editing)
        try {
            val current = view
            if (current == null) {
                val next = BrightnessPopupView(context, { fraction ->
                    val result = brightness.setFraction(fraction)
                    if (result == BrightnessResult.FAILED || result == BrightnessResult.NEED_ACCESS) {
                        dismissed(); failure(if (result == BrightnessResult.NEED_ACCESS) R.string.brightness_access_required else R.string.brightness_failed)
                    }
                }, { active -> handler.removeCallbacks(timeout); if (!active && view != null && !preview) handler.postDelayed(timeout, timeoutMs) }, dismissed)
                view = next
                next.configure(settings, editing, brightness.fraction())
                manager.addView(next, p); layout = p
                if (!editing) {
                    context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, observer)
                    context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, observer)
                    observing = true
                    restartTimeout()
                }
            } else {
                current.configure(settings, editing, brightness.fraction())
                val before = layout
                if (before?.x != p.x || before.y != p.y || before.width != p.width || before.height != p.height) {
                    current.cancelInput(); manager.updateViewLayout(current, p); layout = p
                }
            }
            return true
        } catch (_: RuntimeException) { hide(); failure(R.string.overlay_error); return false }
    }
    fun restartTimeout() { handler.removeCallbacks(timeout); if (!preview && view != null) handler.postDelayed(timeout, timeoutMs) }
    fun hide() {
        handler.removeCallbacks(timeout)
        // Unregister even if setup failed between the two observer registrations.
        if (observing || view != null) runCatching { context.contentResolver.unregisterContentObserver(observer) }
        observing = false
        val previous = view; view = null; layout = null
        previous?.cancelInput()
        if (previous != null) runCatching { manager.removeViewImmediate(previous) }
    }
    @SuppressLint("RtlHardcoded")
    private fun params(b: Bounds, editing: Boolean) = WindowManager.LayoutParams(b.width.roundToInt().coerceAtLeast(1), b.height.roundToInt().coerceAtLeast(1),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            (if (editing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0), PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.LEFT; x = b.x.roundToInt(); y = b.y.roundToInt(); setFitInsetsTypes(0)
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        title = context.getString(R.string.brightness_slider)
    }
}

/** Input and drawing use the same inner track. Writes are coalesced to one per display frame. */
@SuppressLint("ViewConstructor") // Created by the service with input callbacks; never inflated from XML.
internal class BrightnessPopupView(context: Context, private val change: (Float) -> Unit,
    private val touching: (Boolean) -> Unit, private val dismiss: () -> Unit) : View(context) {
    private var settings = BrightnessPopupSettings()
    private var preview = false
    private var level = .5f
    private var dragging = false
    private var cancelled = false
    private var pending: Float? = null
    private var scheduled = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val track = RectF()
    private val clip = Path()
    private val frame = Runnable { scheduled = false; pending?.let { pending = null; change(it) } }
    private var backDispatcher: android.window.OnBackInvokedDispatcher? = null
    private val back = if (Build.VERSION.SDK_INT >= 33) android.window.OnBackInvokedCallback { dismiss() } else null
    init { isFocusableInTouchMode = true; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES; contentDescription = context.getString(R.string.brightness_slider) }
    fun configure(s: BrightnessPopupSettings, editing: Boolean, fraction: Float) {
        val dirty = settings != s || preview != editing || (!dragging && level != fraction)
        settings = s; preview = editing; isEnabled = !editing
        if (!dragging) level = fraction
        if (dirty) invalidate()
    }
    fun updateLevel(fraction: Float) { if (!dragging && level != fraction) { level = fraction; invalidate() } }
    private fun trackBounds() = track.apply { val inset = width * .1f; set(inset, inset, width - inset, height - inset) }
    override fun onDraw(canvas: Canvas) {
        val s = settings
        fun color(value: Long, opacity: Float) { paint.color = value.toInt(); paint.alpha = (opacity * 255).roundToInt() }
        color(s.background, s.backgroundOpacity); canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), width / 2f, width / 2f, paint)
        val t = trackBounds(); val radius = t.width() / 2f
        color(s.track, s.trackOpacity); canvas.drawRoundRect(t, radius, radius, paint)
        clip.rewind(); clip.addRoundRect(t, radius, radius, Path.Direction.CW)
        val save = canvas.save(); canvas.clipPath(clip)
        color(s.fill, s.fillOpacity); canvas.drawRect(t.left, t.bottom - level.coerceIn(0f, 1f) * t.height(), t.right, t.bottom, paint); canvas.restoreToCount(save)
        color(s.icon, s.iconOpacity); paint.style = Paint.Style.STROKE; paint.strokeWidth = width * .028f; paint.strokeCap = Paint.Cap.ROUND
        val cx = width / 2f; val cy = height - width * .48f; val r = width * .08f
        canvas.drawCircle(cx, cy, r, paint)
        for (i in 0..7) { val a = i * Math.PI / 4; val x = kotlin.math.cos(a).toFloat(); val y = kotlin.math.sin(a).toFloat()
            canvas.drawLine(cx + x * r * 1.6f, cy + y * r * 1.6f, cx + x * r * 2.1f, cy + y * r * 2.1f, paint) }
        paint.style = Paint.Style.FILL
    }
    private fun move(y: Float) {
        val t = trackBounds(); level = ((t.bottom - y) / t.height().coerceAtLeast(1f)).coerceIn(0f, 1f)
        pending = level; invalidate()
        if (!scheduled) { scheduled = true; postOnAnimation(frame) }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (preview) return false
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) { dismiss(); return true }
        if (event.pointerCount > 1) { cancelled = true; cancelInput(); return true }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { cancelled = false; dragging = true; touching(true); move(event.y) }
            MotionEvent.ACTION_MOVE -> if (dragging && !cancelled) move(event.y)
            MotionEvent.ACTION_UP -> if (dragging && !cancelled) { move(event.y); removeCallbacks(frame); scheduled = false; val last = pending; pending = null; dragging = false; last?.let(change); touching(false); performClick() }
            MotionEvent.ACTION_CANCEL -> { cancelled = true; cancelInput() }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    fun cancelInput() { val active = dragging; dragging = false; pending = null; scheduled = false; removeCallbacks(frame); if (active) touching(false) }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = if (keyCode == KeyEvent.KEYCODE_BACK) { dismiss(); true } else super.onKeyUp(keyCode, event)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!preview && Build.VERSION.SDK_INT >= 33) { backDispatcher = findOnBackInvokedDispatcher(); backDispatcher?.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, requireNotNull(back)) }
    }
    override fun onDetachedFromWindow() {
        cancelInput()
        if (Build.VERSION.SDK_INT >= 33) backDispatcher?.unregisterOnBackInvokedCallback(requireNotNull(back))
        backDispatcher = null; super.onDetachedFromWindow()
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info); info.className = SeekBar::class.java.name
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 1f, level)
        if (!preview) { info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS); info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD); info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD) }
    }
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (preview) return false
        val value = when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> level + .05f
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> level - .05f
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id -> arguments?.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE) ?: return false
            else -> return super.performAccessibilityAction(action, arguments)
        }
        if (!value.isFinite()) return false
        touching(true); level = value.coerceIn(0f, 1f); change(level); touching(false); invalidate(); return true
    }
}
