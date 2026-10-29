package com.modukey.app.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable internal fun SliderTravelControls(handle: HandleConfig, vm: NudgeViewModel) {
    val kinds = GestureSlot.entries.filter { it != GestureSlot.TAP }.map { handle.target(it).normalized() }
        .filter { it.slideToAdjust }.map { if (it.volume) SliderKind.VOLUME else SliderKind.BRIGHTNESS }.toSet()
    var guide by rememberSaveable(handle.id) { mutableStateOf<SliderKind?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val selected = guide?.takeIf { it in kinds }
    val request = selected?.let { SliderGuideRequest(handle.id, it,
        if (it == SliderKind.VOLUME) handle.volumeTravelDp else handle.brightnessTravelDp) }
    DisposableEffect(lifecycle, request) {
        val token = Any()
        fun update() = vm.previewSliderGuide(token, request?.takeIf { lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) })
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(observer); update()
        onDispose { vm.previewSliderGuide(token, null); lifecycle.removeObserver(observer) }
    }
    if (kinds.isNotEmpty()) CollapsibleSection(R.string.slider_travel) {
        OptionInfo(R.string.slider_travel_help)
        kinds.forEach { kind ->
            val volume = kind == SliderKind.VOLUME
            val travel = if (volume) handle.volumeTravelDp else handle.brightnessTravelDp
            SettingSlider(if (volume) R.string.volume_travel else R.string.brightness_travel, travel, 48f..600f,
                t(R.string.dp_value, travel.roundToInt()), default = 240f) { value ->
                vm.editHandle(handle.id) { if (volume) it.copy(volumeTravelDp = value) else it.copy(brightnessTravelDp = value) }
            }
            Toggle(if (volume) R.string.volume_travel_guide else R.string.brightness_travel_guide,
                R.string.slider_guide_help, selected == kind, default = false) { guide = if (it) kind else null }
        }
    }
}
