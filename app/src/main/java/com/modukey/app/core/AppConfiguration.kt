package com.modukey.app.core

import java.util.UUID

enum class GestureKind { SWIPE, SWIPE_HOLD, TAP, HOLD }
enum class GestureSlot { SWIPE_1, SWIPE_2, SWIPE_3, SWIPE_HOLD, TAP, HOLD }
enum class GestureTarget { NONE, VOLUME, PANEL_1, PANEL_2, PANEL_3;
    val panelIndex: Int? get() = when (this) { PANEL_1 -> 0; PANEL_2 -> 1; PANEL_3 -> 2; else -> null }
}
enum class PanelMode { FIXED, SCROLLABLE }
enum class PanelDirection { VERTICAL, HORIZONTAL }
enum class OutsideTouch { BLOCK, PASS_THROUGH }
enum class ButtonAction { PHONE_SETTINGS, NUDGE_SETTINGS, VOLUME_UP, VOLUME_DOWN, MEDIA_MUTE, SILENT, VIBRATE, DND, LOCK, OPEN_APP }
enum class IconShape { CIRCLE, ROUNDED_SQUARE, SQUIRCLE }

data class IconReference(val packageName: String = "", val drawable: String = "")
data class PanelButton(
    val id: String = newId(), val action: ButtonAction = ButtonAction.VOLUME_UP,
    val appPackage: String = "", val appActivity: String = "", val appLabel: String = "",
    val icon: IconReference = IconReference(),
)
data class PanelLayout(
    val mode: PanelMode = PanelMode.FIXED,
    val direction: PanelDirection = PanelDirection.VERTICAL,
    val crossCount: Int = 1,
    val maximumLines: Int = 5,
    val extentFraction: Float = 0.6f,
    val buttonSizeDp: Float = 56f,
    val followHandle: Boolean = true,
    val x: Float = 0.5f, val y: Float = 0.5f,
) {
    fun normalized() = copy(crossCount = crossCount.coerceIn(1, 12), maximumLines = maximumLines.coerceIn(1, 20),
        extentFraction = extentFraction.valid(0.6f).coerceIn(0.15f, 1f), buttonSizeDp = buttonSizeDp.valid(56f).coerceIn(48f, 96f),
        x = x.valid(0.5f).coerceIn(0f, 1f), y = y.valid(0.5f).coerceIn(0f, 1f))
}
data class PanelConfig(
    val buttons: List<PanelButton> = emptyList(),
    val portrait: PanelLayout = PanelLayout(),
    val landscape: PanelLayout = PanelLayout(),
    val landscapeOverride: Boolean = false,
    val outsideTouch: OutsideTouch = OutsideTouch.BLOCK,
    val background: Long = 0xFF252A21,
    val opacity: Float = 0.98f,
    val cornerDp: Float = 24f,
    val iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    val iconCornerFraction: Float = 0.25f,
    val iconPack: String = "",
) {
    fun layout(landscape: Boolean) = if (landscape && landscapeOverride) this.landscape else portrait
    fun normalized() = copy(portrait = portrait.normalized(), landscape = landscape.normalized(),
        opacity = opacity.valid(0.98f).coerceIn(0.1f, 1f), cornerDp = cornerDp.valid(24f).coerceIn(0f, 48f),
        iconCornerFraction = iconCornerFraction.valid(0.25f).coerceIn(0f, 0.5f), background = background or 0xFF000000)
}
data class HandleConfig(
    val id: String = newId(), val name: String = "", val enabled: Boolean = false,
    val trigger: TriggerSettings = TriggerSettings(),
    val bindings: Map<GestureSlot, GestureTarget> = mapOf(GestureSlot.SWIPE_1 to GestureTarget.VOLUME),
    val volumeSequenceMs: Long = 3_000,
    val panels: List<PanelConfig> = List(3) { PanelConfig() },
) {
    fun target(slot: GestureSlot) = bindings[slot] ?: GestureTarget.NONE
    fun normalized() = copy(name = name.take(80), trigger = trigger.normalized().copy(enabled = false, includePrereleases = false, activation = Activation.SWIPE),
        bindings = GestureSlot.entries.associateWith(::target),
        volumeSequenceMs = volumeSequenceMs.coerceIn(1_000, 10_000),
        panels = List(3) { panels.getOrNull(it)?.normalized() ?: PanelConfig() })
    fun duplicate() = copy(id = newId(), enabled = false,
        panels = panels.map { p -> p.copy(buttons = p.buttons.map { it.copy(id = newId()) }) })
}
data class AppConfiguration(
    val enabled: Boolean = false, val includePrereleases: Boolean = false,
    val handles: List<HandleConfig> = listOf(HandleConfig(id = "first-handle", enabled = true)),
) {
    fun normalized() = copy(handles = handles.map(HandleConfig::normalized))
    fun handle(id: String?) = handles.firstOrNull { it.id == id }
    fun editHandle(id: String, edit: (HandleConfig) -> HandleConfig) = copy(handles = handles.map { if (it.id == id) edit(it) else it })
    companion object {
        fun fromLegacy(old: TriggerSettings) = AppConfiguration(old.enabled, old.includePrereleases,
            listOf(HandleConfig(id = "first-handle", enabled = true, trigger = old,
                bindings = mapOf(when (old.activation) {
                    Activation.SWIPE -> GestureSlot.SWIPE_1; Activation.TAP -> GestureSlot.TAP; Activation.LONG_PRESS -> GestureSlot.HOLD
                } to GestureTarget.VOLUME)))).normalized()
    }
}
fun newId(): String = UUID.randomUUID().toString()
private fun Float.valid(fallback: Float) = if (isFinite()) this else fallback
