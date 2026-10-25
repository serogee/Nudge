package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.modukey.app.R
import com.modukey.app.core.*

/** A temporary reference above the preview Wheel. It never owns input or accessibility focus. */
internal class WheelHitboxPreviewHost(private val context: Context, private val manager: WindowManager) {
    private var view: HitboxView? = null
    @SuppressLint("RtlHardcoded")
    fun show(geometry: WheelGeometryResult, radii: Pair<Float, Float>, screen: ScreenSpace, preview: PreviewGeometry?) {
        val next = view ?: HitboxView(context).also {
            view = it
            try {
                manager.addView(it, WindowManager.LayoutParams(screen.width.toInt(), screen.height.toInt(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
                    gravity = Gravity.TOP or Gravity.LEFT; setFitInsetsTypes(0)
                    title = context.getString(R.string.wheel_hitbox_preview)
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                })
            } catch (e: RuntimeException) { hide(); throw e }
        }
        val params = next.layoutParams as WindowManager.LayoutParams
        if (params.width != screen.width.toInt() || params.height != screen.height.toInt()) {
            params.width = screen.width.toInt(); params.height = screen.height.toInt()
            manager.updateViewLayout(next, params)
        }
        next.update(geometry.hitboxPoints(radii), screen.density, preview)
    }
    fun hide() {
        val previous = view; view = null
        if (previous != null) runCatching { manager.removeViewImmediate(previous) }
    }
    private class HitboxView(context: Context) : View(context) {
        private val path = Path()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val color = context.getColor(android.R.color.system_accent1_400)
        private var density = 1f
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; setWillNotDraw(false) }
        fun update(points: List<WheelPoint>, density: Float, preview: PreviewGeometry?) {
            this.density = density
            path.rewind()
            points.forEachIndexed { index, point ->
                val p = preview?.toDisplay(Bounds(point.x, point.y, 0f, 0f)) ?: Bounds(point.x, point.y, 0f, 0f)
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close(); invalidate()
        }
        override fun onDraw(canvas: Canvas) {
            paint.color = color; paint.alpha = 64; paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)
            paint.alpha = 255; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f * density
            canvas.drawPath(path, paint)
        }
    }
}
