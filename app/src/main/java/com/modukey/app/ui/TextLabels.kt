package com.modukey.app.ui

import android.content.Context
import androidx.annotation.StringRes
import com.modukey.app.R
import com.modukey.app.core.*

@StringRes fun label(value: Any): Int = when (value) {
    Edge.LEFT -> R.string.left; Edge.RIGHT -> R.string.right; Edge.BOTTOM -> R.string.bottom
    GestureSlot.SWIPE_1 -> R.string.swipe_1; GestureSlot.SWIPE_2 -> R.string.swipe_2; GestureSlot.SWIPE_3 -> R.string.swipe_3
    GestureSlot.SWIPE_HOLD -> R.string.swipe_hold; GestureSlot.TAP -> R.string.tap; GestureSlot.HOLD -> R.string.hold
    GestureTarget.NONE -> R.string.no_action; GestureTarget.VOLUME -> R.string.volume_popup
    PanelMode.FIXED -> R.string.fixed; PanelMode.SCROLLABLE -> R.string.scrollable
    PanelDirection.VERTICAL -> R.string.vertical; PanelDirection.HORIZONTAL -> R.string.horizontal
    OutsideTouch.BLOCK -> R.string.outside_block; OutsideTouch.PASS_THROUGH -> R.string.outside_pass
    ButtonAction.PHONE_SETTINGS -> R.string.phone_settings; ButtonAction.NUDGE_SETTINGS -> R.string.nudge_settings
    ButtonAction.VOLUME_UP -> R.string.volume_up; ButtonAction.VOLUME_DOWN -> R.string.volume_down; ButtonAction.MEDIA_MUTE -> R.string.media_mute
    ButtonAction.SILENT -> R.string.silent_ringer; ButtonAction.VIBRATE -> R.string.vibrate; ButtonAction.DND -> R.string.dnd
    ButtonAction.LOCK -> R.string.lock_screen; ButtonAction.OPEN_APP -> R.string.open_app
    IconShape.CIRCLE -> R.string.circle; IconShape.ROUNDED_SQUARE -> R.string.rounded_square; IconShape.SQUIRCLE -> R.string.squircle
    else -> error("Unlabelled option: $value")
}
fun Context.buttonLabel(button: PanelButton): String = if (button.action == ButtonAction.OPEN_APP && button.appLabel.isNotBlank()) button.appLabel else getString(label(button.action))
fun Context.handleLabel(config: AppConfiguration, handle: HandleConfig): String = handle.name.ifBlank { getString(R.string.handle_name, config.handles.indexOfFirst { it.id == handle.id } + 1) }
