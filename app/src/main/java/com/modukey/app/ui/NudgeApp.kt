package com.modukey.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.modukey.app.BuildConfig
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.ConnectionState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LocalFinishAdjustment = staticCompositionLocalOf<() -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeApp(vm: NudgeViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val service by vm.status.collectAsStateWithLifecycle()
    val accessibility by vm.accessibilityEnabled.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tell: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    val openSettings: (String) -> Unit = { action ->
        val intent = Intent(action).apply {
            if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) data = "package:${context.packageName}".toUri()
        }
        try { context.startActivity(intent) }
        catch (_: android.content.ActivityNotFoundException) { tell("Open your phone's Settings app and follow the instructions below.") }
    }
    DisposableEffect(lifecycle, tab, vm) {
        val token = Any()
        fun resume() { vm.refreshPermissions(); if (tab == 1) vm.beginCustomization(token) }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE -> { vm.endCustomization(token); vm.flush() }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        onDispose { vm.endCustomization(token); vm.flush(); lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalFinishAdjustment provides vm::flush) {
    NudgeTheme {
        Scaffold(
            topBar = { TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.ic_modukey), contentDescription = null,
                        modifier = Modifier.size(36.dp))
                    Text("Nudge", fontWeight = FontWeight.Bold)
                }
            }, actions = { Text("${BuildConfig.VERSION_NAME}  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }) },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Customize", "Behavior & help").forEachIndexed { index, title ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, label = { Text(title) }, icon = {
                            Icon(when (index) { 0 -> Icons.Outlined.Home; 1 -> Icons.Outlined.Settings; else -> Icons.Outlined.Info }, contentDescription = null)
                        })
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val s = settings
            if (s == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    if (error == null) CircularProgressIndicator() else Column(Modifier.padding(24.dp)) {
                        Text(error!!)
                        OutlinedButton(onClick = vm::retrySaving) { Text("Retry reading settings") }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    error?.let {
                        Notice(it, error = true)
                        OutlinedButton(onClick = vm::retrySaving) { Text("Retry saving") }
                    }
                    service.error?.let { Notice(it, error = true) }
                    when (tab) {
                        0 -> HomeScreen(s, vm, accessibility, openSettings, tell)
                        1 -> CustomizeScreen(s, vm)
                        else -> BehaviorScreen(s, vm, openSettings, tell)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
    }
}

@Composable
private fun HomeScreen(s: TriggerSettings, vm: NudgeViewModel, accessibility: Boolean, openSettings: (String) -> Unit, tell: (String) -> Unit) {
    val service by vm.status.collectAsStateWithLifecycle()
    val check by vm.popupCheck.collectAsStateWithLifecycle()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("A LITTLE GESTURE. LESS REACHING.", style = MaterialTheme.typography.labelSmall)
            Text("Volume,\nwithin reach.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text("Swipe your edge. Your phone's own volume slider does the rest.", style = MaterialTheme.typography.bodyLarge)
            ToggleRow("Enable edge shortcut", if (s.enabled) "Your chosen gesture is enabled" else "Turn on after accessibility setup", s.enabled) {
                vm.update { current -> current.copy(enabled = it) }
            }
        }
    }
    Panel("Connection") {
        StatusLine("Accessibility permission", if (accessibility) "Enabled" else "Needs setup", accessibility)
        StatusLine("Accessibility service", when (service.connection(accessibility)) {
            ConnectionState.DISABLED -> "Disabled"
            ConnectionState.ENABLED_DISCONNECTED -> "Enabled but disconnected"
            ConnectionState.CONNECTED -> "Connected"
        }, service.connected)
        StatusLine("Edge touch area", when { service.triggerAttached -> "Active"; !s.enabled -> "Paused"; !service.connected -> "Waiting for service"; else -> "Hidden by current conditions" }, service.triggerAttached)
        if (!accessibility || !service.connected) {
            Text(if (!accessibility) "Open Accessibility → Downloaded apps → Nudge edge shortcut and enable it." else "Permission is enabled but the service is disconnected. In Accessibility, switch Nudge off and on again.")
            Button(onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text("Open accessibility") }
        }
    }
    Panel("Try it first") {
        Text("This only requests the native popup. It leaves the current volume unchanged.")
        Button(onClick = { tell(vm.testVolume()) }, modifier = Modifier.fillMaxWidth()) { Text("Test volume popup") }
        Text(check, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (check != "Not checked this session") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.confirmPopup(true) }) { Text("I saw it") }
                OutlinedButton(onClick = { vm.confirmPopup(false) }) { Text("Didn't appear") }
            }
        }
    }
    Notice("Invisible still means touchable. The configured edge area receives touches; other screen areas work normally. Use a narrow area to keep it comfortable.")
    Panel("Made for your phone") {
        Text("HyperOS 1 • Redmi Note 12", fontWeight = FontWeight.SemiBold)
        Text("Confirm compatibility on your installed build with the device checks. Native popup styling and timeout are controlled by HyperOS.")
        Text("Background autostart is recommended for Nudge on HyperOS. Enable it during setup; see Behavior & help for the steps. Whether your exact build requires it still needs testing.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CustomizeScreen(s: TriggerSettings, vm: NudgeViewModel) {
    val service by vm.status.collectAsStateWithLifecycle()
    var landscape by rememberSaveable { mutableStateOf(false) }
    val placement = s.placement(landscape)
    fun place(transform: (Placement) -> Placement) {
        val editingLandscape = landscape && s.landscapeOverrideEnabled
        vm.update { current ->
            if (editingLandscape) current.copy(landscape = transform(current.landscape)) else current.copy(portrait = transform(current.portrait))
        }
    }
    Text("Make room for your thumb.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Panel("Live preview") {
        Choices(listOf(false, true), landscape, { if (it) "Landscape" else "Portrait" }) { landscape = it }
        PhonePreview(s, landscape, vm::flush) { value -> place { it.copy(position = value) } }
        Text("Drag the shaded area to position it. A faint touch-area guide stays visible while Customize is open. Make handle visible shows or hides the handle in both previews immediately. The on-phone preview lets touches through and cannot activate volume while editing.", style = MaterialTheme.typography.bodySmall)
        if (!service.connected) Notice("Connect Nudge in Accessibility for an on-phone preview. You can still customize with the preview above.")
        else Text("The on-phone preview follows your phone's current orientation, even with the shortcut switched off. Rotate your phone to preview the other orientation.", style = MaterialTheme.typography.bodySmall)
        ToggleRow("Enable in landscape", "Allow the shortcut when your phone is sideways", s.landscapeEnabled) { vm.update { current -> current.copy(landscapeEnabled = it) } }
        ToggleRow("Override placement in landscape", "Off uses the default edge and center in both orientations", s.landscapeOverrideEnabled) { vm.update { current -> current.copy(landscapeOverrideEnabled = it) } }
        if (landscape && !s.landscapeOverrideEnabled) Text("Editing the shared default placement. Bottom stays at the bottom after rotation.", style = MaterialTheme.typography.bodySmall)
    }
    Panel("Position & touch area") {
        Choices(Edge.entries, placement.edge, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { edge -> place { it.copy(edge = edge) } }
        SettingSlider("Center along edge", placement.position, 0f..1f, "${(placement.position * 100).roundToInt()}%") { value -> place { it.copy(position = value) } }
        SettingSlider("Touch length", s.lengthDp, 48f..200f, "${s.lengthDp.roundToInt()} dp") { value -> vm.update { it.copy(lengthDp = value) } }
        SettingSlider("Touch depth", s.depthDp, 8f..32f, "${s.depthDp.roundToInt()} dp") { value -> vm.update { it.copy(depthDp = value) } }
        Text("Position is measured along the usable edge: top to bottom on sides, left to right on the bottom. Resizing keeps that center, clamping only at corners. Touch dimensions are shared across orientations.", style = MaterialTheme.typography.bodySmall)
    }
    Panel("Visible handle") {
        ToggleRow("Make handle visible", "Show the handle in the previews and during normal use", s.indicatorVisible) { value -> vm.update { it.copy(indicatorVisible = value) } }
        SettingSlider("Indicator length", s.indicatorLengthFraction, 0f..1f, "${(s.indicatorLengthFraction * 100).roundToInt()}% of touch length", enabled = s.indicatorVisible) { value -> vm.update { it.copy(indicatorLengthFraction = value) } }
        SettingSlider("Indicator thickness", s.indicatorThicknessFraction, 0f..1f, "${(s.indicatorThicknessFraction * 100).roundToInt()}% of touch depth", enabled = s.indicatorVisible) { value -> vm.update { it.copy(indicatorThicknessFraction = value) } }
        SettingSlider("Opacity", s.indicatorOpacity, 0f..1f, "${(s.indicatorOpacity * 100).roundToInt()}%", enabled = s.indicatorVisible) { value -> vm.update { it.copy(indicatorOpacity = value) } }
        SettingSlider("Corner radius", s.cornerRadiusDp, 0f..16f, "${s.cornerRadiusDp.roundToInt()} dp", enabled = s.indicatorVisible) { value -> vm.update { it.copy(cornerRadiusDp = value) } }
        ColorPicker(s.indicatorColor, enabled = s.indicatorVisible) { value -> vm.update { it.copy(indicatorColor = value) } }
        Text(if (s.indicatorVisible) "Length and thickness scale with the touch area. Corner radius fits inside the indicator when drawn; its saved value is kept when you resize." else "Turn on Make handle visible to edit its appearance. Your handle settings are kept while it is off.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BehaviorScreen(s: TriggerSettings, vm: NudgeViewModel, openSettings: (String) -> Unit, tell: (String) -> Unit) {
    val context = LocalContext.current
    var exclusions by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Text("Your gesture. Your rhythm.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Panel("Activation") {
        Choices(Activation.entries, s.activation, { when (it) { Activation.SWIPE -> "Swipe"; Activation.TAP -> "Tap"; Activation.LONG_PRESS -> "Hold" } }) { value -> vm.update { it.copy(activation = value) } }
        Text(when (s.activation) {
            Activation.SWIPE -> "Swipe inward from the edge and release. Movement mostly along the edge cancels the gesture."
            Activation.TAP -> "Tap and release within 250 ms."
            Activation.LONG_PRESS -> "Hold still for at least half a second, then release."
        }, style = MaterialTheme.typography.bodySmall)
        if (s.activation == Activation.SWIPE) {
            SettingSlider("Swipe distance", s.swipeDistanceDp, 12f..80f, "${s.swipeDistanceDp.roundToInt()} dp") { value -> vm.update { it.copy(swipeDistanceDp = value) } }
            SettingSlider("Swipe time limit", s.swipeTimeoutMs.toFloat(), 200f..1200f, "${s.swipeTimeoutMs} ms") { value -> vm.update { it.copy(swipeTimeoutMs = value.toLong()) } }
        }
        ToggleRow("Haptic feedback", "A small confirmation when you activate the shortcut", s.haptics) { value -> vm.update { it.copy(haptics = value) } }
    }
    Panel("Visibility") {
        ToggleRow("Available on lock screen", "Works on the awake lock screen after the first unlock following reboot. App exclusions are ignored while locked.", s.lockScreenEnabled) { value -> vm.update { it.copy(lockScreenEnabled = value) } }
        Text("Always hidden while the screen is off or on the always-on display. This option does not wake or unlock your phone or change indicator visibility.", style = MaterialTheme.typography.bodySmall)
        Text("Landscape is ${if (s.landscapeEnabled) "enabled" else "disabled"}. Change its placement in Customize.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { vm.loadApps(); exclusions = true }) { Text("Excluded apps · ${s.excludedPackages.size}") }
        Text("Window-change observation is off until exclusions are needed. After enabling exclusions, switch apps once to initialize visibility. Nudge does not read screen content.", style = MaterialTheme.typography.bodySmall)
    }
    Panel("HyperOS setup & recovery") {
        Text("Built to stay quiet: no polling, wake locks, periodic jobs, foreground service or ongoing animations. The trigger draws only when its appearance changes. Battery usage still needs measurement on your phone.", style = MaterialTheme.typography.bodySmall)
        HelpStep("1", "Accessibility", "Settings → Additional settings → Accessibility → Downloaded apps → Nudge edge shortcut. Some builds put Accessibility directly in Settings.")
        OutlinedButton(onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text("Accessibility settings") }
        HelpStep("2", "If accessibility is restricted", "Open Nudge's App info → ⋮ → Allow restricted settings, then return to Accessibility. This option depends on your Android build and how the APK was installed.")
        OutlinedButton(onClick = { openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) }) { Text("Nudge app info") }
        HelpStep("3", "Background autostart (recommended)", "Enable Nudge in Settings → Apps → Permissions → Background autostart. Some builds use Settings → Apps → Background autostart, call it Autostart, or list it in Security → Permissions. This can be needed for reliable background operation on HyperOS; your exact build still needs testing. Set it manually: Nudge does not verify this setting.")
        HelpStep("4", "If HyperOS still stops Nudge", "Try App info → Battery saver → No restrictions. For background app locking, try Security → Settings → Boost speed → Lock apps → Nudge, or lock Nudge in Recents where available. Leave battery settings unchanged if the shortcut stays connected, and check battery usage after any change. Menus vary by build.")
        HelpStep("5", "If it stops or says malfunctioning", "Reopen Nudge and check Connection on Home. Enabled but disconnected means the permission remains enabled but Android lost the service connection. Copy diagnostics below before switching Nudge off and on in Accessibility. Recent process-exit records can help distinguish a crash from system cleanup. Force-stop or revoked permissions require recovery by you.")
        Text("Extra overlay permission, root and ADB are not required by this implementation. Menu labels can vary by region. A missing native popup is a failed compatibility test, not a successful activation.", style = MaterialTheme.typography.bodySmall)
    }
    Panel("Device & updates") {
        Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid ${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT}\nNudge ${BuildConfig.VERSION_NAME}")
        OutlinedButton(onClick = {
            vm.inspectDiagnostics { details ->
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Nudge compatibility", details))
                tell("Device, service and recent exit details copied. Add your HyperOS version and test results.")
            }
        }) { Text("Copy compatibility details") }
        Text("Install newer Nudge release APKs over this installation to keep settings. Debug and release are separate apps. All release updates must use the original signing key.", style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = { confirmReset = true }) { Text("Reset customization") }
    if (exclusions) ExclusionsDialog(s, vm) { exclusions = false }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, title = { Text("Reset customization?") },
        text = { Text("Restore gesture, placement, appearance and exclusions to defaults. Your enable switch stays as it is.") },
        confirmButton = { TextButton(onClick = { vm.reset(); confirmReset = false }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ExclusionsDialog(s: TriggerSettings, vm: NudgeViewModel, dismiss: () -> Unit) {
    val apps by vm.launchableApps.collectAsStateWithLifecycle()
    val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Hide in these apps") }, text = {
        Column {
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search apps") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (!loaded) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                items(apps.filter { it.label.contains(search, true) || it.packageName.contains(search, true) }, key = { it.packageName }) { app ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Checkbox) {
                        vm.update { current -> current.copy(excludedPackages = if (app.packageName in current.excludedPackages) current.excludedPackages - app.packageName else current.excludedPackages + app.packageName) }
                    }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = app.packageName in s.excludedPackages, onCheckedChange = null)
                        Column { Text(app.label); Text(app.packageName, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                val unavailable = s.excludedPackages - apps.map { it.packageName }.toSet()
                if (loaded) items(unavailable.toList(), key = { it }) { pkg ->
                    TextButton(onClick = { vm.update { current -> current.copy(excludedPackages = current.excludedPackages - pkg) } }) { Text("Remove saved exclusion: $pkg") }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Done") } })
}

@Composable
private fun ColorPicker(color: Long, enabled: Boolean = true, change: (Long) -> Unit) {
    var custom by remember(color) { mutableStateOf("%06X".format(color and 0xFFFFFF)) }
    val labelColor = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Text("Indicator color", style = MaterialTheme.typography.labelLarge, color = labelColor)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0xFFB8D982L, 0xFF6BB7E8L, 0xFFE3A0CFL, 0xFFF1BA70L, 0xFFFFFFFFL, 0xFF242A20L).forEach { value ->
            FilledIconButton(onClick = { change(value) }, enabled = enabled, modifier = Modifier.semantics { contentDescription = "Indicator color #%06X".format(value and 0xFFFFFF) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(value))) {
                if ((color and 0xFFFFFF) == (value and 0xFFFFFF)) Text("✓", color = if (!enabled) labelColor else if (value == 0xFF242A20L) Color.White else Color.Black)
            }
        }
    }
    OutlinedTextField(value = custom, onValueChange = { value ->
        custom = value.removePrefix("#").take(6)
        if (custom.matches(Regex("[0-9a-fA-F]{6}"))) change(0xFF000000 or custom.toLong(16))
    }, enabled = enabled, label = { Text("Hex color") }, prefix = { Text("#") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = enabled && !custom.matches(Regex("[0-9a-fA-F]{6}")))
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, detail: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { change(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = change)
    }
}

@Composable
private fun SettingSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, enabled: Boolean = true, change: (Float) -> Unit) {
    val finish = LocalFinishAdjustment.current
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (enabled) LocalContentColor.current else disabledColor)
            Text(display, style = MaterialTheme.typography.labelMedium, color = if (enabled) MaterialTheme.colorScheme.primary else disabledColor)
        }
        Slider(value = value.coerceIn(range), onValueChange = change, onValueChangeFinished = finish, valueRange = range, enabled = enabled)
    }
}

@Composable
private fun <T> Choices(values: List<T>, selected: T, label: (T) -> String, change: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value -> FilterChip(selected = value == selected, onClick = { change(value) }, label = { Text(label(value)) }) }
    }
}

@Composable
private fun Notice(text: String, error: Boolean = false) {
    Surface(shape = RoundedCornerShape(18.dp), color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatusLine(title: String, state: String, good: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(7.dp).background(if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(state, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HelpStep(number: String, title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$number  $title", fontWeight = FontWeight.Medium)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
