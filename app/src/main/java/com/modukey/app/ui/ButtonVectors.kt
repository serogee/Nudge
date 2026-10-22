package com.modukey.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import com.modukey.app.core.ButtonAction

/** Static action vectors also provide full-resolution launcher shortcut icons. */
object ButtonVectors {
    private fun icon(name: String, draw: PathBuilder.() -> Unit) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
    }.build()
    private fun speaker(draw: PathBuilder.() -> Unit) = icon("Volume") {
        moveTo(3f,9f); lineTo(7f,9f); lineTo(12f,5f); lineTo(12f,19f); lineTo(7f,15f); lineTo(3f,15f); close(); draw()
    }
    private val volumeUp = speaker { moveTo(16f,12f); lineTo(22f,12f); moveTo(19f,9f); lineTo(19f,15f) }
    private val volumeDown = speaker { moveTo(16f,12f); lineTo(22f,12f) }
    private val mute = speaker { moveTo(16f,9f); lineTo(21f,15f); moveTo(21f,9f); lineTo(16f,15f) }
    private val silent = icon("Silent") { moveTo(7f,16f); lineTo(17f,16f); lineTo(16f,7f); lineTo(8f,7f); close(); moveTo(10f,20f); lineTo(14f,20f); moveTo(3f,3f); lineTo(21f,21f) }
    private val vibrate = icon("Vibrate") { moveTo(8f,4f); lineTo(16f,4f); lineTo(16f,20f); lineTo(8f,20f); close(); moveTo(4f,8f); lineTo(3f,12f); lineTo(4f,16f); moveTo(20f,8f); lineTo(21f,12f); lineTo(20f,16f) }
    private val dnd = icon("Do Not Disturb") { moveTo(21f,12f); curveTo(21f,24f,3f,24f,3f,12f); curveTo(3f,0f,21f,0f,21f,12f); moveTo(7f,12f); lineTo(17f,12f) }
    private val lock = icon("Lock") { moveTo(5f,10f); lineTo(19f,10f); lineTo(19f,21f); lineTo(5f,21f); close(); moveTo(8f,10f); lineTo(8f,7f); curveTo(8f,0f,16f,0f,16f,7f); lineTo(16f,10f); moveTo(12f,14f); lineTo(12f,17f) }
    fun action(action: ButtonAction): ImageVector = when (action) {
        ButtonAction.PHONE_SETTINGS -> Icons.Outlined.Settings
        ButtonAction.NUDGE_SETTINGS -> AppIcons.Nudge
        ButtonAction.VOLUME_UP -> volumeUp; ButtonAction.VOLUME_DOWN -> volumeDown; ButtonAction.MEDIA_MUTE -> mute
        ButtonAction.SILENT -> silent; ButtonAction.VIBRATE -> vibrate; ButtonAction.DND -> dnd; ButtonAction.LOCK -> lock
        ButtonAction.INPUT_METHOD_PICKER -> AppIcons.Keyboard; ButtonAction.CLOSE_PANELS -> AppIcons.ClosePanels
        ButtonAction.SCREENSHOT -> AppIcons.Screenshot; ButtonAction.NOTIFICATIONS -> AppIcons.Notifications; ButtonAction.CONTROL_PANEL -> AppIcons.ControlPanel
        ButtonAction.BRIGHTNESS_MAXIMUM -> AppIcons.BrightnessMaximum; ButtonAction.BRIGHTNESS_MINIMUM -> AppIcons.BrightnessMinimum
        ButtonAction.BRIGHTNESS_UP -> AppIcons.BrightnessUp; ButtonAction.BRIGHTNESS_DOWN -> AppIcons.BrightnessDown
        ButtonAction.BRIGHTNESS_AUTOMATIC -> AppIcons.BrightnessAutomatic
        ButtonAction.TOGGLE_HANDLES -> AppIcons.Handle; ButtonAction.OPEN_APP -> AppIcons.Apps; ButtonAction.EMPTY -> AppIcons.Empty
        ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.TOGGLE_AND_HIDE_HANDLES -> AppIcons.HandleVisibility
    }
}
