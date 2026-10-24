package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.modukey.app.R
import com.modukey.app.core.WheelLayoutSettings
import kotlin.math.roundToInt

@Composable internal fun WheelRadiusControl(settings: WheelLayoutSettings, buttonSize: Float, change: (Float, Float) -> Unit) {
    val (inner, outer) = settings.radii(buttonSize)
    val defaults = WheelLayoutSettings().radii(56f)
    SettingTitle(t(R.string.wheel_radius), reset = if (inner to outer != defaults) ({ change(defaults.first, defaults.second) }) else null)
    val names = listOf(t(R.string.wheel_inner_radius), t(R.string.wheel_outer_radius))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${names[0]}: ${t(R.string.dp_value, inner.roundToInt())}")
        Text("${names[1]}: ${t(R.string.dp_value, outer.roundToInt())}")
    }
    DragSlider(listOf(inner, outer), 12f..336f, names,
        limits = { if (it == 0) 12f..(outer - 48f) else (inner + 48f)..336f }, finish = LocalFinishAdjustment.current) { index, value ->
        if (index == 0) change(value, outer) else change(inner, value)
    }
}
