package com.modukey.app.platform

import android.graphics.Matrix
import android.graphics.Path
import com.modukey.app.core.Edge

/** Half-squircle guide: flush at the screen edge, with soft inward-facing corners. */
object EdgeHighlightShape {
    fun path(width: Float, height: Float, edge: Edge, destination: Path = Path()): Path {
        destination.rewind()
        val depth = if (edge == Edge.BOTTOM) height else width
        val length = if (edge == Edge.BOTTOM) width else height
        if (depth <= 0f || length <= 0f) return destination
        val radius = minOf(depth, length / 2)
        // Flatter shoulders than a circular corner approximate a squircle's curve.
        val shoulder = radius * 0.9f
        destination.apply {
            moveTo(0f, 0f)
            lineTo(depth - radius, 0f)
            cubicTo(depth - radius + shoulder, 0f, depth, radius - shoulder, depth, radius)
            lineTo(depth, length - radius)
            cubicTo(depth, length - radius + shoulder, depth - radius + shoulder, length, depth - radius, length)
            lineTo(0f, length)
            close()
        }
        when (edge) {
            Edge.LEFT -> Unit
            Edge.RIGHT -> destination.transform(Matrix().apply { setScale(-1f, 1f); postTranslate(width, 0f) })
            Edge.BOTTOM -> destination.transform(Matrix().apply {
                setValues(floatArrayOf(0f, 1f, 0f, -1f, 0f, height, 0f, 0f, 1f))
            })
        }
        return destination
    }
}
