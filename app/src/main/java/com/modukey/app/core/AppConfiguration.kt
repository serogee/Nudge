package com.modukey.app.core

import java.util.UUID

enum class GestureKind { SWIPE, SWIPE_HOLD, TAP, HOLD }
enum class GestureSlot { SWIPE_1, SWIPE_2, SWIPE_3, SWIPE_HOLD, TAP, HOLD }
enum class SliderActivation { AFTER_HOLD, IMMEDIATE }
data class GestureTarget(val panelId: String? = null, val volume: Boolean = false, val action: PanelButton? = null, val brightness: Boolean = false,
    val slideToAdjust: Boolean = false, val sliderActivation: SliderActivation = SliderActivation.AFTER_HOLD, val sliderHoldMs: Long = 200,
    val wheelPick: WheelPickSettings = WheelPickSettings()) {
    val isNone get() = panelId == null && !volume && action == null && !brightness
    fun normalized(): GestureTarget {
        val target = if (action != null) copy(panelId = null, volume = volume || (action.showVolumePopup && action.action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)), action = action.normalized().copy(showVolumePopup = false)) else this
        val pick = target.wheelPick.normalized().let { if (target.panelId == null) it.copy(enabled = false) else it }
        return target.copy(slideToAdjust = target.slideToAdjust && (target.volume xor target.brightness) && !pick.enabled,
            sliderHoldMs = target.sliderHoldMs.coerceIn(100, 1_000), wheelPick = pick)
    }
    fun duplicate() = copy(action = action?.copy(id = newId()))
    companion object {
        val NONE = GestureTarget()
        val VOLUME = GestureTarget(volume = true)
        fun panel(id: String, volume: Boolean = false) = GestureTarget(id, volume)
        fun direct(action: PanelButton) = GestureTarget(action = action).normalized()
    }
}
enum class PanelMode { FIXED, SCROLLABLE, WHEEL }
enum class PanelDirection { VERTICAL, HORIZONTAL }
enum class OutsideTouch { BLOCK, PASS_THROUGH, KEEP_OPEN }
enum class ButtonAction { PHONE_SETTINGS, NUDGE_SETTINGS, VOLUME_UP, VOLUME_DOWN, MEDIA_MUTE, SILENT, VIBRATE, DND, LOCK, OPEN_APP, INPUT_METHOD_PICKER, CLOSE_PANELS, TOGGLE_HANDLES, TOGGLE_HANDLE_VISIBILITY, TOGGLE_AND_HIDE_HANDLES, SCREENSHOT, BRIGHTNESS_MAXIMUM, BRIGHTNESS_MINIMUM, NOTIFICATIONS, CONTROL_PANEL, BRIGHTNESS_UP, BRIGHTNESS_DOWN, BRIGHTNESS_AUTOMATIC, EMPTY }
enum class PanelAnimation { NONE, FADE, SLIDE, SCALE }
enum class EmptyAppearance { NO_ICON, NO_BUTTON, SLASH }

enum class IconContainer { DEFAULT, CONTAINER, STANDALONE }
enum class BuiltInIcon { CAMERA, WALLET, PHONE, FLASHLIGHT, MUSIC, HEADPHONES, CALENDAR, CLOCK, MAP, HOME, FAVORITE, APPS, NUDGE }

data class IconReference(val packageName: String = "", val drawable: String = "", val builtIn: BuiltInIcon? = null)
data class PanelButton(
    val id: String = newId(), val action: ButtonAction = ButtonAction.VOLUME_UP,
    val appPackage: String = "", val appActivity: String = "", val appLabel: String = "",
    val icon: IconReference = IconReference(),
    val showVolumePopup: Boolean = false,
    val iconContainer: IconContainer = IconContainer.DEFAULT,
    val emptyAppearance: EmptyAppearance = EmptyAppearance.SLASH,
    val handleScope: HandleToggleScope = HandleToggleScope.ALL,
    val brightnessReturn: BrightnessReturnSettings = BrightnessReturnSettings(),
    val toggleScope: HandleGroupScope = HandleGroupScope.ALL,
    val visibilityScope: HandleGroupScope = HandleGroupScope.ALL,
) {
    fun normalized() = copy(brightnessReturn = brightnessReturn.normalized())
}
data class PanelLayout(
    val mode: PanelMode = PanelMode.FIXED,
    val direction: PanelDirection = PanelDirection.VERTICAL,
    val crossCount: Int = 1,
    val maximumLines: Int = 5,
    val extentFraction: Float = 0.6f,
    val buttonSizeDp: Float = 56f,
    val followHandle: Boolean = true,
    val x: Float = 0.5f, val y: Float = 0.5f,
    val offsetXDp: Float = 0f, val offsetYDp: Float = 0f,
    val wheel: WheelLayoutSettings = WheelLayoutSettings(),
) {
    fun normalized() = copy(crossCount = crossCount.coerceIn(1, 12), maximumLines = maximumLines.coerceIn(1, 20),
        extentFraction = extentFraction.valid(0.6f).coerceIn(0.15f, 1f), buttonSizeDp = buttonSizeDp.valid(56f).coerceIn(48f, 96f),
        x = x.valid(0.5f).coerceIn(0f, 1f), y = y.valid(0.5f).coerceIn(0f, 1f),
        offsetXDp = offsetXDp.valid(0f).coerceIn(-240f, 240f), offsetYDp = offsetYDp.valid(0f).coerceIn(-240f, 240f), wheel = wheel.normalized())
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
    val iconCornerFraction: Float = 0.25f,
    val iconPack: String = "",
    val animation: PanelAnimation = PanelAnimation.NONE,
    val iconBackground: Boolean = true,
    val automaticButtonColor: Boolean = true,
    val buttonColor: Long = 0xFF3C3C3E,
    val automaticIconColor: Boolean = true,
    val iconColor: Long = 0xFFFFFFFF,
    val autoCloseEnabled: Boolean = false,
    val autoCloseMs: Long = 3_000,
    val closeAnimation: PanelAnimation = PanelAnimation.NONE,
    val buttonOpacity: Float = 1f,
    val iconOpacity: Float = 1f,
    val activeAppearance: ActiveButtonAppearance = ActiveButtonAppearance(),
    val wheelHover: WheelHoverAppearance = WheelHoverAppearance(),
) {
    val isWheel get() = portrait.mode == PanelMode.WHEEL || (landscapeOverride && landscape.mode == PanelMode.WHEEL)
    fun layout(landscape: Boolean): PanelLayout {
        val layout = if (landscape && landscapeOverride) this.landscape else portrait
        return if (isWheel) layout.copy(mode = PanelMode.WHEEL) else layout
    }
    /** Editing appearance or orientation cannot convert a wheel into a panel, or the reverse. */
    fun retainKind(original: PanelConfig): PanelConfig = if (original.isWheel) copy(
        portrait = portrait.copy(mode = PanelMode.WHEEL), landscape = landscape.copy(mode = PanelMode.WHEEL)) else copy(
        portrait = portrait.copy(mode = portrait.mode.takeUnless { it == PanelMode.WHEEL } ?: original.portrait.mode),
        landscape = landscape.copy(mode = landscape.mode.takeUnless { it == PanelMode.WHEEL } ?: original.landscape.mode.takeUnless { it == PanelMode.WHEEL } ?: PanelMode.FIXED))
    fun normalized() = copy(buttons = buttons.map(PanelButton::normalized), portrait = portrait.normalized().let { if (isWheel) it.copy(mode = PanelMode.WHEEL) else it },
        landscape = landscape.normalized().let { if (isWheel) it.copy(mode = PanelMode.WHEEL) else if (it.mode == PanelMode.WHEEL) it.copy(mode = portrait.mode) else it },
        opacity = opacity.valid(0.98f).coerceIn(0f, 1f), cornerDp = cornerDp.valid(24f).coerceIn(0f, 48f),
        iconCornerFraction = iconCornerFraction.valid(0.25f).coerceIn(0f, 0.5f), background = background or 0xFF000000,
        buttonColor = buttonColor or 0xFF000000, iconColor = iconColor or 0xFF000000,
        autoCloseMs = autoCloseMs.coerceIn(1_000, 60_000),
        buttonOpacity = buttonOpacity.valid(1f).coerceIn(0f, 1f), iconOpacity = iconOpacity.valid(1f).coerceIn(0f, 1f), activeAppearance = activeAppearance.normalized(), wheelHover = wheelHover.normalized())
}
data class HandleConfig(
    val id: String = newId(), val name: String = "", val enabled: Boolean = false,
    val trigger: TriggerSettings = TriggerSettings(),
    val bindings: Map<GestureSlot, GestureTarget> = mapOf(GestureSlot.SWIPE_1 to GestureTarget.VOLUME),
    val volumeSequenceMs: Long = 3_000,
    val indicatorHidden: Boolean = false,
    val volumeTravelDp: Float = 240f,
    val brightnessTravelDp: Float = 240f,
) {
    fun target(slot: GestureSlot) = bindings[slot] ?: GestureTarget.NONE
    val hasContinuousSwipe get() = GestureSlot.entries.take(3).any { target(it).normalized().slideToAdjust }
    fun runtimeTarget(slot: GestureSlot) = if (slot == GestureSlot.SWIPE_HOLD && hasContinuousSwipe) GestureTarget.NONE else target(slot)
    fun normalized() = copy(name = name.take(80), trigger = trigger.normalized().copy(enabled = false, includePrereleases = false, activation = Activation.SWIPE),
        bindings = GestureSlot.entries.associateWith { slot -> target(slot).normalized().let { if (slot == GestureSlot.TAP) it.copy(slideToAdjust = false, wheelPick = it.wheelPick.copy(enabled = false)) else it } },
        volumeSequenceMs = volumeSequenceMs.coerceIn(1_000, 10_000),
        volumeTravelDp = volumeTravelDp.valid(240f).coerceIn(48f, 600f), brightnessTravelDp = brightnessTravelDp.valid(240f).coerceIn(48f, 600f))
    fun duplicate() = copy(id = newId(), enabled = false, bindings = bindings.mapValues { it.value.duplicate() })
}

data class SavedPanel(val id: String = newId(), val name: String = "", val settings: PanelConfig = PanelConfig()) {
    val isWheel get() = settings.isWheel
    fun normalized() = copy(name = name.take(80), settings = settings.normalized())
    fun duplicate() = copy(id = newId(), settings = settings.copy(buttons = settings.buttons.map { it.copy(id = newId()) }))
}
data class AppConfiguration(
    val enabled: Boolean = false, val includePrereleases: Boolean = false,
    val handles: List<HandleConfig> = listOf(HandleConfig(id = "first-handle", enabled = true)),
    val panels: List<SavedPanel> = emptyList(),
    val brightnessPopup: BrightnessPopupSettings = BrightnessPopupSettings(),
    val shortcuts: List<HomeShortcut> = emptyList(),
    val handleAnimationsEnabled: Boolean = true,
    val pausedHandleIds: Set<String> = emptySet(),
) {
    val hideHandleIndicators get() = handles.isNotEmpty() && handles.all { it.indicatorHidden }
    val hiddenIndicatorIds get() = handles.filter { it.indicatorHidden }.mapTo(mutableSetOf()) { it.id }
    fun hideIndicators(hidden: Boolean) = copy(handles = handles.map { it.copy(indicatorHidden = hidden) })
    fun hideIndicators(targets: Set<String>, hidden: Boolean) = copy(handles = handles.map { if (it.id in targets) it.copy(indicatorHidden = hidden) else it })
    fun pauseHandles(targets: Set<String>, paused: Boolean) = copy(pausedHandleIds = if (paused) pausedHandleIds + targets else pausedHandleIds - targets)
    fun normalized(): AppConfiguration {
        val ids = panels.map { it.id }.toSet()
        return copy(pausedHandleIds = pausedHandleIds.intersect(handles.map { it.id }.toSet()),
            brightnessPopup = brightnessPopup.normalized(), shortcuts = shortcuts.map { it.copy(name = it.name.take(80), button = it.button.normalized()) }, panels = panels.map(SavedPanel::normalized), handles = handles.map { h ->
            h.normalized().let { it.copy(bindings = it.bindings.mapValues { (_, target) ->
                if (target.panelId != null && target.panelId !in ids) target.copy(panelId = null) else target
            }) }
        })
    }
    fun handle(id: String?) = handles.firstOrNull { it.id == id }
    fun panel(id: String?) = panels.firstOrNull { it.id == id }
    fun editHandle(id: String, edit: (HandleConfig) -> HandleConfig) = copy(handles = handles.map { if (it.id == id) edit(it) else it })
    fun editPanel(id: String, edit: (SavedPanel) -> SavedPanel) = copy(panels = panels.map { if (it.id == id) edit(it) else it })
    fun removePanel(id: String) = copy(panels = panels.filterNot { it.id == id }, handles = handles.map { h ->
        h.copy(bindings = h.bindings.mapValues { (_, target) -> if (target.panelId == id) target.copy(panelId = null) else target })
    })
    fun addCopies(imported: AppConfiguration): AppConfiguration {
        val copies = imported.panels.associate { it.id to it.duplicate() }
        return copy(panels = panels + imported.panels.map { copies.getValue(it.id) }, handles = handles + imported.handles.map { h ->
            h.duplicate().let { duplicate -> duplicate.copy(bindings = duplicate.bindings.mapValues { (_, target) ->
                target.copy(panelId = target.panelId?.let { copies.getValue(it).id })
            }) }
        })
    }
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
