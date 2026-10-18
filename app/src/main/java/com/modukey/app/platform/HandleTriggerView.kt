package com.modukey.app.platform

import android.animation.ValueAnimator
import android.os.SystemClock
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.*
import com.modukey.app.R
import com.modukey.app.core.*

/** Small physical input window. Appearance changes do not cancel a captured gesture. */
class HandleTriggerView(context: Context, private val activate: (GestureKind) -> Unit, private val onReveal: () -> Unit = {}) : View(context) {
    constructor(context: Context) : this(context, {})
    private var settings = TriggerSettings()
    private var edge = Edge.RIGHT
    private var editing = false
    private var gesture = recognizer()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guide = Path()
    private val indicator = RectF()
    private var radius = 0f
    private val softHide = SoftHideSession()
    private var fullScreen: Boolean? = null
    private var panelOpen = false
    private var scheduledAt: Long? = null
    private var bounce = 0f
    private var animator: ValueAnimator? = null
    private val expire = Runnable { scheduledAt = null; softHide.expire(SystemClock.uptimeMillis()); scheduleExpiry(); invalidate() }

    init {
        contentDescription = context.getString(R.string.handle_description); isClickable = true; setWillNotDraw(false)
        setOnApplyWindowInsetsListener { _, insets ->
            if (!panelOpen) {
                fullScreen = !insets.isVisible(WindowInsets.Type.statusBars())
                updateSoftHide(); invalidate()
            }
            insets
        }
    }
    fun configure(settings: TriggerSettings, edge: Edge, editing: Boolean) {
        if (this.edge != edge || this.editing != editing || this.settings.swipeDistanceDp != settings.swipeDistanceDp || this.settings.swipeTimeoutMs != settings.swipeTimeoutMs ||
            this.settings.holdTimeMs != settings.holdTimeMs || this.settings.swipeHoldTimeMs != settings.swipeHoldTimeMs) {
            cancelGesture(); this.edge = edge; this.settings = settings; gesture = recognizer()
        }
        this.settings = settings; this.edge = edge; this.editing = editing
        importantForAccessibility = if (editing) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = !editing; geometry(); updateSoftHide(); invalidate()
    }
    private fun recognizer() = HandleGesture(edge, settings.swipeDistanceDp * resources.displayMetrics.density, settings.swipeTimeoutMs,
        ViewConfiguration.get(context).scaledTouchSlop.toFloat(), settings.holdTimeMs, settings.swipeHoldTimeMs)
    fun cancelGesture() { gesture.cancel(); softHide.touch(false, SystemClock.uptimeMillis()); scheduleExpiry() }
    fun setPanelOpen(open: Boolean) {
        if (panelOpen == open) return
        panelOpen = open; softHide.panel(open, SystemClock.uptimeMillis()); scheduleExpiry()
        if (!open) requestApplyInsets()
    }
    fun resetSoftHide() { softHide.reset(); animator?.cancel(); bounce = 0f; scheduleExpiry(); invalidate() }
    private fun updateSoftHide() {
        softHide.configure(!editing && settings.softHideEnabled, fullScreen, settings.softHideDelayMs, SystemClock.uptimeMillis())
        if (!softHide.active) { animator?.cancel(); animator = null; bounce = 0f }
        scheduleExpiry()
    }
    private fun scheduleExpiry() {
        val next = softHide.deadline
        if (next == scheduledAt) return
        removeCallbacks(expire); scheduledAt = next
        next?.let { postDelayed(expire, (it - SystemClock.uptimeMillis()).coerceAtLeast(0)) }
    }
    private fun completeGesture(kind: GestureKind) {
        if (softHide.gesture(SystemClock.uptimeMillis())) {
            onReveal()
            if (ValueAnimator.areAnimatorsEnabled()) {
                animator?.cancel()
                animator = ValueAnimator.ofFloat(0f, 1f, -.15f, 0f).apply {
                    duration = 220
                    addUpdateListener { bounce = it.animatedValue as Float; invalidate() }
                    start()
                }
            }
            invalidate()
        } else activate(kind)
        scheduleExpiry()
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); requestApplyInsets() }
    override fun onDetachedFromWindow() {
        cancelGesture(); removeCallbacks(expire); scheduledAt = null; animator?.cancel(); animator = null; bounce = 0f
        super.onDetachedFromWindow()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { cancelGesture(); geometry() }
    private fun geometry() {
        EdgeHighlightShape.path(width.toFloat(), height.toFloat(), edge, guide)
        val b = TriggerGeometry.indicator(width.toFloat(), height.toFloat(), settings, edge)
        indicator.set(b.x, b.y, b.x + b.width, b.y + b.height)
        radius = (settings.cornerRadiusDp * resources.displayMetrics.density).coerceAtMost(minOf(b.width, b.height) / 2)
        systemGestureExclusionRects = if (editing || edge == Edge.BOTTOM) emptyList() else listOf(Rect(0, 0, width, height))
    }
    override fun onDraw(canvas: Canvas) {
        if (editing) { paint.color = 0x554E7E21; canvas.drawPath(guide, paint) }
        if (if (softHide.active) softHide.revealed else settings.indicatorVisible) {
            paint.color = settings.indicatorColor.toInt(); paint.alpha = (settings.indicatorOpacity * 255).toInt()
            val travel = bounce * 4 * resources.displayMetrics.density
            canvas.save()
            canvas.translate(if (edge == Edge.LEFT) travel else if (edge == Edge.RIGHT) -travel else 0f, if (edge == Edge.BOTTOM) -travel else 0f)
            canvas.drawRoundRect(indicator, radius, radius, paint); canvas.restore()
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (editing) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { softHide.touch(true, SystemClock.uptimeMillis()); scheduleExpiry(); gesture.down(event.rawX, event.rawY, event.eventTime) }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> cancelGesture()
            MotionEvent.ACTION_MOVE -> if (event.pointerCount == 1) gesture.move(event.rawX, event.rawY, event.eventTime) else gesture.cancel()
            MotionEvent.ACTION_UP -> { softHide.touch(false, SystemClock.uptimeMillis()); gesture.up(event.rawX, event.rawY, event.eventTime)?.let(::completeGesture); scheduleExpiry() }
        }
        return true
    }
    override fun performClick(): Boolean {
        if (editing) return false
        super.performClick(); completeGesture(GestureKind.TAP); return true
    }
}
