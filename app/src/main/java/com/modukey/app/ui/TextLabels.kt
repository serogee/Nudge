package com.modukey.app.ui

import android.content.Context
import androidx.annotation.StringRes
import com.modukey.app.R
import com.modukey.app.core.*

@StringRes fun label(value: Any): Int = when (value) {
    SliderActivation.AFTER_HOLD -> R.string.slider_after_hold; SliderActivation.IMMEDIATE -> R.string.slider_immediate
    BrightnessTouchBehavior.JUMP_TO_TOUCH -> R.string.brightness_jump_to_touch
    BrightnessTouchBehavior.SLIDE_TO_ADJUST -> R.string.brightness_slide_to_adjust
    BrightnessSide.OPPOSITE_VOLUME -> R.string.opposite_volume; BrightnessSide.LEFT, VolumeSide.LEFT -> R.string.left
    BrightnessSide.RIGHT, VolumeSide.RIGHT -> R.string.right; BrightnessSide.CUSTOM -> R.string.slider_custom
    HoldExecution.ON_RELEASE -> R.string.hold_on_release; HoldExecution.AFTER_TIME -> R.string.hold_after_time
    Edge.LEFT -> R.string.left; Edge.RIGHT -> R.string.right; Edge.BOTTOM -> R.string.bottom
    GestureSlot.SWIPE_1 -> R.string.swipe_1; GestureSlot.SWIPE_2 -> R.string.swipe_2; GestureSlot.SWIPE_3 -> R.string.swipe_3
    GestureSlot.SWIPE_HOLD -> R.string.swipe_hold; GestureSlot.TAP -> R.string.tap; GestureSlot.HOLD -> R.string.hold
    WheelFocusAnimation.NONE -> R.string.animation_none; WheelFocusAnimation.SMOOTH -> R.string.animation_smooth
    WheelFocusAnimation.SPRING -> R.string.animation_spring
    WheelHitboxMode.AUTOMATIC -> R.string.wheel_hitbox_automatic
    WheelHitboxMode.BUTTON_SIZE -> R.string.wheel_hitbox_button
    WheelHitboxMode.MANUAL -> R.string.wheel_hitbox_manual
    PanelMode.FIXED -> R.string.fixed; PanelMode.SCROLLABLE -> R.string.scrollable; PanelMode.WHEEL -> R.string.wheel
    PanelDirection.VERTICAL -> R.string.vertical; PanelDirection.HORIZONTAL -> R.string.horizontal
    OutsideTouch.BLOCK -> R.string.outside_block; OutsideTouch.PASS_THROUGH -> R.string.outside_pass; OutsideTouch.KEEP_OPEN -> R.string.outside_keep
    ButtonAction.PHONE_SETTINGS -> R.string.phone_settings; ButtonAction.NUDGE_SETTINGS -> R.string.nudge_settings
    ButtonAction.VOLUME_UP -> R.string.volume_up; ButtonAction.VOLUME_DOWN -> R.string.volume_down; ButtonAction.MEDIA_MUTE -> R.string.media_mute
    ButtonAction.SILENT -> R.string.silent_ringer; ButtonAction.VIBRATE -> R.string.vibrate; ButtonAction.DND -> R.string.dnd
    ButtonAction.LOCK -> R.string.lock_screen; ButtonAction.OPEN_APP -> R.string.open_app
    ButtonAction.INPUT_METHOD_PICKER -> R.string.input_method_picker
    ButtonAction.CLOSE_PANELS -> R.string.close_panels; ButtonAction.SCREENSHOT -> R.string.screenshot
    ButtonAction.BRIGHTNESS_MAXIMUM -> R.string.brightness_maximum; ButtonAction.BRIGHTNESS_MINIMUM -> R.string.brightness_minimum
    ButtonAction.BRIGHTNESS_AUTOMATIC -> R.string.brightness_automatic
    ButtonAction.TOGGLE_HANDLES -> R.string.toggle_handles
    ButtonAction.TOGGLE_ACCESSIBILITY -> R.string.toggle_accessibility
    ButtonAction.TOGGLE_HANDLE_VISIBILITY -> R.string.toggle_handle_visibility
    ButtonAction.TOGGLE_AND_HIDE_HANDLES -> R.string.toggle_and_hide_handles
    ButtonAction.NOTIFICATIONS -> R.string.notifications; ButtonAction.CONTROL_PANEL -> R.string.control_panel
    ButtonAction.BRIGHTNESS_UP -> R.string.brightness_up; ButtonAction.BRIGHTNESS_DOWN -> R.string.brightness_down
    HandleToggleScope.ALL -> R.string.all_handles; HandleToggleScope.CURRENT -> R.string.current_handle; HandleToggleScope.OTHERS -> R.string.other_handles
    HandleGroupScope.NONE -> R.string.no_handles_target; HandleGroupScope.ALL -> R.string.all_handles
    HandleGroupScope.CURRENT -> R.string.current_handle; HandleGroupScope.OTHERS -> R.string.other_handles
    SwipeHoldDistanceMode.ANY_DIRECTION -> R.string.any_direction; SwipeHoldDistanceMode.INWARD_ONLY -> R.string.inward_only
    ButtonAction.EMPTY -> R.string.empty_button
    EmptyAppearance.NO_ICON -> R.string.empty_no_icon; EmptyAppearance.NO_BUTTON -> R.string.empty_no_button; EmptyAppearance.SLASH -> R.string.empty_slash
    PanelAnimation.NONE -> R.string.animation_none; PanelAnimation.FADE -> R.string.animation_fade
    PanelAnimation.SLIDE -> R.string.animation_slide; PanelAnimation.SCALE -> R.string.animation_scale
    IconContainer.DEFAULT -> R.string.container_default; IconContainer.CONTAINER -> R.string.with_container; IconContainer.STANDALONE -> R.string.standalone_icon
    BuiltInIcon.CAMERA -> R.string.camera; BuiltInIcon.WALLET -> R.string.wallet; BuiltInIcon.PHONE -> R.string.phone
    BuiltInIcon.FLASHLIGHT -> R.string.flashlight; BuiltInIcon.MUSIC -> R.string.music; BuiltInIcon.HEADPHONES -> R.string.headphones
    BuiltInIcon.CALENDAR -> R.string.calendar; BuiltInIcon.CLOCK -> R.string.clock; BuiltInIcon.MAP -> R.string.map
    BuiltInIcon.HOME -> R.string.home; BuiltInIcon.FAVORITE -> R.string.favorite; BuiltInIcon.APPS -> R.string.apps
    BuiltInIcon.NUDGE -> R.string.app_name
    else -> error("Unlabelled option: $value")
}
fun Context.buttonLabel(button: PanelButton): String = if (button.action == ButtonAction.OPEN_APP && button.appLabel.isNotBlank()) button.appLabel else getString(label(button.action))
fun Context.handleLabel(config: AppConfiguration, handle: HandleConfig): String = handle.name.ifBlank { getString(R.string.handle_name, config.handles.indexOfFirst { it.id == handle.id } + 1) }
fun Context.panelLabel(panel: SavedPanel): String = panel.name.ifBlank { getString(if (panel.isWheel) R.string.untitled_wheel else R.string.untitled_panel) }
fun Context.targetLabel(config: AppConfiguration, target: GestureTarget): String {
    var name = target.action?.let { getString(R.string.action_target, buttonLabel(it)) }
        ?: config.panel(target.panelId)?.let { getString(if (it.isWheel) R.string.wheel_target else R.string.panel_target, panelLabel(it)) }
    name = if (target.volume) name?.let { getString(R.string.panel_with_volume, it) } ?: getString(R.string.volume_popup) else name
    return if (target.brightness) name?.let { getString(R.string.target_with_brightness, it) } ?: getString(R.string.brightness_slider)
        else name ?: getString(R.string.no_action)
}

@StringRes fun availabilityLabel(value: HandleAvailability?, enabled: Boolean) = when(value) {
    HandleAvailability.ACTIVE -> R.string.active; HandleAvailability.DISABLED -> R.string.disabled; HandleAvailability.PAUSED -> R.string.paused
    HandleAvailability.SCREEN_OFF -> R.string.hidden_screen_off; HandleAvailability.LOCKED -> R.string.hidden_locked; HandleAvailability.LANDSCAPE -> R.string.hidden_landscape
    HandleAvailability.EXCLUDED -> R.string.hidden_excluded; HandleAvailability.EDITING -> R.string.editing; HandleAvailability.ERROR -> R.string.overlay_error
    null -> if (enabled) R.string.pending else R.string.disabled
}
