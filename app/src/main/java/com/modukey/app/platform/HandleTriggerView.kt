package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.*
import com.modukey.app.R
import com.modukey.app.core.*

/** Small physical input window. Appearance changes do not cancel a captured gesture. */
class HandleTriggerView(context: Context, private val activate: (GestureKind) -> Unit) : View(context) {
    constructor(context: Context) : this(context, {})
    private var settings = TriggerSettings()
    private var edge = Edge.RIGHT
    private var editing = false
    private var gesture = recognizer()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guide = Path()
    private val indicator = RectF()
    private var radius = 0f
    init { contentDescription = context.getString(R.string.handle_description); isClickable = true; setWillNotDraw(false) }
    fun configure(settings: TriggerSettings, edge: Edge, editing: Boolean) {
        if (this.edge != edge || this.editing != editing || this.settings.swipeDistanceDp != settings.swipeDistanceDp || this.settings.swipeTimeoutMs != settings.swipeTimeoutMs) {
            cancelGesture(); this.edge = edge; this.settings = settings; gesture = recognizer()
        }
        this.settings = settings; this.edge = edge; this.editing = editing
        importantForAccessibility = if (editing) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = !editing; geometry(); invalidate()
    }
    private fun recognizer() = HandleGesture(edge, settings.swipeDistanceDp * resources.displayMetrics.density, settings.swipeTimeoutMs, ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    fun cancelGesture() = gesture.cancel()
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
        if (settings.indicatorVisible) { paint.color = settings.indicatorColor.toInt(); paint.alpha = (settings.indicatorOpacity * 255).toInt(); canvas.drawRoundRect(indicator, radius, radius, paint) }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (editing) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> gesture.down(event.rawX, event.rawY, event.eventTime)
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> gesture.cancel()
            MotionEvent.ACTION_MOVE -> if (event.pointerCount == 1) gesture.move(event.rawX, event.rawY, event.eventTime) else gesture.cancel()
            MotionEvent.ACTION_UP -> gesture.up(event.rawX, event.rawY, event.eventTime)?.let(activate)
        }
        return true
    }
    override fun performClick(): Boolean {
        if (editing) return false
        super.performClick(); activate(GestureKind.TAP); return true
    }
}
