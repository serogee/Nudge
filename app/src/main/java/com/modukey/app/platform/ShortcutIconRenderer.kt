package com.modukey.app.platform

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.ui.AppIcons
import com.modukey.app.ui.ButtonVectors
import kotlin.math.roundToInt

/** Rasterize static app/vector icons on demand. No composition or running window is needed. */
internal object ShortcutIconRenderer {
    suspend fun render(context: Context, icons: IconRepository, button: PanelButton): Bitmap {
        val size = (96 * context.resources.displayMetrics.density).roundToInt().coerceIn(96, 288)
        val bitmap = createBitmap(size, size); val canvas = Canvas(bitmap)
        val container = button.iconContainer == IconContainer.CONTAINER
        val outer = size * .08f; val radius = size * .23f
        if (container) canvas.drawRoundRect(outer, outer, size - outer, size - outer, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3C3C3E.toInt() })
        val inset = if (container) size * .25f else outer
        val side = size - inset * 2
        val save = canvas.save(); canvas.translate(inset, inset)
        canvas.clipRect(0f, 0f, side, side)
        val external = if (button.icon.builtIn == null) icons.icon(button, "", size) else null
        when {
            external != null -> {
                val shader = BitmapShader(external, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                    setLocalMatrix(Matrix().apply { setScale(side / external.width, side / external.height) })
                }
                canvas.drawRoundRect(0f, 0f, side, side, side * .25f, side * .25f,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader })
            }
            button.icon.builtIn == null && button.action == ButtonAction.NUDGE_SETTINGS -> ContextCompat.getDrawable(context, R.drawable.ic_modukey)?.let { it.setBounds(0, 0, side.roundToInt(), side.roundToInt()); it.draw(canvas) }
            else -> {
                val vector = button.icon.builtIn?.let(AppIcons::library) ?: ButtonVectors.action(button.action)
                canvas.scale(side / vector.viewportWidth, side / vector.viewportHeight)
                drawGroup(canvas, vector.root)
            }
        }
        canvas.restoreToCount(save); return bitmap
    }
    private fun drawGroup(canvas: Canvas, group: VectorGroup) {
        val save = canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation); canvas.scale(group.scaleX, group.scaleY); canvas.translate(-group.pivotX, -group.pivotY)
        if (group.clipPathData.isNotEmpty()) canvas.clipPath(PathParser().addPathNodes(group.clipPathData).toPath().asAndroidPath())
        for (node in group) when (node) {
            is VectorGroup -> drawGroup(canvas, node)
            is VectorPath -> {
                val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                path.fillType = if (node.pathFillType == PathFillType.EvenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                if (node.fill != null) { paint.alpha = (node.fillAlpha * 255).roundToInt(); paint.style = Paint.Style.FILL; canvas.drawPath(path, paint) }
                if (node.stroke != null) {
                    paint.alpha = (node.strokeAlpha * 255).roundToInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = node.strokeLineWidth
                    paint.strokeCap = when (node.strokeLineCap) { StrokeCap.Round -> Paint.Cap.ROUND; StrokeCap.Square -> Paint.Cap.SQUARE; else -> Paint.Cap.BUTT }
                    paint.strokeJoin = when (node.strokeLineJoin) { StrokeJoin.Round -> Paint.Join.ROUND; StrokeJoin.Bevel -> Paint.Join.BEVEL; else -> Paint.Join.MITER }
                    paint.strokeMiter = node.strokeLineMiter; canvas.drawPath(path, paint)
                }
            }
        }
        canvas.restoreToCount(save)
    }
}
