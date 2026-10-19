package com.modukey.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.modukey.app.core.*
import com.modukey.app.platform.EdgeHighlightShape
import kotlin.math.min

@Composable
fun PhonePreview(settings: TriggerSettings, landscape: Boolean, others: List<TriggerSettings> = emptyList()) {
    val context = LocalContext.current
    val screen = rememberPreviewSpace(landscape)
    val screenW = screen.width; val screenH = screen.height; val density = screen.density; val insets = screen.insets
    val placement = settings.placement(landscape)
    val bounds = TriggerGeometry.bounds(screenW, screenH, density, insets, settings, landscape)
    val highlightPath = remember(bounds.width, bounds.height, placement.edge) {
        EdgeHighlightShape.path(bounds.width, bounds.height, placement.edge).asComposePath()
    }
    Canvas(
        Modifier.fillMaxWidth().height(if (landscape) 175.dp else 280.dp)
            .semantics { contentDescription = context.getString(com.modukey.app.R.string.handle_preview_description) }

    ) {
        val scale = min(size.width / screenW, size.height / screenH) * 0.92f
        val origin = Offset((size.width - screenW * scale) / 2, (size.height - screenH * scale) / 2)
        fun location(x: Float, y: Float) = origin + Offset(x * scale, y * scale)
        drawRoundRect(Color(0xFF22281E), origin - Offset(5f, 5f), Size(screenW * scale + 10f, screenH * scale + 10f), CornerRadius(25f))
        drawRoundRect(Color(0xFFE9EDDF), origin, Size(screenW * scale, screenH * scale), CornerRadius(22f))
        drawCircle(Color(0xFF22281E), 3.5f, location(screenW / 2, insets.top.coerceAtLeast(20 * density) / 2))
        // Decorative content gives context without suggesting a custom volume panel.
        drawRoundRect(Color(0xFFD6DDCA), location(screenW * 0.14f, screenH * 0.28f), Size(screenW * 0.72f * scale, screenH * 0.13f * scale), CornerRadius(12f))
        drawRoundRect(Color(0xFFCBD6BD), location(screenW * 0.14f, screenH * 0.46f), Size(screenW * 0.46f * scale, 5 * density * scale), CornerRadius(5f))
        drawRoundRect(Color(0xFFD6DDCA), location(screenW * 0.14f, screenH * 0.51f), Size(screenW * 0.65f * scale, 4 * density * scale), CornerRadius(5f))
        others.forEach { other ->
            val b = TriggerGeometry.bounds(screenW, screenH, density, insets, other, landscape)
            drawRoundRect(Color(0x66C24646), location(b.x, b.y), Size(b.width * scale, b.height * scale), CornerRadius(3f))
        }
        withTransform({
            val position = location(bounds.x, bounds.y)
            translate(position.x, position.y)
            scale(scale, scale, pivot = Offset.Zero)
        }) { drawPath(highlightPath, Color(0x554E7E21)) }
        if (settings.indicatorVisible) {
            val indicator = TriggerGeometry.indicator(bounds.width, bounds.height, settings, placement.edge, density)
            val radius = min(settings.cornerRadiusDp * density, min(indicator.width, indicator.height) / 2) * scale
            drawRoundRect(Color(settings.indicatorColor).copy(alpha = settings.indicatorOpacity), location(bounds.x + indicator.x, bounds.y + indicator.y), Size(indicator.width * scale, indicator.height * scale), CornerRadius(radius))
        }
    }
}
