package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.min
import kotlin.math.roundToInt

@Composable fun PanelCardPreview(panel: PanelConfig, trigger: TriggerSettings, landscape: Boolean = false) {
    if (panel.buttons.isEmpty()) {
        Surface(Modifier.fillMaxWidth().height(88.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(AppIcons.Apps, null, Modifier.size(24.dp)); Spacer(Modifier.height(8.dp)); Text(t(R.string.no_buttons_setup), style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        val screen = rememberPreviewSpace(landscape)
        val handle = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, trigger, landscape)
        val layout = panel.layout(landscape)
        val geometry = PanelGeometry.calculate(screen.usable, screen.density, layout, panel.buttons.size, handle, trigger.placement(landscape).edge)
        Layout(content = { PanelContent(panel, layout, geometry) }, modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) { children, constraints ->
            val factor = min(1f, min(constraints.maxWidth / geometry.bounds.width, constraints.maxHeight / geometry.bounds.height))
            val child = children.single().measure(Constraints.fixed(geometry.bounds.width.roundToInt().coerceAtLeast(1), geometry.bounds.height.roundToInt().coerceAtLeast(1)))
            val height = (geometry.bounds.height * factor).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(constraints.maxWidth, height) {
                child.placeWithLayer(((constraints.maxWidth - child.width * factor) / 2).roundToInt(), 0) { scaleX = factor; scaleY = factor; transformOrigin = TransformOrigin(0f, 0f) }
            }
        }
    }
}
