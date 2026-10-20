package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import kotlin.math.roundToInt

val LocalFinishAdjustment = staticCompositionLocalOf<() -> Unit> { {} }
@Composable fun t(@StringRes id: Int, vararg args: Any): String = stringResource(id, *args)
fun Modifier.semanticsDescription(value: String) = semantics { contentDescription = value }
@Composable fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) = Section(t(title), content)
@Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); content()
        }
    }
}
@Composable fun CollapsibleSection(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    val label = t(title)
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val description = t(if (expanded) R.string.collapse_section else R.string.expand_section, label)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) AppIcons.ChevronUp else AppIcons.Down, description, Modifier.size(20.dp))
                }
            }
            if (expanded) content()
        }
    }
}
@Composable fun Help(@StringRes text: Int) { Text(t(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun Notice(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) { Text(text, Modifier.padding(12.dp)) } }
@Composable fun EditIcon(icon: ImageVector, @StringRes description: Int, enabled: Boolean = true, action: () -> Unit) {
    IconButton(onClick = action, enabled = enabled) { Icon(icon, t(description), Modifier.size(20.dp)) }
}
@Composable fun SettingTitle(title: String, enabled: Boolean = true, reset: (() -> Unit)? = null) {
    val finish = LocalFinishAdjustment.current
    Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f), modifier = Modifier.weight(1f, fill = false))
        if (reset != null) {
            val description = t(R.string.reset_setting, title)
            IconButton(onClick = { reset(); finish() }, enabled = enabled, modifier = Modifier.size(40.dp)) {
                Icon(AppIcons.Reset, description, Modifier.size(16.dp))
            }
        }
    }
}
@Composable fun Toggle(@StringRes title: Int, @StringRes detail: Int?, checked: Boolean, default: Boolean? = null, enabled: Boolean = true, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Switch) { change(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            SettingTitle(t(title), enabled = enabled, reset = if (default != null && checked != default) ({ change(default) }) else null)
            detail?.let { Help(it) }
        }
        val description = t(title)
        Switch(checked = checked, onCheckedChange = change, enabled = enabled, modifier = Modifier.semantics { contentDescription = description })
    }
}
@Composable fun SettingSlider(@StringRes title: Int, value: Float, range: ClosedFloatingPointRange<Float>, display: String,
    enabled: Boolean = true, default: Float? = null, change: (Float) -> Unit) {
    val finish = LocalFinishAdjustment.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SettingTitle(t(title), enabled, if (default != null && value != default) ({ change(default) }) else null) }
            Text(display, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f), style = MaterialTheme.typography.labelMedium)
        }
        val name = t(title)
        Slider(value = value.coerceIn(range), onValueChange = change, onValueChangeFinished = finish, valueRange = range,
            enabled = enabled, modifier = Modifier.semantics { contentDescription = name })
    }
}
@Composable fun PercentSlider(@StringRes title: Int, value: Float, enabled: Boolean = true, default: Float? = null, change: (Float) -> Unit) =
    SettingSlider(title, value, 0f..1f, t(R.string.percent, (value*100).roundToInt()), enabled, default, change)
@Composable fun <T> Choices(values: List<T>, selected: T, change: (T) -> Unit) {
    val c = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { v -> FilterChip(selected = v == selected, onClick = { change(v) }, label = { Text(optionText(c, v as Any)) }) } }
}
fun optionText(context: android.content.Context, value: Any): String = context.getString(label(value))
@Composable fun <T> OptionRow(title: String, values: List<T>, selected: T, default: T? = null, change: (T) -> Unit) {
    val c = LocalContext.current; var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        SettingTitle(title, reset = if (default != null && selected != default) ({ change(default) }) else null)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(optionText(c, selected as Any), Modifier.weight(1f)); Icon(AppIcons.Down, null, Modifier.size(18.dp))
            }
            DropdownMenu(expanded, { expanded = false }) {
                values.forEach { v -> DropdownMenuItem(text = { Text(optionText(c, v as Any)) }, onClick = { expanded = false; change(v) }) }
            }
        }
    }
}
@Composable fun BrowserTabs(titles: List<String>, selected: Int, select: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        titles.forEachIndexed { index, title ->
            val active = selected == index
            Surface(modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                color = if (active) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow) {
                Tab(selected = active, onClick = { select(index) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(title, Modifier.padding(horizontal = 4.dp, vertical = 14.dp), style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
@Composable fun ColorPicker(color: Long, enabled: Boolean = true, @StringRes title: Int = R.string.color, default: Long? = null, change: (Long) -> Unit) {
    var hex by remember(color) { mutableStateOf("%06X".format(color and 0xFFFFFF)) }
    var selecting by remember { mutableStateOf(false) }
    val c = LocalContext.current
    val label = t(title)
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { SettingTitle(label, enabled, if (default != null && color != default) ({ change(default) }) else null) }
        Surface(Modifier.size(48.dp, 32.dp).clickable(enabled = enabled, role = Role.Button) { selecting = true }
            .semantics { contentDescription = c.getString(R.string.edit_color, label) }, shape = MaterialTheme.shapes.small,
            color = Color(color), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else .38f))) {}
    }
    if (!selecting) return
    val palettes = listOf(listOf(0xFFB8D982L,0xFF6BB7E8L,0xFFE3A0CFL,0xFFF1BA70L,0xFFFFFFFFL,0xFF252A21L),
        listOf(0xFF202223L,0xFF6F6F6FL,0xFF9D9F9FL,0xFF3C3C3EL,0xFF3482FFL,0xFFE8E8E8L),
        listOf(0xFF202122L))
    AlertDialog(onDismissRequest = { selecting = false }, title = { Text(label) },
        text = { Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = hex, onValueChange = { hex = it.removePrefix("#").take(6); if (hex.matches(Regex("[0-9a-fA-F]{6}"))) change(0xFF000000 or hex.toLong(16)) },
                label = { Text(t(R.string.hex_color)) }, prefix = { Text("#") }, singleLine = true,
                isError = !hex.matches(Regex("[0-9a-fA-F]{6}")), modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                palettes.flatten().distinct().forEach { v -> FilledIconButton(onClick = { change(v) },
                    modifier = Modifier.semantics { contentDescription = c.getString(R.string.color_description, "%06X".format(v and 0xFFFFFF)) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(v))) {
                    if (v == color) Icon(AppIcons.Check, null, Modifier.size(18.dp), tint = contrastOn(Color(v)))
                } }
            }
        } }, confirmButton = { TextButton(onClick = { selecting = false }) { Text(t(R.string.done)) } })
}
