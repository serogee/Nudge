package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable fun HandleGestureSettings(config: AppConfiguration, h: HandleConfig, vm: NudgeViewModel) {
    val s = h.trigger
    @Composable fun binding(slot: GestureSlot) = GestureBindingRow(config, slot, h.target(slot), vm) { value ->
        vm.editHandle(h.id) { it.copy(bindings = it.bindings + (slot to value)) }
    }
    Section(R.string.gestures) {
        GestureSlot.entries.forEach { binding(it) }
    }
    Section(R.string.swipe_settings) {
        SettingSlider(R.string.swipe_distance, s.swipeDistanceDp, 12f..80f, t(R.string.dp_value, s.swipeDistanceDp.roundToInt()), default = 24f) { v -> vm.editTrigger(h.id) { it.copy(swipeDistanceDp = v) } }
        SettingSlider(R.string.swipe_time, s.swipeTimeoutMs.toFloat(), 200f..1200f, t(R.string.ms_value, s.swipeTimeoutMs.toInt()), default = 600f) { v -> vm.editTrigger(h.id) { it.copy(swipeTimeoutMs = v.toLong()) } }
        GestureFeedbackControls(R.string.swipe_feedback, s.swipeFeedback) { v -> vm.editTrigger(h.id) { it.copy(swipeFeedback = v) } }
    }
    Section(R.string.swipe_hold_settings) {
        SettingSlider(R.string.swipe_hold_distance, s.swipeHoldDistanceDp, 12f..80f, t(R.string.dp_value, s.swipeHoldDistanceDp.roundToInt()), default = 24f) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldDistanceDp = v) } }
        SettingSlider(R.string.swipe_hold_swipe_time, s.swipeHoldTimeoutMs.toFloat(), 200f..1200f, t(R.string.ms_value, s.swipeHoldTimeoutMs.toInt()), default = 600f) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldTimeoutMs = v.toLong()) } }
        SettingSlider(R.string.swipe_hold_time, s.swipeHoldTimeMs.toFloat(), 300f..3000f, t(R.string.ms_value, s.swipeHoldTimeMs.toInt()), default = 500f) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldTimeMs = v.toLong()) } }
        OptionRow(t(R.string.execute_hold), HoldExecution.entries, s.swipeHoldExecution, HoldExecution.ON_RELEASE) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldExecution = v) } }
        GestureFeedbackControls(R.string.swipe_hold_feedback, s.swipeHoldFeedback) { v -> vm.editTrigger(h.id) { it.copy(swipeHoldFeedback = v) } }
    }
    Section(R.string.hold_settings) {
        SettingSlider(R.string.hold_time, s.holdTimeMs.toFloat(), 300f..3000f, t(R.string.ms_value, s.holdTimeMs.toInt()), default = 500f) { v -> vm.editTrigger(h.id) { it.copy(holdTimeMs = v.toLong()) } }
        OptionRow(t(R.string.execute_hold), HoldExecution.entries, s.holdExecution, HoldExecution.ON_RELEASE) { v -> vm.editTrigger(h.id) { it.copy(holdExecution = v) } }
        GestureFeedbackControls(R.string.hold_feedback, s.holdFeedback) { v -> vm.editTrigger(h.id) { it.copy(holdFeedback = v) } }
    }
    Section(R.string.tap_settings) {
        GestureFeedbackControls(R.string.tap_feedback, s.tapFeedback) { v -> vm.editTrigger(h.id) { it.copy(tapFeedback = v) } }
    }
    Section(R.string.swipe_sequence_settings) {
        SettingSlider(R.string.volume_sequence, h.volumeSequenceMs.toFloat(), 1000f..10000f, t(R.string.seconds_value, h.volumeSequenceMs / 1000f), default = 3000f) { v -> vm.editHandle(h.id) { it.copy(volumeSequenceMs = v.toLong()) } }
        Help(R.string.volume_sequence_help); Help(R.string.gesture_help); Help(R.string.hold_help)
    }
}

@Composable private fun GestureFeedbackControls(@StringRes title: Int, s: GestureFeedback, change: (GestureFeedback) -> Unit) {
    Toggle(title, R.string.gesture_feedback_help, s.enabled, default = false) { change(s.copy(enabled = it)) }
    SettingSlider(R.string.haptic_strength, s.strength, .1f..1f, t(R.string.percent, (s.strength * 100).roundToInt()), enabled = s.enabled, default = .5f) { change(s.copy(strength = it)) }
}
