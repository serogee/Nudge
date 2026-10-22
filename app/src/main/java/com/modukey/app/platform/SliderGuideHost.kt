package com.modukey.app.platform

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.modukey.app.R
import com.modukey.app.core.*

/** A noninteractive, full-size distance reference that exists only in the handle editor. */
internal class SliderGuideHost(private val context: Context, private val manager: WindowManager) {
    private var view: GuideView? = null
    @SuppressLint("RtlHardcoded") // Overlay coordinates refer to physical display edges.
    fun show(request: SliderGuideRequest, handle: HandleConfig, screen: ScreenSpace, fraction: Float) {
        val next = view ?: GuideView(context).also {
            view = it
            try {
                manager.addView(it, WindowManager.LayoutParams(screen.width.toInt(), screen.height.toInt(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
                    gravity = Gravity.TOP or Gravity.LEFT; setFitInsetsTypes(0); title = context.getString(R.string.slider_travel)
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                })
            } catch (e: RuntimeException) { hide(); throw e }
        }
        val b = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, handle.trigger, screen.landscape)
        next.update(request, b, screen, fraction)
    }
    fun hide() { val previous = view; view = null; if (previous != null) runCatching { manager.removeViewImmediate(previous) } }
    private class GuideView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var top = 0f; private var bottom = 0f; private var anchor = 0f; private var x = 0f
        private var safeTop = 0f; private var safeBottom = 0f
        private var title = ""
        private val zero = context.getString(R.string.percent, 0)
        private val full = context.getString(R.string.percent, 100)
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; setWillNotDraw(false) }
        fun update(request: SliderGuideRequest, handle: Bounds, screen: ScreenSpace, fraction: Float) {
            val travel = request.travelDp * screen.density
            anchor = handle.y + handle.height / 2
            top = anchor - (1f - fraction.coerceIn(0f, 1f)) * travel
            bottom = top + travel
            x = if (handle.x + handle.width / 2 < screen.width / 2) screen.usable.x + 48 * screen.density
                else screen.usable.x + screen.usable.width - 100 * screen.density
            safeTop = screen.usable.y + 24 * screen.density
            safeBottom = screen.usable.y + screen.usable.height - 24 * screen.density
            title = context.getString(if (request.kind == SliderKind.VOLUME) R.string.volume_travel else R.string.brightness_travel)
            invalidate()
        }
        override fun onDraw(canvas: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = Color.WHITE; paint.strokeWidth = 3 * d; paint.textSize = 12 * d
            paint.setShadowLayer(3 * d, 0f, 0f, Color.BLACK)
            val a = top.coerceIn(safeTop, safeBottom); val b = bottom.coerceIn(safeTop, safeBottom)
            canvas.drawLine(x, a, x, b, paint)
            canvas.drawLine(x - 8*d, a, x + 8*d, a, paint)
            canvas.drawLine(x - 8*d, b, x + 8*d, b, paint)
            canvas.drawText(if (top < safeTop) "↑ $full" else full, x + 14*d, a + 4*d, paint)
            canvas.drawText(if (bottom > safeBottom) "↓ $zero" else zero, x + 14*d, b + 4*d, paint)
            paint.color = 0xFF80CBC4.toInt()
            canvas.drawCircle(x, anchor.coerceIn(safeTop, safeBottom), 6*d, paint)
            canvas.drawText(title, (x - 80*d).coerceAtLeast(8*d), (anchor - 16*d).coerceIn(safeTop, safeBottom), paint)
        }
    }
}
