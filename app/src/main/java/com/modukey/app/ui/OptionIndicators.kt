package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.ButtonAction

/** Only action-specific controls earn a badge; common icon editing does not. */
internal fun ButtonAction.hasAdditionalOptions(volumePopupOptions: Boolean = true) = when (this) {
    ButtonAction.OPEN_APP, ButtonAction.EMPTY, ButtonAction.BRIGHTNESS_MAXIMUM, ButtonAction.BRIGHTNESS_MINIMUM,
    ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.TOGGLE_AND_HIDE_HANDLES -> true
    ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN -> volumePopupOptions
    else -> false
}

@Composable internal fun OptionsIndicator(expanded: Boolean, enabled: Boolean = true) {
    Icon(if (expanded) AppIcons.ChevronUp else AppIcons.Down,
        t(if (expanded) R.string.additional_options_shown else R.string.additional_options_available),
        Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) .7f else .38f))
}

@Composable internal fun ActionOptionsBadge(action: ButtonAction, background: Color, modifier: Modifier = Modifier) {
    val shape = remember { SquircleShape(.4f) }
    Surface(modifier.size(18.dp), shape = shape, color = background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Settings, t(R.string.action_has_options, t(label(action))), Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun DependentSettings(@StringRes parent: Int, content: @Composable ColumnScope.() -> Unit) =
    DependentSettings(t(parent), content)

/** A bent arrow connects a group to its parent without adding a second control. */
@Composable internal fun DependentSettings(parent: String, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(AppIcons.Dependent, t(R.string.options_for_setting, parent), Modifier.padding(top = 12.dp).size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
