package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.modukey.app.core.*

class EdgeTriggerView(context: Context, private val onActivate: () -> Unit) : View(context) {
    private var settings = TriggerSettings()
    private var edge = Edge.RIGHT
    private var editing = false
    private var gesture = recognizer()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorRect = RectF()
    private val highlightPath = Path()
    private var indicatorRadius = 0f

    constructor(context: Context) : this(context, {})

    init {
        contentDescription = "Nudge volume shortcut"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
        setWillNotDraw(false)
    }

    fun configure(settings: TriggerSettings, edge: Edge, editing: Boolean) {
        val edgeChanged = this.edge != edge
        val gestureChanged = this.edge != edge || this.editing != editing ||
            this.settings.activation != settings.activation || this.settings.swipeDistanceDp != settings.swipeDistanceDp ||
            this.settings.swipeTimeoutMs != settings.swipeTimeoutMs
        this.settings = settings
        this.edge = edge
        if (edgeChanged) updateHighlight()
        this.editing = editing
        if (gestureChanged) gesture = recognizer()
        isClickable = !editing
        importantForAccessibility = if (editing) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_YES
        updateIndicator()
        invalidate()
        updateExclusion()
    }

    private fun recognizer() = TriggerGesture(
        edge, settings.activation, settings.swipeDistanceDp * resources.displayMetrics.density,
        settings.swipeTimeoutMs, ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { updateExclusion(); updateIndicator(); updateHighlight() }

    private fun updateHighlight() { EdgeHighlightShape.path(width.toFloat(), height.toFloat(), edge, highlightPath) }

    private fun updateIndicator() {
        val bounds = TriggerGeometry.indicator(width.toFloat(), height.toFloat(), settings, edge, resources.displayMetrics.density)
        indicatorRect.set(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height)
        indicatorRadius = (settings.cornerRadiusDp * resources.displayMetrics.density).coerceAtMost(minOf(bounds.width, bounds.height) / 2)
    }

    private fun updateExclusion() {
        systemGestureExclusionRects = if (editing || edge == Edge.BOTTOM) emptyList() else listOf(Rect(0, 0, width, height))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (editing) {
            paint.color = 0x554E7E21
            canvas.drawPath(highlightPath, paint)
        }
        if (!settings.indicatorVisible) return
        paint.color = settings.indicatorColor.toInt()
        paint.alpha = (settings.indicatorOpacity * 255).toInt()
        canvas.drawRoundRect(indicatorRect, indicatorRadius, indicatorRadius, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (editing) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> gesture.down(event.rawX, event.rawY, event.eventTime)
            MotionEvent.ACTION_POINTER_DOWN -> gesture.cancel()
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount != 1) gesture.cancel()
                gesture.move(event.rawX, event.rawY)
            }
            MotionEvent.ACTION_CANCEL -> gesture.cancel()
            MotionEvent.ACTION_UP -> if (gesture.up(event.rawX, event.rawY, event.eventTime)) performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        if (editing) return false
        super.performClick()
        onActivate()
        return true
    }

    fun cancelGesture() { gesture.cancel() }
}
