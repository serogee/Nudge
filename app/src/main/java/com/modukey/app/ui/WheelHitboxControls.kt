package com.modukey.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable internal fun WheelHitboxControls(settings: WheelHitboxSettings, reference: Pair<Float, Float> = 32f to 240f,
    change: ((WheelHitboxSettings) -> WheelHitboxSettings) -> Unit) {
    OptionRow(t(R.string.wheel_hitbox), WheelHitboxMode.entries, settings.mode, WheelHitboxMode.AUTOMATIC) { mode -> change { it.withMode(mode, reference) } }
    Help(when (settings.mode) {
        WheelHitboxMode.AUTOMATIC -> R.string.wheel_hitbox_automatic_help
        WheelHitboxMode.BUTTON_SIZE -> R.string.wheel_hitbox_button_help
        WheelHitboxMode.MANUAL -> R.string.wheel_hitbox_manual_help
    })
    if (settings.mode != WheelHitboxMode.MANUAL) return
    val s = settings.normalized()
    val defaults = WheelHitboxSettings()
    var editing by remember { mutableStateOf<Int?>(null) }
    val names = listOf(t(R.string.wheel_hitbox_inner), t(R.string.wheel_hitbox_outer))
    val values = listOf(s.innerRadiusDp, s.outerRadiusDp)
    fun limits(index: Int) = if (index == 0) 0f..(s.outerRadiusDp - 8f) else (s.innerRadiusDp + 8f)..600f
    fun set(index: Int, value: Float) = change { current ->
        (if (index == 0) current.copy(innerRadiusDp = value) else current.copy(outerRadiusDp = value)).normalized()
    }
    DependentSettings(R.string.wheel_hitbox) {
        SettingTitle(t(R.string.wheel_hitbox_radii), reset = if (values != listOf(defaults.innerRadiusDp, defaults.outerRadiusDp)) ({
            change { it.copy(innerRadiusDp = defaults.innerRadiusDp, outerRadiusDp = defaults.outerRadiusDp) }
        }) else null)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            names.forEachIndexed { index, name ->
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clickable { editing = index }
                    .semanticsDescription(t(R.string.edit_setting_value, name)).padding(vertical = 12.dp)) {
                    Text(t(R.string.wheel_radius_value, name, values[index].roundToInt()))
                }
            }
        }
        DragSlider(values, 0f..600f, names, limits = ::limits, finish = LocalFinishAdjustment.current, change = ::set)
    }
    editing?.let { index ->
        val finish = LocalFinishAdjustment.current
        SliderValueDialog(names[index], values[index], limits(index), 1f, dismiss = { editing = null }) {
            set(index, it); finish(); editing = null
        }
    }
}
