package com.modukey.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState
import kotlin.math.PI
import kotlin.math.roundToInt

@Composable internal fun WheelContent(panel: PanelConfig, geometry: WheelGeometryResult, state: SoundState,
    interactive: Boolean, selected: Int?, onAction: (PanelButton) -> Unit) {
    val density = LocalDensity.current
    val slices = geometry.slices(selected)
    val highlight = MaterialTheme.colorScheme.primary
    Layout(content = {
        panel.buttons.take(geometry.count).forEachIndexed { index, button ->
            val size = with(density) { slices[index].size.toDp() }
            Box(if (index == selected) Modifier.border(3.dp, highlight, iconShape(panel)) else Modifier) {
                PanelButtonFace(button, panel, size, state, interactive) { onAction(button) }
            }
        }
    }, modifier = Modifier.fillMaxSize().drawBehind {
        val cx = geometry.center.x - geometry.bounds.x
        val cy = geometry.center.y - geometry.bounds.y
        val path = Path().apply {
            moveTo(cx, cy)
            for (step in 0..64) {
                val point = geometry.point(-PI.toFloat() / 2f + PI.toFloat() * step / 64,
                    geometry.radius + geometry.cell * geometry.expansion / 2f)
                lineTo(point.x - geometry.bounds.x, point.y - geometry.bounds.y)
            }
            close()
        }
        drawPath(path, Color(panel.background).copy(alpha = panel.opacity))
    }) { children, constraints ->
        val measured = children.mapIndexed { index, child ->
            val size = slices[index].size.roundToInt().coerceAtLeast(1)
            child.measure(Constraints.fixed(size, size))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            measured.forEachIndexed { index, child ->
                val p = slices[index].point
                child.place((p.x - geometry.bounds.x - child.width / 2f).roundToInt(),
                    (p.y - geometry.bounds.y - child.height / 2f).roundToInt())
            }
        }
    }
}
