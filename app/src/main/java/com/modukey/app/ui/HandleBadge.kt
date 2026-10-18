package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.modukey.app.core.Edge
import com.modukey.app.core.HandleConfig

@Composable fun HandleBadge(handle: HandleConfig, name: String, modifier: Modifier = Modifier) {
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val shape = androidx.compose.runtime.remember(handle.trigger.placement(landscape).edge) { HandleBadgeShape(handle.trigger.placement(landscape).edge) }
    val color = Color(handle.trigger.indicatorColor)
    Surface(modifier.size(44.dp), shape = shape, color = color, contentColor = contrastOn(color)) {
        Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium) }
    }
}

private class HandleBadgeShape(private val edge: Edge) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val corner = with(density) { 12.dp.toPx() }
        val bite = with(density) { 6.dp.toPx() }
        val outer = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(corner)))
        }
        val center = when (edge) {
            Edge.LEFT -> androidx.compose.ui.geometry.Offset(0f, size.height / 2)
            Edge.RIGHT -> androidx.compose.ui.geometry.Offset(size.width, size.height / 2)
            Edge.BOTTOM -> androidx.compose.ui.geometry.Offset(size.width / 2, size.height)
        }
        val notch = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center.x - bite, center.y - bite, center.x + bite, center.y + bite))
        }
        return androidx.compose.ui.graphics.Outline.Generic(androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, outer, notch))
    }
}
