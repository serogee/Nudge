package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modukey.app.core.Edge
import com.modukey.app.core.HandleConfig

@Composable fun HandleBadge(handle: HandleConfig, name: String, modifier: Modifier = Modifier) {
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val edge = handle.trigger.placement(landscape).edge
    val color = Color(handle.trigger.indicatorColor)
    val ink = contrastOn(color)
    Surface(modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = color, contentColor = ink) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 4.dp.toPx()
                drawRoundRect(ink.copy(alpha = .55f), topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.3.dp.toPx()))
                val depth = 3.dp.toPx(); val length = 14.dp.toPx(); val gap = 7.dp.toPx()
                val horizontal = edge == Edge.BOTTOM
                val pill = androidx.compose.ui.geometry.Size(if (horizontal) length else depth, if (horizontal) depth else length)
                val x = when (edge) { Edge.LEFT -> gap; Edge.RIGHT -> size.width - gap - depth; Edge.BOTTOM -> (size.width - length) / 2 }
                val y = if (horizontal) size.height - gap - depth else (size.height - length) / 2
                drawRoundRect(ink, androidx.compose.ui.geometry.Offset(x, y), pill, androidx.compose.ui.geometry.CornerRadius(depth / 2))
            }
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}
