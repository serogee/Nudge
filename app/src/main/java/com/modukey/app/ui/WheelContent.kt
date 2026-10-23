package com.modukey.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt

@Composable internal fun WheelContent(panel: PanelConfig, layout: PanelLayout, geometry: WheelGeometryResult, state: SoundState,
    interactive: Boolean, selected: Int?, onAction: (PanelButton) -> Unit) {
    val density = LocalDensity.current
    val latestAction by rememberUpdatedState(onAction)
    val iconRasterDp = with(density) { (geometry.cell * geometry.expansion * .55f).toDp().value.roundToInt().coerceAtLeast(1) }
    val animate = layout.wheel.animationsEnabled && geometry.expansion > 1f && ValueAnimator.areAnimatorsEnabled()
    val weights = List(geometry.count) { index ->
        val weight by animateFloatAsState(if (index == selected) 1f else 0f,
            animationSpec = if (animate) tween(140, easing = FastOutSlowInEasing) else snap(), label = "wheel-focus-$index")
        weight
    }
    val slices = geometry.slices(weights)
    val shown = panel.buttons.take(geometry.count)
    val faces = shown.map { buttonPresentation(it, panel, state) }
    val highlight = MaterialTheme.colorScheme.primary
    val click = if (interactive) Modifier.pointerInput(geometry, shown, selected) {
        detectTapGestures { position ->
            geometry.tapHit(position.x + geometry.bounds.x, position.y + geometry.bounds.y, selected)?.let { index ->
                shown[index].takeIf { it.action != ButtonAction.EMPTY }?.let(latestAction)
            }
        }
    } else Modifier

    Layout(content = {
        shown.forEachIndexed { index, button ->
            val face = faces[index]
            val icon = with(density) { slices[index].iconSize.toDp() }
            Box(Modifier.clip(CircleShape)
                .then(if (interactive && button.action != ButtonAction.EMPTY) Modifier.rejectDraggedClick()
                    .clickable(role = Role.Button, onClickLabel = face.title) { latestAction(button) } else Modifier)
                .then(if (button.action != ButtonAction.EMPTY) Modifier.semantics {
                    contentDescription = face.description
                    if (face.toggle) this.selected = face.active
                } else Modifier), contentAlignment = Alignment.Center) {
                if (button.action != ButtonAction.EMPTY || button.emptyAppearance == EmptyAppearance.SLASH)
                    ButtonGraphic(button, panel.iconPack, Modifier.size(icon).clip(iconShape(panel)).alpha(panel.iconOpacity),
                        face.palette.icon, iconRasterDp)
            }
        }
    }, modifier = Modifier.fillMaxSize().then(click).drawBehind {
        fun path(slice: WheelSlice) = Path().apply {
            val inset = geometry.angularGap(slice) / 2f
            val first = (slice.start + inset) * 180f / PI.toFloat()
            val sweep = (slice.end - slice.start - inset * 2f) * 180f / PI.toFloat()
            val start = when (geometry.edge) { Edge.LEFT -> first; Edge.RIGHT -> 180f - first; Edge.BOTTOM -> first - 90f }
            val direction = if (geometry.edge == Edge.RIGHT) -1f else 1f
            val cx = geometry.center.x - geometry.bounds.x; val cy = geometry.center.y - geometry.bounds.y
            fun rect(radius: Float) = Rect(cx - radius, cy - radius, cx + radius, cy + radius)
            arcTo(rect(slice.outerRadius), start, sweep * direction, true)
            val end = geometry.point(slice.end - inset, slice.innerRadius)
            lineTo(end.x - geometry.bounds.x, end.y - geometry.bounds.y)
            arcTo(rect(slice.innerRadius), start + sweep * direction, -sweep * direction, false)
            close()
        }
        fun fill(shape: Path, color: Color) = drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = color
                if (panel.cornerDp > 0f) pathEffect = PathEffect.cornerPathEffect(panel.cornerDp.dp.toPx())
            }
            canvas.drawPath(shape, paint)
        }
        val background = Color(panel.background).copy(alpha = panel.opacity)
        if (slices.isEmpty()) {
            val empty = WheelSlice(-PI.toFloat() / 2f, PI.toFloat() / 2f, geometry.center, geometry.cell,
                geometry.innerRadius, geometry.innerRadius + geometry.cell, geometry.cell * .55f)
            fill(path(empty), background)
        }
        slices.forEachIndexed { index, slice ->
            val button = shown[index]
            if (button.action != ButtonAction.EMPTY || button.emptyAppearance != EmptyAppearance.NO_BUTTON) {
                val shape = path(slice); val face = faces[index]
                fill(shape, background)
                if (face.container) fill(shape, face.palette.background)
                face.palette.outline?.let { drawPath(shape, it, style = Stroke(2.dp.toPx())) }
                if (index == selected) drawPath(shape, highlight, style = Stroke(2.dp.toPx()))
            }
        }
    }) { children, constraints ->
        val measured = children.mapIndexed { index, child ->
            val target = max(geometry.minimumCell, slices[index].iconSize).roundToInt().coerceAtLeast(1)
            child.measure(Constraints.fixed(target, target))
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
