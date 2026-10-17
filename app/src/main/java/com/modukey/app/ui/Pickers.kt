package com.modukey.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.IconPackInfo

@Composable fun ActionPicker(dismiss:()->Unit, choose:(ButtonAction)->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={ Text(t(R.string.add_button)) },text={ LazyColumn(Modifier.heightIn(max=450.dp)) {
        items(ButtonAction.entries) { action -> TextButton(onClick={ choose(action) },modifier=Modifier.fillMaxWidth()) { Text(t(label(action))) } }
    } },confirmButton={ TextButton(onClick=dismiss) { Text(t(R.string.cancel)) } })
}
@Composable fun AppPicker(vm:NudgeViewModel,dismiss:()->Unit,choose:(LaunchableApp)->Unit) {
    val apps by vm.launchableApps.collectAsStateWithLifecycle(); val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=dismiss,title={ Text(t(R.string.choose_app)) },text={ Column {
        OutlinedTextField(query,{ query=it },label={ Text(t(R.string.search_apps)) },singleLine=true)
        LazyColumn(Modifier.heightIn(max=400.dp)) {
            if(!loaded) item { CircularProgressIndicator() }
            val found=apps.filter { it.label.contains(query,true)||it.packageName.contains(query,true) }
            if(loaded && found.isEmpty()) item { Help(R.string.no_apps) }
            items(found,key={ it.packageName+"/"+it.activityName }) { app ->
                Column(Modifier.fillMaxWidth().clickable(role=Role.Button) { choose(app) }.padding(vertical=12.dp)) { Text(app.label); Text(app.packageName,style=MaterialTheme.typography.labelSmall) }
            }
        }
    } },confirmButton={ TextButton(onClick=dismiss) { Text(t(R.string.cancel)) } })
}
@Composable fun ExclusionsDialog(handle:HandleConfig,vm:NudgeViewModel,dismiss:()->Unit) {
    val apps by vm.launchableApps.collectAsStateWithLifecycle(); val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=dismiss,title={ Text(t(R.string.excluded_apps)) },text={ Column {
        OutlinedTextField(query,{ query=it },label={ Text(t(R.string.search_apps)) },singleLine=true)
        LazyColumn(Modifier.heightIn(max=400.dp)) {
            if(!loaded) item { CircularProgressIndicator() }
            items(apps.distinctBy { it.packageName }.filter { it.label.contains(query,true)||it.packageName.contains(query,true) },key={ it.packageName }) { app ->
                Row(Modifier.fillMaxWidth().clickable(role=Role.Checkbox) { vm.editTrigger(handle.id) { current -> current.copy(excludedPackages=if(app.packageName in current.excludedPackages) current.excludedPackages-app.packageName else current.excludedPackages+app.packageName) } }.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(app.packageName in handle.trigger.excludedPackages,null)
                    Column { Text(app.label); Text(app.packageName,style=MaterialTheme.typography.labelSmall) }
                }
            }
            val missing=handle.trigger.excludedPackages-apps.map { it.packageName }.toSet()
            if(loaded) items(missing.toList(),key={ it }) { pkg -> TextButton(onClick={ vm.editTrigger(handle.id) { it.copy(excludedPackages=it.excludedPackages-pkg) } }) { Text(t(R.string.remove)+": "+pkg) } }
        }
    } },confirmButton={ TextButton(onClick=dismiss) { Text(t(R.string.done)) } })
}
@Composable fun PackPicker(packs:List<IconPackInfo>,dismiss:()->Unit,choose:(String)->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={ Text(t(R.string.icon_pack)) },text={ LazyColumn(Modifier.heightIn(max=400.dp)) {
        item { TextButton(onClick={ choose("") }) { Text(t(R.string.original_icons)) } }
        if(packs.isEmpty()) item { Help(R.string.no_icon_packs) }
        items(packs,key={ it.packageName }) { p -> TextButton(onClick={ choose(p.packageName) }) { Text(p.label) } }
    } },confirmButton={ TextButton(onClick=dismiss) { Text(t(R.string.cancel)) } })
}
@Composable fun IconPicker(packs:List<IconPackInfo>,dismiss:()->Unit,choose:(IconReference)->Unit) {
    val app=LocalContext.current.applicationContext as NudgeApplication
    var pack by remember { mutableStateOf(packs.firstOrNull()?.packageName.orEmpty()) }
    var picking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(pack) { loading=true; names=if(pack.isBlank()) emptyList() else app.icons.names(pack); loading=false }
    AlertDialog(onDismissRequest=dismiss,title={ Text(t(R.string.choose_icon)) },text={ Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={ picking=true }) { Text(packs.firstOrNull { it.packageName==pack }?.label ?: t(R.string.icon_pack)) }
        OutlinedTextField(query,{ query=it },label={ Text(t(R.string.search_icons)) },singleLine=true)
        if(loading) CircularProgressIndicator()
        else if(names.isEmpty()) Help(if(packs.isEmpty()) R.string.no_icon_packs else R.string.no_pack_icons)
        LazyVerticalGrid(columns=GridCells.Fixed(3),modifier=Modifier.height(320.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(names.filter { it.contains(query,true) },key={ it }) { name ->
                Column(Modifier.clickable(role=Role.Button) { choose(IconReference(pack,name)) }.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    ButtonGraphic(PanelButton(icon=IconReference(pack,name)),"",Modifier.size(48.dp))
                    Text(name,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    } },confirmButton={ TextButton(onClick=dismiss) { Text(t(R.string.cancel)) } })
    if(picking) PackPicker(packs,{ picking=false }) { pack=it; picking=false }
}
