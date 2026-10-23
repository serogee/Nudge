package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.modukey.app.core.*
import com.modukey.app.platform.ScreenSpace
import com.modukey.app.platform.EdgeHighlightShape
import kotlin.math.min
import kotlin.math.roundToInt

@Composable internal fun PanelPreview(panel: PanelConfig, layout: PanelLayout, geometry: PanelGeometryResult, screen: ScreenSpace, handle: Bounds, trigger: TriggerSettings) {
    val edge = trigger.placement(screen.landscape).edge
    val path = remember(handle.width, handle.height, edge) { EdgeHighlightShape.path(handle.width,handle.height,edge).asComposePath() }
    fun scale(size: Size) = min(size.width / screen.width, size.height / screen.height) * 0.92f
    fun origin(size: Size, factor: Float) = Offset((size.width-screen.width*factor)/2, (size.height-screen.height*factor)/2)
    val modifier = Modifier.fillMaxWidth().height(if (screen.landscape) 210.dp else 320.dp).clipToBounds()
        .drawBehind {
            val factor = scale(size); val o = origin(size, factor)
            drawRoundRect(Color(0xFF22281E), o-Offset(4f,4f), Size(screen.width*factor+8,screen.height*factor+8), CornerRadius(20f))
            drawRoundRect(Color(0xFFE9EDDF), o, Size(screen.width*factor,screen.height*factor), CornerRadius(18f))
            withTransform({ translate(o.x+handle.x*factor,o.y+handle.y*factor); scale(factor,factor,Offset.Zero) }) { drawPath(path,Color(0x554E7E21)) }
            if(trigger.indicatorVisible) {
                val indicator=TriggerGeometry.indicator(handle.width,handle.height,trigger,edge,screen.density)
                val radius=min(trigger.cornerRadiusDp*screen.density,min(indicator.width,indicator.height)/2)*factor
                drawRoundRect(Color(trigger.indicatorColor).copy(alpha=trigger.indicatorOpacity),o+Offset((handle.x+indicator.x)*factor,(handle.y+indicator.y)*factor),Size(indicator.width*factor,indicator.height*factor),CornerRadius(radius))
            }
        }
    Layout(content = { PanelContent(panel, layout, geometry, wheelSelection = if (geometry.wheel != null && layout.wheel.expandSelection && geometry.visibleCount > 0) geometry.visibleCount / 2 else null) }, modifier = modifier) { children, constraints ->
        val child = children.single().measure(Constraints.fixed(geometry.bounds.width.roundToInt().coerceAtLeast(1),geometry.bounds.height.roundToInt().coerceAtLeast(1)))
        val size = Size(constraints.maxWidth.toFloat(),constraints.maxHeight.toFloat()); val factor = scale(size); val o = origin(size,factor)
        layout(constraints.maxWidth,constraints.maxHeight) {
            child.placeWithLayer((o.x+geometry.bounds.x*factor).roundToInt(),(o.y+geometry.bounds.y*factor).roundToInt()) {
                scaleX = factor; scaleY = factor; transformOrigin = TransformOrigin(0f,0f)
            }
        }
    }
}
