package com.modukey.app.core

fun GestureTarget.dragsWheel(config: AppConfiguration, landscape: Boolean): Boolean = wheelPick.enabled &&
    config.panel(panelId)?.settings?.layout(landscape)?.mode == PanelMode.WHEEL

fun HandleConfig.terminalWheelStage(config: AppConfiguration, landscape: Boolean): Int? = (0..2).firstOrNull {
    val target = target(GestureSlot.entries[it])
    target.dragsWheel(config, landscape) && !target.wheelPick.allowLaterSwipes
}

/** Retain stored assignments. Only the effective routing table changes with the active orientation. */
fun HandleConfig.effectiveGestures(config: AppConfiguration, landscape: Boolean): HandleConfig {
    val terminal = terminalWheelStage(config, landscape)
    val continuous = (0..(terminal ?: 2)).any {
        val target = target(GestureSlot.entries[it])
        target.slideToAdjust || target.dragsWheel(config, landscape)
    }
    return copy(bindings = GestureSlot.entries.associateWith { slot ->
        if ((slot.ordinal < 3 && terminal != null && slot.ordinal > terminal) || (slot == GestureSlot.SWIPE_HOLD && continuous)) GestureTarget.NONE
        else target(slot)
    })
}
