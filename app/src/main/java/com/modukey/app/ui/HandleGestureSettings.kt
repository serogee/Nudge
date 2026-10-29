package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable fun HandleGestureSettings(config: AppConfiguration, h: HandleConfig, vm: NudgeViewModel, landscape: Boolean = false) {
    val s = h.trigger
    val terminal = h.terminalWheelStage(config, landscape)
    val continuous = h.hasContinuousSwipe || (0..2).any { h.target(GestureSlot.entries[it]).dragsWheel(config, landscape) }
    @Composable fun binding(slot: GestureSlot) = GestureBindingRow(config, slot, h.target(slot), vm,
        enabled = !(slot == GestureSlot.SWIPE_HOLD && continuous) && !(slot.ordinal < 3 && terminal != null && slot.ordinal > terminal)) { value ->
        vm.editHandle(h.id) { it.copy(bindings = it.bindings + (slot to value)) }
    }
    Section(R.string.gestures) {
        GestureSlot.entries.forEach { binding(it) }
        if (continuous) Help(R.string.continuous_swipe_hold_conflict)
        if (terminal != null) Help(R.string.wheel_later_disabled)
    }
    SliderTravelControls(h, vm)
    CollapsibleSection(R.string.swipe_settings) {
        OptionRow(t(R.string.swipe_hold_distance_mode), SwipeDistanceMode.entries, s.swipeDistanceMode, SwipeDistanceMode.INWARD_ONLY) { v -> vm.editTrigger(h.id) { it.copy(swipeDistanceMode = v) } }
        OptionInfo(R.string.swipe_distance_mode_help)
        SettingSlider(R.string.swipe_distance, s.swipeDistanceDp, 12f..80f, t(R.string.dp_value, s.swipeDistanceDp.roundToInt()), default = 24f) { v -> vm.editTrigger(h.id) { it.copy(swipeDistanceDp = v) } }
        DurationInput(R.string.swipe_time, s.swipeTimeoutMs, 200L..1200L, default = 600) { v -> vm.editTrigger(h.id) { it.copy(swipeTimeoutMs = v) } }
        GestureFeedbackControls(R.string.swipe_feedback, s.swipeFeedback) { v -> vm.editTrigger(h.id) { it.copy(swipeFeedback = v) } }
    }
    CollapsibleSection(R.string.swipe_hold_settings) {
        OptionRow(t(R.string.swipe_hold_distance_mode), SwipeHoldDistanceMode.entries, s.swipeHoldDistanceMode, SwipeHoldDistanceMode.ANY_DIRECTION) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldDistanceMode = v) } }
        OptionInfo(R.string.swipe_hold_distance_mode_help)
        SettingSlider(R.string.swipe_hold_distance, s.swipeHoldDistanceDp, 12f..80f, t(R.string.dp_value, s.swipeHoldDistanceDp.roundToInt()), default = 24f) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldDistanceDp = v) } }
        DurationInput(R.string.swipe_hold_swipe_time, s.swipeHoldTimeoutMs, 200L..1200L, default = 600) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldTimeoutMs = v) } }
        DurationInput(R.string.swipe_hold_time, s.swipeHoldTimeMs, 300L..3000L, default = 500) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldTimeMs = v) } }
        OptionRow(t(R.string.execute_hold), HoldExecution.entries, s.swipeHoldExecution, HoldExecution.ON_RELEASE) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldExecution = v) } }
        GestureFeedbackControls(R.string.swipe_hold_feedback, s.swipeHoldFeedback) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldFeedback = v) } }
    }
    CollapsibleSection(R.string.hold_settings) {
        DurationInput(R.string.hold_time, s.holdTimeMs, 300L..3000L, default = 500) { v -> vm.editTrigger(h.id) { it.copy(holdTimeMs = v) } }
        OptionRow(t(R.string.execute_hold), HoldExecution.entries, s.holdExecution, HoldExecution.ON_RELEASE) { v -> vm.editTrigger(h.id) { it.copy(holdExecution = v) } }
        GestureFeedbackControls(R.string.hold_feedback, s.holdFeedback) { v -> vm.editTrigger(h.id) { it.copy(holdFeedback = v) } }
    }
    CollapsibleSection(R.string.tap_settings) {
        GestureFeedbackControls(R.string.tap_feedback, s.tapFeedback) { v -> vm.editTrigger(h.id) { it.copy(tapFeedback = v) } }
    }
    CollapsibleSection(R.string.swipe_sequence_settings) {
        DurationInput(R.string.volume_sequence, h.volumeSequenceMs, 1000L..10000L, default = 3000) { v -> vm.editHandle(h.id) { it.copy(volumeSequenceMs = v) } }
        OptionInfo(R.string.volume_sequence_help); OptionInfo(R.string.gesture_help); OptionInfo(R.string.hold_help)
    }
}

@Composable private fun GestureFeedbackControls(@StringRes title: Int, s: GestureFeedback, change: (GestureFeedback) -> Unit) {
    Toggle(title, R.string.gesture_feedback_help, s.enabled, default = false, optionsExpanded = s.enabled) { change(s.copy(enabled = it)) }
    DependentSettings(title) {
        SettingSlider(R.string.haptic_strength, s.strength, .1f..1f, t(R.string.percent, (s.strength * 100).roundToInt()), enabled = s.enabled, default = .5f, valueScale = 100f) { change(s.copy(strength = it)) }
    }
}
