package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.GestureTarget
import kotlin.math.roundToInt

val LocalFinishAdjustment = staticCompositionLocalOf<() -> Unit> { {} }
@Composable fun t(@StringRes id: Int, vararg args: Any): String = stringResource(id, *args)
@Composable fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) = Section(t(title), content)
@Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); content() }
    }
}
@Composable fun Help(@StringRes text: Int) { Text(t(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun Notice(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) { Text(text, Modifier.padding(12.dp)) } }
@Composable fun Toggle(@StringRes title: Int, @StringRes detail: Int?, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { change(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(t(title)); detail?.let { Help(it) } }
        Switch(checked = checked, onCheckedChange = change)
    }
}
@Composable fun SettingSlider(@StringRes title: Int, value: Float, range: ClosedFloatingPointRange<Float>, display: String, enabled: Boolean = true, change: (Float) -> Unit) {
    val finish = LocalFinishAdjustment.current
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(t(title), color = color); Text(display, color = color, style = MaterialTheme.typography.labelMedium) }
        Slider(value = value.coerceIn(range), onValueChange = change, onValueChangeFinished = finish, valueRange = range, enabled = enabled)
    }
}
@Composable fun PercentSlider(@StringRes title: Int, value: Float, enabled: Boolean = true, change: (Float) -> Unit) = SettingSlider(title, value, 0f..1f, t(R.string.percent, (value*100).roundToInt()), enabled, change)
@Composable fun <T> Choices(values: List<T>, selected: T, change: (T) -> Unit) {
    val c = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { v -> FilterChip(selected = v == selected, onClick = { change(v) }, label = { Text(optionText(c, v as Any)) }) } }
}
fun optionText(context: android.content.Context, value: Any): String = if (value is GestureTarget && value.panelIndex != null) context.getString(R.string.panel_name, value.panelIndex!! + 1) else context.getString(label(value))
@Composable fun <T> OptionRow(title: String, values: List<T>, selected: T, change: (T) -> Unit) {
    val c = LocalContext.current; var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, Modifier.weight(1f))
        Box { TextButton(onClick = { expanded = true }) { Text(optionText(c, selected as Any)) }
            DropdownMenu(expanded, { expanded = false }) { values.forEach { v -> DropdownMenuItem(text = { Text(optionText(c, v as Any)) }, onClick = { expanded = false; change(v) }) } }
        }
    }
}
@Composable fun ColorPicker(color: Long, enabled: Boolean = true, change: (Long) -> Unit) {
    var hex by remember(color) { mutableStateOf("%06X".format(color and 0xFFFFFF)) }
    val c = LocalContext.current
    Text(t(R.string.color), color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0xFFB8D982L, 0xFF6BB7E8L, 0xFFE3A0CFL, 0xFFF1BA70L, 0xFFFFFFFFL, 0xFF252A21L).forEach { v ->
            FilledIconButton(onClick = { change(v) }, enabled = enabled, modifier = Modifier.semantics { contentDescription = c.getString(R.string.color_description, "%06X".format(v and 0xFFFFFF)) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(v))) { if (v == color) Text("✓", color = Color.Black) }
        }
    }
    OutlinedTextField(value = hex, onValueChange = { hex = it.removePrefix("#").take(6); if (hex.matches(Regex("[0-9a-fA-F]{6}"))) change(0xFF000000 or hex.toLong(16)) },
        enabled = enabled, label = { Text(t(R.string.hex_color)) }, prefix = { Text("#") }, singleLine = true,
        isError = enabled && !hex.matches(Regex("[0-9a-fA-F]{6}")), modifier = Modifier.fillMaxWidth())
}
