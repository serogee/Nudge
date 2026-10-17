package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable fun PanelEditor(config: AppConfiguration, h: HandleConfig, index: Int, vm: NudgeViewModel) {
    val context = LocalContext.current
    var landscape by rememberSaveable(h.id, index) { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var appFor by remember { mutableStateOf<String?>(null) }
    var iconFor by remember { mutableStateOf<String?>(null) }
    var packPicker by remember { mutableStateOf(false) }
    val packs by vm.iconPacks.collectAsStateWithLifecycle()
    val service by vm.status.collectAsStateWithLifecycle()
    LaunchedEffect(h.id, index) { vm.loadPacks() }
    val p = h.panels[index]; val l = p.layout(landscape); val screen = rememberPreviewSpace(landscape)
    val handle = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, h.trigger, landscape)
    val g = PanelGeometry.calculate(screen.usable, screen.density, l, p.buttons.size, handle, h.trigger.placement(landscape).edge)
    fun layout(change: (PanelLayout) -> PanelLayout) = vm.editLayout(h.id,index,landscape,change)
    Section(t(R.string.panel_name,index+1)) {
        Text(context.handleLabel(config,h), style = MaterialTheme.typography.bodySmall)
        OrientationChoices(landscape) { landscape = it }
        PanelPreview(p,l,g,screen,handle,h.trigger,vm::flush) { x,y -> layout { it.copy(x=x,y=y) } }
        Help(R.string.panel_preview_help); Help(R.string.panel_drag_help)
        Help(if (service.connected) R.string.preview_orientation else R.string.preview_unavailable)
        Button(onClick = { vm.testPanel(h.id,index) }, enabled = service.connected) { Text(t(R.string.test_panel)) }
        Toggle(R.string.panel_landscape_override,R.string.override_help,p.landscapeOverride) { value -> vm.editPanel(h.id,index) { it.copy(landscapeOverride=value) } }
        if (landscape && !p.landscapeOverride) Help(R.string.inherited_help)
    }
    Section(R.string.layout) {
        Choices(PanelMode.entries,l.mode) { value -> layout { it.copy(mode=value) } }
        Choices(PanelDirection.entries,l.direction) { value -> layout { it.copy(direction=value) } }
        CountStepper(if(l.direction==PanelDirection.VERTICAL) R.string.columns else R.string.rows,l.crossCount,1,12) { value -> layout { it.copy(crossCount=value) } }
        if(l.mode==PanelMode.FIXED) {
            CountStepper(if(l.direction==PanelDirection.VERTICAL) R.string.maximum_rows else R.string.maximum_columns,l.maximumLines,1,20) { value -> layout { it.copy(maximumLines=value) } }
            Help(R.string.fixed_help)
        } else {
            SettingSlider(if(l.direction==PanelDirection.VERTICAL) R.string.panel_height else R.string.panel_width,l.extentFraction,0.15f..1f,t(R.string.percent,(l.extentFraction*100).roundToInt())) { value -> layout { it.copy(extentFraction=value) } }
            Help(R.string.scrollable_help)
        }
        SettingSlider(R.string.button_size,l.buttonSizeDp,48f..96f,t(R.string.dp_value,l.buttonSizeDp.roundToInt())) { value -> layout { it.copy(buttonSizeDp=value) } }
        if (g.crossCount < l.crossCount) Notice(t(R.string.panel_screen_limit,g.crossCount))
        if (p.buttons.size>g.visibleCount) Notice(t(R.string.panel_overflow,p.buttons.size-g.visibleCount))
    }
    Section(R.string.position) {
        Toggle(R.string.follow_handle,R.string.follow_handle_help,l.followHandle) { value -> layout { it.copy(followHandle=value) } }
        PercentSlider(R.string.center_x,l.x,!l.followHandle) { value -> layout { it.copy(x=value) } }
        PercentSlider(R.string.center_y,l.y,!l.followHandle) { value -> layout { it.copy(y=value) } }
    }
    Section(R.string.outside_touch) { Choices(OutsideTouch.entries,p.outsideTouch) { value -> vm.editPanel(h.id,index) { it.copy(outsideTouch=value) } }; Help(R.string.outside_help) }
    Section(R.string.appearance) {
        ColorPicker(p.background) { value -> vm.editPanel(h.id,index) { it.copy(background=value) } }
        SettingSlider(R.string.opacity,p.opacity,0.1f..1f,t(R.string.percent,(p.opacity*100).roundToInt())) { value -> vm.editPanel(h.id,index) { it.copy(opacity=value) } }
        SettingSlider(R.string.corner_radius,p.cornerDp,0f..48f,t(R.string.dp_value,p.cornerDp.roundToInt())) { value -> vm.editPanel(h.id,index) { it.copy(cornerDp=value) } }
        Text(t(R.string.icon_shape)); Choices(IconShape.entries,p.iconShape) { value -> vm.editPanel(h.id,index) { it.copy(iconShape=value) } }
        SettingSlider(R.string.icon_corners,p.iconCornerFraction,0f..0.5f,t(R.string.percent,(p.iconCornerFraction*100).roundToInt()),p.iconShape!=IconShape.CIRCLE) { value -> vm.editPanel(h.id,index) { it.copy(iconCornerFraction=value) } }
        OutlinedButton(onClick = { vm.loadPacks(); packPicker=true }) { Text(packs.firstOrNull { it.packageName==p.iconPack }?.label ?: if(p.iconPack.isBlank()) t(R.string.original_icons) else p.iconPack) }
        if(p.iconPack.isNotBlank() && packs.none { it.packageName==p.iconPack }) Help(R.string.pack_unavailable)
    }
    Section(R.string.buttons) {
        Button(onClick = { adding=true }) { Text(t(R.string.add_button)) }
        if(p.buttons.isEmpty()) Help(R.string.no_buttons)
        else LazyColumn(Modifier.fillMaxWidth().heightIn(max=440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(p.buttons,key={ _,b -> b.id }) { position,b ->
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.alpha(if(position>=g.visibleCount) 0.38f else 1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        ButtonGraphic(b,p.iconPack,Modifier.size(32.dp)); Text(context.buttonLabel(b),Modifier.weight(1f))
                        if(position>=g.visibleCount) Text(t(R.string.not_shown),style=MaterialTheme.typography.labelSmall)
                    }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick={ editing=b.id }) { Text(t(R.string.edit)) }
                        TextButton(onClick={ vm.moveButton(h.id,index,b.id,-1) },enabled=position>0) { Text(t(R.string.move_up)) }
                        TextButton(onClick={ vm.moveButton(h.id,index,b.id,1) },enabled=position<p.buttons.lastIndex) { Text(t(R.string.move_down)) }
                        TextButton(onClick={ vm.editPanel(h.id,index) { it.copy(buttons=it.buttons.filterNot { item -> item.id==b.id }) } }) { Text(t(R.string.remove)) }
                    }
                }
            }
        }
    }
    if(adding) ActionPicker({ adding=false }) { action ->
        adding=false
        if(action==ButtonAction.OPEN_APP) { vm.loadApps(); appFor="" }
        else vm.editPanel(h.id,index) { it.copy(buttons=it.buttons+PanelButton(action=action)) }
    }
    appFor?.let { target -> AppPicker(vm,{ appFor=null }) { chosen ->
        if(target.isEmpty()) vm.editPanel(h.id,index) { it.copy(buttons=it.buttons+PanelButton(action=ButtonAction.OPEN_APP,appPackage=chosen.packageName,appActivity=chosen.activityName,appLabel=chosen.label)) }
        else vm.editButton(h.id,index,target) { it.copy(appPackage=chosen.packageName,appActivity=chosen.activityName,appLabel=chosen.label) }
        appFor=null
    } }
    p.buttons.firstOrNull { it.id==editing }?.let { b ->
        AlertDialog(onDismissRequest={ editing=null },title={ Text(context.buttonLabel(b)) },text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OptionRow(t(R.string.buttons),ButtonAction.entries,b.action) { action -> vm.editButton(h.id,index,b.id) { it.copy(action=action) } }
            if(b.action==ButtonAction.OPEN_APP) OutlinedButton(onClick={ vm.loadApps(); appFor=b.id }) { Text(t(R.string.choose_app)) }
            OutlinedButton(onClick={ vm.loadPacks(); iconFor=b.id }) { Text(t(R.string.choose_icon)) }
            TextButton(onClick={ vm.editButton(h.id,index,b.id) { it.copy(icon=IconReference()) } }) { Text(t(R.string.reset_icon)) }
        } },confirmButton={ TextButton(onClick={ editing=null }) { Text(t(R.string.done)) } })
    }
    if(packPicker) PackPicker(packs,{ packPicker=false }) { pack -> vm.editPanel(h.id,index) { it.copy(iconPack=pack) }; packPicker=false }
    iconFor?.let { id -> IconPicker(packs,{ iconFor=null }) { icon -> vm.editButton(h.id,index,id) { it.copy(icon=icon) }; iconFor=null } }
}

@Composable private fun CountStepper(title: Int,value: Int,min: Int,max: Int,change:(Int)->Unit) {
    val context=LocalContext.current; val text=t(title)
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(text,Modifier.weight(1f))
        TextButton(onClick={ change(value-1) },enabled=value>min,modifier=Modifier.semantics { contentDescription=context.getString(R.string.decrease,text) }) { Text("−") }
        Text(t(R.string.count_value,value))
        TextButton(onClick={ change(value+1) },enabled=value<max,modifier=Modifier.semantics { contentDescription=context.getString(R.string.increase,text) }) { Text("+") }
    }
}
