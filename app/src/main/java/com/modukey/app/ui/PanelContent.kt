package com.modukey.app.ui

import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState
import kotlin.math.roundToInt

/** Fixed uses plain rows/columns; no scroll container can expose overflow accidentally. */
@Composable
fun PanelContent(panel: PanelConfig, layout: PanelLayout, geometry: PanelGeometryResult, state: SoundState = SoundState(),
    interactive: Boolean = false, wheelSelection: Int? = null, wheelDragging: Boolean = false, onAction: (PanelButton) -> Unit = {}) {
    geometry.wheel?.let {
        WheelContent(panel, layout, it, state, interactive && !wheelDragging, wheelSelection, onAction)
        return
    }
    val density = LocalDensity.current
    val cell = with(density) { geometry.cell.toDp() }
    val gap = with(density) { geometry.gap.toDp() }
    val padding = with(density) { geometry.padding.toDp() }
    val shown = panel.buttons.take(geometry.visibleCount)
    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(panel.cornerDp.dp),
        color = Color(panel.background).copy(alpha = panel.opacity), contentColor = contrastOn(Color(panel.background).copy(alpha = panel.opacity).compositeOver(MaterialTheme.colorScheme.surface))) {
        if (shown.isEmpty()) Box(Modifier.padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(stringResource(R.string.no_buttons), style = MaterialTheme.typography.labelSmall) }
        else if (layout.mode == PanelMode.SCROLLABLE) {
            if (layout.direction == PanelDirection.VERTICAL) LazyVerticalGrid(columns = GridCells.Fixed(geometry.crossCount),
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(padding), userScrollEnabled = interactive,
                horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                items(shown, key = { it.id }) { b -> PanelButtonFace(b, panel, cell, state, interactive) { onAction(b) } }
            } else LazyHorizontalGrid(rows = GridCells.Fixed(geometry.crossCount), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(padding), userScrollEnabled = interactive,
                horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                items(shown, key = { it.id }) { b -> PanelButtonFace(b, panel, cell, state, interactive) { onAction(b) } }
            }
        } else if (layout.direction == PanelDirection.VERTICAL) {
            Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(gap)) {
                shown.chunked(geometry.crossCount).forEach { line -> Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    line.forEach { b -> PanelButtonFace(b, panel, cell, state, interactive) { onAction(b) } }
                } }
            }
        } else Row(Modifier.padding(padding), horizontalArrangement = Arrangement.spacedBy(gap)) {
            shown.chunked(geometry.crossCount).forEach { line -> Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                line.forEach { b -> PanelButtonFace(b, panel, cell, state, interactive) { onAction(b) } }
            } }
        }
    }
}

internal data class ButtonPresentation(val title: String, val description: String, val active: Boolean, val toggle: Boolean,
    val container: Boolean, val palette: ButtonPalette)

@Composable internal fun buttonPresentation(button: PanelButton, panel: PanelConfig, state: SoundState): ButtonPresentation {
    val context = LocalContext.current
    val title = context.buttonLabel(button)
    val selected = when (button.action) {
        ButtonAction.MEDIA_MUTE -> state.muted
        ButtonAction.SILENT -> state.ringer == AudioManager.RINGER_MODE_SILENT
        ButtonAction.VIBRATE -> state.ringer == AudioManager.RINGER_MODE_VIBRATE
        ButtonAction.DND -> state.ownDnd
        ButtonAction.BRIGHTNESS_MAXIMUM -> state.maximumBrightness
        ButtonAction.BRIGHTNESS_MINIMUM -> state.minimumBrightness
        ButtonAction.BRIGHTNESS_AUTOMATIC -> state.automaticBrightness
        ButtonAction.TOGGLE_HANDLE_VISIBILITY -> handleToggleTargets(button.handleScope, state.ownerHandleId, state.enabledHandleIds).let { targets ->
            targets.isNotEmpty() && targets.none { it in state.hiddenIndicatorIds }
        }
        ButtonAction.TOGGLE_AND_HIDE_HANDLES -> {
            val paused = handleGroupTargets(button.toggleScope, state.ownerHandleId, state.enabledHandleIds)
            val hidden = handleGroupTargets(button.visibilityScope, state.ownerHandleId, state.enabledHandleIds)
            button.hasHandleGroups &&
                (button.toggleScope == HandleGroupScope.NONE || (paused.isNotEmpty() && paused.all { it in state.pausedHandleIds })) &&
                (button.visibilityScope == HandleGroupScope.NONE || (hidden.isNotEmpty() && hidden.all { it in state.hiddenIndicatorIds }))
        }
        ButtonAction.TOGGLE_HANDLES -> handleToggleTargets(button.handleScope, state.ownerHandleId, state.enabledHandleIds).let { targets ->
            state.handlesEnabled && targets.isNotEmpty() && targets.none { it in state.pausedHandleIds }
        }
        else -> false
    }
    val isToggle = button.action in setOf(ButtonAction.MEDIA_MUTE, ButtonAction.SILENT, ButtonAction.VIBRATE, ButtonAction.DND,
        ButtonAction.BRIGHTNESS_MAXIMUM, ButtonAction.BRIGHTNESS_MINIMUM, ButtonAction.BRIGHTNESS_AUTOMATIC, ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.TOGGLE_AND_HIDE_HANDLES)
    val description = if (button.action == ButtonAction.TOGGLE_AND_HIDE_HANDLES) {
        fun group(scope: HandleGroupScope, affected: Set<String>, off: Int, on: Int): String {
            val targets = handleGroupTargets(scope, state.ownerHandleId, state.enabledHandleIds)
            return context.getString(when {
                scope == HandleGroupScope.NONE || targets.isEmpty() -> R.string.no_handles_target
                targets.all { it in affected } -> on
                targets.none { it in affected } -> off
                else -> R.string.handle_group_mixed
            })
        }
        context.getString(R.string.combined_handle_state, title,
            group(button.toggleScope, state.pausedHandleIds, R.string.handle_group_running, R.string.handle_group_paused),
            group(button.visibilityScope, state.hiddenIndicatorIds, R.string.handle_group_visible, R.string.handle_group_hidden))
    } else if (isToggle) context.getString(R.string.action_state, title, context.getString(if (selected) R.string.on else R.string.off)) else title
    val container = if (button.action == ButtonAction.EMPTY && button.emptyAppearance == EmptyAppearance.NO_ICON) true else when (button.iconContainer) {
        IconContainer.DEFAULT -> panel.iconBackground
        IconContainer.CONTAINER -> true
        IconContainer.STANDALONE -> false
    }
    val palette = buttonPalette(panel, selected, container, MaterialTheme.colorScheme.surface)
    return ButtonPresentation(title, description, selected, isToggle, container, palette)
}

@Composable
fun PanelButtonFace(button: PanelButton, panel: PanelConfig, cell: androidx.compose.ui.unit.Dp, state: SoundState = SoundState(), interactive: Boolean = false, action: () -> Unit = {}) {
    if (button.action == ButtonAction.EMPTY && button.emptyAppearance == EmptyAppearance.NO_BUTTON) {
        Box(Modifier.size(cell)); return
    }
    val presentation = buttonPresentation(button, panel, state)
    val title = presentation.title; val description = presentation.description
    val selected = presentation.active; val isToggle = presentation.toggle
    val container = presentation.container; val palette = presentation.palette
    val shape = iconShape(panel)
    val tint = palette.icon
    Box(Modifier.size(cell).clip(shape).then(if (container) Modifier.background(palette.background) else Modifier)
        .then(if (palette.outline != null) Modifier.border(2.dp, palette.outline, shape) else Modifier)
        .then(if (interactive && button.action != ButtonAction.EMPTY) Modifier.rejectDraggedClick().clickable(role = Role.Button, onClickLabel = title, onClick = action) else Modifier)
        .then(if (button.action != ButtonAction.EMPTY) Modifier.semantics { contentDescription = description; if (isToggle) this.selected = selected } else Modifier), contentAlignment = androidx.compose.ui.Alignment.Center) {
        if (button.action != ButtonAction.EMPTY || button.emptyAppearance == EmptyAppearance.SLASH) ButtonGraphic(button, panel.iconPack, Modifier.size(if (container) cell * .55f else cell).clip(shape).alpha(panel.iconOpacity), tint, cell.value.toInt())
    }
}

@Composable
fun ButtonGraphic(button: PanelButton, pack: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface, sizeDp: Int = 48, imageTint: Color? = null) {
    if (button.action == ButtonAction.EMPTY) { Icon(AppIcons.Empty, null, modifier, tint = tint); return }
    button.icon.builtIn?.let { Icon(AppIcons.library(it), null, modifier, tint = tint); return }
    val app = LocalContext.current.applicationContext as NudgeApplication
    val density = LocalDensity.current.density
    val pixels = (sizeDp * density).toInt()
    val bitmap by key(button.action, button.appPackage, button.appActivity, button.icon, pack, pixels) {
        produceState(app.icons.cached(button, pack, pixels)?.asImageBitmap()) {
            value = app.icons.icon(button, pack, pixels)?.asImageBitmap()
        }
    }
    val imageFilter = imageTint?.let { ColorFilter.tint(it, BlendMode.SrcAtop) }
    if (bitmap != null) Image(bitmap!!, contentDescription = null, modifier = modifier, colorFilter = imageFilter)
    else when (button.action) {
        ButtonAction.NUDGE_SETTINGS -> Image(painterResource(R.drawable.ic_modukey), null, modifier, colorFilter = imageFilter)
        ButtonAction.PHONE_SETTINGS -> Icon(Icons.Outlined.Settings, null, modifier, tint = tint)
        ButtonAction.INPUT_METHOD_PICKER -> Icon(AppIcons.Keyboard, null, modifier, tint = tint)
        ButtonAction.CLOSE_PANELS -> Icon(AppIcons.ClosePanels, null, modifier, tint = tint)
        ButtonAction.SCREENSHOT -> Icon(AppIcons.Screenshot, null, modifier, tint = tint)
        ButtonAction.BRIGHTNESS_MAXIMUM -> Icon(AppIcons.BrightnessMaximum, null, modifier, tint = tint)
        ButtonAction.BRIGHTNESS_MINIMUM -> Icon(AppIcons.BrightnessMinimum, null, modifier, tint = tint)
        ButtonAction.TOGGLE_HANDLES -> Icon(AppIcons.Handle, null, modifier, tint = tint)
        ButtonAction.TOGGLE_ACCESSIBILITY -> Icon(ButtonVectors.action(button.action), null, modifier, tint = tint)
        ButtonAction.TOGGLE_HANDLE_VISIBILITY -> Icon(AppIcons.HandleVisibility, null, modifier, tint = tint)
        ButtonAction.TOGGLE_AND_HIDE_HANDLES -> Icon(ButtonVectors.action(button.action), null, modifier, tint = tint)
        ButtonAction.NOTIFICATIONS -> Icon(AppIcons.Notifications, null, modifier, tint = tint)
        ButtonAction.CONTROL_PANEL -> Icon(AppIcons.ControlPanel, null, modifier, tint = tint)
        ButtonAction.BRIGHTNESS_UP -> Icon(AppIcons.BrightnessUp, null, modifier, tint = tint)
        ButtonAction.BRIGHTNESS_DOWN -> Icon(AppIcons.BrightnessDown, null, modifier, tint = tint)
        ButtonAction.BRIGHTNESS_AUTOMATIC -> Icon(AppIcons.BrightnessAutomatic, null, modifier, tint = tint)
        else -> Canvas(modifier) {
            val u = size.minDimension / 24f
            fun p(x: Float, y: Float) = Offset(x * u, y * u)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(tint, p(x1, y1), p(x2, y2), 1.8f * u, StrokeCap.Round)
            when (button.action) {
                ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN, ButtonAction.MEDIA_MUTE -> {
                    val path = Path().apply { moveTo(3*u, 9*u); lineTo(7*u, 9*u); lineTo(12*u, 5*u); lineTo(12*u, 19*u); lineTo(7*u, 15*u); lineTo(3*u, 15*u); close() }
                    drawPath(path, tint)
                    if (button.action == ButtonAction.MEDIA_MUTE) { line(16f, 9f, 21f, 15f); line(21f, 9f, 16f, 15f) }
                    else { line(16f, 12f, 22f, 12f); if (button.action == ButtonAction.VOLUME_UP) line(19f, 9f, 19f, 15f) }
                }
                ButtonAction.SILENT -> { line(7f, 16f, 17f, 16f); line(7f, 16f, 8f, 7f); line(8f, 7f, 16f, 7f); line(16f, 7f, 17f, 16f); line(10f, 20f, 14f, 20f); line(3f, 3f, 21f, 21f) }
                ButtonAction.VIBRATE -> { drawRect(tint, p(8f, 4f), Size(8*u, 16*u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6f*u)); line(4f, 8f, 3f, 12f); line(3f, 12f, 4f, 16f); line(20f, 8f, 21f, 12f); line(21f, 12f, 20f, 16f) }
                ButtonAction.DND -> { drawCircle(tint, 9*u, p(12f, 12f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f*u)); line(7f, 12f, 17f, 12f) }
                ButtonAction.LOCK -> { drawRect(tint, p(5f, 10f), Size(14*u, 11*u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f*u)); drawArc(tint, 180f, 180f, false, p(8f, 2f), Size(8*u, 14*u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f*u)); line(12f, 14f, 12f, 17f) }
                else -> for (x in listOf(4f, 14f)) for (y in listOf(4f, 14f)) drawRect(tint, p(x,y), Size(6*u,6*u))
            }
        }
    }
}

internal class SquircleShape(private val fraction: Float) : Shape {
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


fun iconShape(panel: PanelConfig): Shape = RoundedCornerShape((panel.iconCornerFraction * 100).roundToInt())

fun contrastOn(background: Color): Color {
    val dark = Color(0xFF202223)
    val luminance = background.luminance()
    return if ((luminance + .05f) / (dark.luminance() + .05f) >= 1.05f / (luminance + .05f)) dark else Color.White
}
