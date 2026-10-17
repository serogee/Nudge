package com.modukey.app.ui

import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.modukey.app.core.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun PhonePreview(settings: TriggerSettings, landscape: Boolean, onPosition: (Float) -> Unit) {
    val context = LocalContext.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val metrics = remember(config) { context.getSystemService(WindowManager::class.java).currentWindowMetrics }
    val density = context.resources.displayMetrics.density
    val actualLandscape = metrics.bounds.width() > metrics.bounds.height()
    val short = min(metrics.bounds.width(), metrics.bounds.height()).toFloat()
    val long = max(metrics.bounds.width(), metrics.bounds.height()).toFloat()
    val screenW = if (landscape) long else short
    val screenH = if (landscape) short else long
    val safe = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
    val mandatory = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
    val insets = if (actualLandscape == landscape) Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), max(safe.bottom, mandatory.bottom).toFloat())
        else Insets(top = 28 * density, bottom = 24 * density)
    val placement = settings.placement(landscape)
    val bounds = TriggerGeometry.bounds(screenW, screenH, density, insets, settings, landscape)
    val currentPosition by rememberUpdatedState(onPosition)
    var gestureSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        Modifier.fillMaxWidth().height(if (landscape) 175.dp else 280.dp)
            .semantics { contentDescription = "Phone preview. Drag the highlighted trigger to reposition it; the position slider provides the same control." }
            .pointerInput(landscape, placement.edge, settings.lengthDp, screenW, screenH, insets) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val scale = min(gestureSize.width / screenW, gestureSize.height / screenH) * 0.92f
                    if (scale > 0) {
                        val origin = Offset((gestureSize.width - screenW * scale) / 2, (gestureSize.height - screenH * scale) / 2)
                        val along = if (placement.edge == Edge.BOTTOM) (change.position.x - origin.x) / scale - insets.left else (change.position.y - origin.y) / scale - insets.top
                        val available = if (placement.edge == Edge.BOTTOM) screenW - insets.left - insets.right else screenH - insets.top - insets.bottom
                        val length = min(settings.lengthDp * density, available)
                        currentPosition(((along - length / 2) / (available - length).coerceAtLeast(1f)).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        gestureSize = size
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
        drawRect(Color(0x554E7E21), location(bounds.x, bounds.y), Size(bounds.width * scale, bounds.height * scale))
        if (settings.indicatorVisible) {
            val indicator = TriggerGeometry.indicator(bounds.width, bounds.height, density, settings, placement.edge)
            val radius = min(settings.cornerRadiusDp * density, min(indicator.width, indicator.height) / 2) * scale
            drawRoundRect(Color(settings.indicatorColor).copy(alpha = settings.indicatorOpacity), location(bounds.x + indicator.x, bounds.y + indicator.y), Size(indicator.width * scale, indicator.height * scale), CornerRadius(radius))
        }
    }
}
