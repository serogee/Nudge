package com.modukey.app.ui

import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState

/** Fixed uses plain rows/columns; no scroll container can expose overflow accidentally. */
@Composable
fun PanelContent(panel: PanelConfig, layout: PanelLayout, geometry: PanelGeometryResult, state: SoundState = SoundState(),
    interactive: Boolean = false, onAction: (PanelButton) -> Unit = {}) {
    val density = LocalDensity.current
    val cell = with(density) { geometry.cell.toDp() }
    val gap = with(density) { geometry.gap.toDp() }
    val padding = with(density) { geometry.padding.toDp() }
    val shown = panel.buttons.take(geometry.visibleCount)
    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(panel.cornerDp.dp),
        color = Color(panel.background).copy(alpha = panel.opacity), contentColor = Color.White) {
        if (shown.isEmpty()) Box(Modifier.padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(stringResource(R.string.no_buttons), style = MaterialTheme.typography.labelSmall) }
        else if (layout.mode == PanelMode.SCROLLABLE) {
            if (layout.direction == PanelDirection.VERTICAL) LazyVerticalGrid(columns = GridCells.Fixed(geometry.crossCount),
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(padding), userScrollEnabled = interactive,
                horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                items(shown, key = { it.id }) { b -> ActionButton(b, panel, cell, state, interactive) { onAction(b) } }
            } else LazyHorizontalGrid(rows = GridCells.Fixed(geometry.crossCount), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(padding), userScrollEnabled = interactive,
                horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                items(shown, key = { it.id }) { b -> ActionButton(b, panel, cell, state, interactive) { onAction(b) } }
            }
        } else if (layout.direction == PanelDirection.VERTICAL) {
            Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(gap)) {
                shown.chunked(geometry.crossCount).forEach { line -> Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    line.forEach { b -> ActionButton(b, panel, cell, state, interactive) { onAction(b) } }
                } }
            }
        } else Row(Modifier.padding(padding), horizontalArrangement = Arrangement.spacedBy(gap)) {
            shown.chunked(geometry.crossCount).forEach { line -> Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                line.forEach { b -> ActionButton(b, panel, cell, state, interactive) { onAction(b) } }
            } }
        }
    }
}

@Composable
private fun ActionButton(button: PanelButton, panel: PanelConfig, cell: androidx.compose.ui.unit.Dp, state: SoundState, interactive: Boolean, action: () -> Unit) {
    val context = LocalContext.current
    val title = context.buttonLabel(button)
    val selected = when (button.action) {
        ButtonAction.MEDIA_MUTE -> state.muted
        ButtonAction.SILENT -> state.ringer == AudioManager.RINGER_MODE_SILENT
        ButtonAction.VIBRATE -> state.ringer == AudioManager.RINGER_MODE_VIBRATE
        ButtonAction.DND -> state.dnd
        else -> false
    }
    val isToggle = button.action in setOf(ButtonAction.MEDIA_MUTE, ButtonAction.SILENT, ButtonAction.VIBRATE, ButtonAction.DND)
    val description = if (isToggle) context.getString(R.string.action_state, title, context.getString(if (selected) R.string.on else R.string.off)) else title
    val shape = when (panel.iconShape) {
        IconShape.CIRCLE -> CircleShape
        IconShape.ROUNDED_SQUARE -> RoundedCornerShape((panel.iconCornerFraction * 100).toInt())
        IconShape.SQUIRCLE -> remember(panel.iconCornerFraction) { SquircleShape(panel.iconCornerFraction) }
    }
    Box(Modifier.size(cell).clip(shape).background(if (selected) Color(0xFF426523) else Color.White.copy(alpha = 0.09f))
        .then(if (interactive) Modifier.clickable(role = Role.Button, onClickLabel = title, onClick = action) else Modifier)
        .semantics { contentDescription = description }, contentAlignment = androidx.compose.ui.Alignment.Center) {
        ButtonGraphic(button, panel.iconPack, Modifier.size(cell * 0.5f).clip(shape))
    }
}

@Composable
fun ButtonGraphic(button: PanelButton, pack: String, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as NudgeApplication
    val density = LocalDensity.current.density
    val bitmap by produceState<ImageBitmap?>(null, button.action, button.appPackage, button.appActivity, button.icon, pack, density) {
        value = app.icons.icon(button, pack, (48 * density).toInt())?.asImageBitmap()
    }
    if (bitmap != null) Image(bitmap!!, contentDescription = null, modifier = modifier)
    else when (button.action) {
        ButtonAction.NUDGE_SETTINGS -> Image(painterResource(R.drawable.ic_modukey), null, modifier)
        ButtonAction.PHONE_SETTINGS -> Icon(Icons.Outlined.Settings, null, modifier, tint = Color.White)
        else -> Canvas(modifier) {
            val u = size.minDimension / 24f
            fun p(x: Float, y: Float) = Offset(x * u, y * u)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(Color.White, p(x1, y1), p(x2, y2), 1.8f * u, StrokeCap.Round)
            when (button.action) {
                ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN, ButtonAction.MEDIA_MUTE -> {
                    val path = Path().apply { moveTo(3*u, 9*u); lineTo(7*u, 9*u); lineTo(12*u, 5*u); lineTo(12*u, 19*u); lineTo(7*u, 15*u); lineTo(3*u, 15*u); close() }
                    drawPath(path, Color.White)
                    if (button.action == ButtonAction.MEDIA_MUTE) { line(16f, 9f, 21f, 15f); line(21f, 9f, 16f, 15f) }
                    else { line(16f, 12f, 22f, 12f); if (button.action == ButtonAction.VOLUME_UP) line(19f, 9f, 19f, 15f) }
                }
                ButtonAction.SILENT -> { line(7f, 16f, 17f, 16f); line(7f, 16f, 8f, 7f); line(8f, 7f, 16f, 7f); line(16f, 7f, 17f, 16f); line(10f, 20f, 14f, 20f); line(3f, 3f, 21f, 21f) }
                ButtonAction.VIBRATE -> { drawRect(Color.White, p(8f, 4f), Size(8*u, 16*u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6f*u)); line(4f, 8f, 3f, 12f); line(3f, 12f, 4f, 16f); line(20f, 8f, 21f, 12f); line(21f, 12f, 20f, 16f) }
                ButtonAction.DND -> { drawCircle(Color.White, 9*u, p(12f, 12f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f*u)); line(7f, 12f, 17f, 12f) }
                ButtonAction.LOCK -> { drawRect(Color.White, p(5f, 10f), Size(14*u, 11*u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f*u)); drawArc(Color.White, 180f, 180f, false, p(8f, 2f), Size(8*u, 14*u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f*u)); line(12f, 14f, 12f, 17f) }
                else -> for (x in listOf(4f, 14f)) for (y in listOf(4f, 14f)) drawRect(Color.White, p(x,y), Size(6*u,6*u))
            }
        }
    }
}

private class SquircleShape(private val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = size.minDimension * fraction.coerceIn(0f, 0.5f)
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(r, 0f); lineTo(w-r, 0f); cubicTo(w, 0f, w, 0f, w, r)
            lineTo(w, h-r); cubicTo(w, h, w, h, w-r, h)
            lineTo(r, h); cubicTo(0f, h, 0f, h, 0f, h-r)
            lineTo(0f, r); cubicTo(0f, 0f, 0f, 0f, r, 0f); close()
        }
        return Outline.Generic(p)
    }
}
