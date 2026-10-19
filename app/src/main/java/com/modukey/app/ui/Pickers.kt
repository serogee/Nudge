package com.modukey.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.IconPackInfo

@Composable fun ActionPicker(dismiss: () -> Unit, includeEmpty: Boolean = true, choose: (ButtonAction) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.button_action)) }, text = {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightIn(max = 460.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ButtonAction.entries.filter { includeEmpty || it != ButtonAction.EMPTY }) { action ->
                Surface(onClick = { choose(action) }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(12.dp).heightIn(min = 72.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                        ButtonGraphic(PanelButton(action = action), "", Modifier.size(28.dp))
                        Text(t(label(action)), Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}
@Composable fun AppPicker(vm: NudgeViewModel, dismiss: () -> Unit, choose: (LaunchableApp) -> Unit) {
    val apps by vm.launchableApps.collectAsStateWithLifecycle()
    val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(vm) { vm.loadApps() }
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.choose_app)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(t(R.string.search_apps)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                if (!loaded) item { CircularProgressIndicator() }
                val found = apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
                if (loaded && found.isEmpty()) item { Help(R.string.no_apps) }
                items(found, key = { it.packageName + "/" + it.activityName }) { app ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { choose(app) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ButtonGraphic(PanelButton(action = ButtonAction.OPEN_APP, appPackage = app.packageName, appActivity = app.activityName), "", Modifier.size(40.dp))
                        Column(Modifier.weight(1f)) { Text(app.label); Text(app.packageName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}
@Composable fun ExclusionsDialog(handle:HandleConfig,vm:NudgeViewModel,dismiss:()->Unit) {
    val apps by vm.launchableApps.collectAsStateWithLifecycle(); val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val excluded = handle.trigger.excludedPackages
    val installed = remember(apps) { apps.distinctBy { it.packageName } }
    val ordered = remember(installed, query, excluded) {
        installed.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
            .sortedByDescending { it.packageName in excluded }
    }
    val missing = remember(installed, excluded, query) {
        (excluded - installed.map { it.packageName }.toSet()).filter { it.contains(query, true) }.sorted()
    }
    AlertDialog(onDismissRequest=dismiss,title={ Text(t(R.string.excluded_apps)) },text={ Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(query,{ query=it },label={ Text(t(R.string.search_apps)) },singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=400.dp)) {
            if(!loaded) item { CircularProgressIndicator() }
            if(loaded) items(missing,key={ it }) { pkg ->
                TextButton(onClick={ vm.editTrigger(handle.id) { it.copy(excludedPackages=it.excludedPackages-pkg) } }) {
                    Icon(AppIcons.Apps, null, Modifier.size(40.dp)); Spacer(Modifier.width(12.dp))
                    Text(t(R.string.remove)+": "+pkg)
                }
            }
            items(ordered,key={ it.packageName }) { app ->
                val selected = app.packageName in excluded
                Row(Modifier.fillMaxWidth().toggleable(value = selected, role = Role.Checkbox, onValueChange = { checked ->
                    vm.editTrigger(handle.id) { current -> current.copy(excludedPackages =
                        if (checked) current.excludedPackages + app.packageName else current.excludedPackages - app.packageName) }
                }).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(selected,null)
                    val icon = remember(app.packageName, app.activityName) { PanelButton(action = ButtonAction.OPEN_APP, appPackage = app.packageName, appActivity = app.activityName) }
                    ButtonGraphic(icon, "", Modifier.size(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label)
                        Text(app.packageName,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
    } },confirmButton={ TextButton(onClick=dismiss) { Text(t(R.string.done)) } })
}

@Composable fun PackPicker(packs: List<IconPackInfo>, dismiss: () -> Unit, choose: (String) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.icon_pack)) }, text = {
        LazyColumn(Modifier.heightIn(max = 400.dp)) {
            item { ListItem(headlineContent = { Text(t(R.string.original_icons)) }, leadingContent = { Icon(AppIcons.Apps, null) }, modifier = Modifier.clickable { choose("") }) }
            if (packs.isEmpty()) item { Help(R.string.no_icon_packs) }
            items(packs, key = { it.packageName }) { pack ->
                ListItem(headlineContent = { Text(pack.label) }, leadingContent = { Icon(AppIcons.Apps, null) }, modifier = Modifier.clickable { choose(pack.packageName) })
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}
@Composable fun IconPicker(packs: List<IconPackInfo>, dismiss: () -> Unit, current: IconReference = IconReference(), choose: (IconReference) -> Unit) {
    val app = LocalContext.current.applicationContext as NudgeApplication
    var source by remember { mutableIntStateOf(if (current.packageName.isNotBlank()) 1 else 0) }
    var pack by remember { mutableStateOf(current.packageName.ifBlank { packs.firstOrNull()?.packageName.orEmpty() }) }
    var picking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(pack, source) { loading = true; names = if (source == 1 && pack.isNotBlank()) app.icons.names(pack) else emptyList(); loading = false }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.choose_icon)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { choose(IconReference()) }) { Icon(AppIcons.Reset, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(t(R.string.original_icon)) }
            BrowserTabs(listOf(t(R.string.icon_library), t(R.string.icon_packs)), source) { source = it; query = "" }
            if (source == 1) {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(packs.firstOrNull { it.packageName == pack }?.label ?: t(R.string.icon_pack), Modifier.weight(1f)); Icon(AppIcons.Down, null)
                }
            }
            OutlinedTextField(query, { query = it }, label = { Text(t(R.string.search_icons)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (source == 1 && !loading && names.isEmpty()) Help(if (packs.isEmpty()) R.string.no_icon_packs else R.string.no_pack_icons)
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (source == 0) items(BuiltInIcon.entries.filter { context.getString(label(it)).contains(query, true) }, key = { it.name }) { item ->
                    IconChoice(context.getString(label(item)), { choose(IconReference(builtIn = item)) }) { Icon(AppIcons.library(item), null, Modifier.size(32.dp)) }
                } else items(names.filter { it.contains(query, true) }, key = { it }) { name ->
                    IconChoice(name, { choose(IconReference(pack, name)) }) { ButtonGraphic(PanelButton(icon = IconReference(pack, name)), "", Modifier.size(40.dp)) }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
    if (picking) PackPicker(packs, { picking = false }) { pack = it; picking = false }
}
@Composable private fun IconChoice(name: String, select: () -> Unit, content: @Composable () -> Unit) {
    Surface(onClick = select, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(8.dp).heightIn(min = 76.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            content(); Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
        }
    }
}
