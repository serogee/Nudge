package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

typealias MigratedPanelName = (String, Int, Int) -> String

/** Schema 11 adds brightness slider touch behavior. */
object ConfigurationCodec {
    const val MAX_BYTES = 1_048_576
    const val SCHEMA = 11
    private val defaultName: MigratedPanelName = { name, handle, slot -> "${name.ifBlank { (handle + 1).toString() }} / ${slot + 1}" }
    fun encode(config: AppConfiguration): String = JSONObject().put("schema", SCHEMA).put("enabled", config.enabled)
        .put("includePrereleases", config.includePrereleases).put("handles", array(config.handles.map(::handle)))
        .put("panels", array(config.panels.map { panel(it.settings).put("id", it.id).put("name", it.name) }))
        .put("brightnessPopup", brightness(config.brightnessPopup))
        .put("shortcuts", array(config.shortcuts.map { JSONObject().put("id", it.id).put("name", it.name).put("button", button(it.button)).put("handleId", it.handleId.orEmpty()) })).toString()

    fun upgrade(text: String, name: MigratedPanelName = defaultName): String = guarded {
        if (JSONObject(text).getInt("schema") in 1 until SCHEMA) encode(decode(text, checkBackupSize = false, migratedName = name)) else text
    }
    fun decode(text: String, checkBackupSize: Boolean = true, migratedName: MigratedPanelName = defaultName): AppConfiguration {
        if (checkBackupSize && text.toByteArray(Charsets.UTF_8).size > MAX_BYTES) throw IOException("backup_size")
        return guarded {
            val root = JSONObject(text)
            val schema = root.getInt("schema")
            if (schema !in 1..SCHEMA) throw IOException("configuration_version")
            val panels = if (schema >= 2) root.getJSONArray("panels").objects().map { SavedPanel(it.getString("id"), it.optString("name"), panel(it, schema)) }.toMutableList()
                else mutableListOf()
            val handles = root.getJSONArray("handles").objects().mapIndexed { handleIndex, h ->
                val id = h.getString("id")
                val bindings = h.getJSONObject("bindings")
                val oldPanels = if (schema == 1) h.getJSONArray("panels").objects().map { panel(it, schema) }.also { require(it.size == 3) } else emptyList()
                val oldTargets = if (schema == 1) GestureSlot.entries.associateWith { slot ->
                    bindings.optString(slot.name, "NONE").also { require(it in listOf("NONE", "VOLUME", "PANEL_1", "PANEL_2", "PANEL_3")) }
                } else emptyMap()
                val ids = oldPanels.mapIndexed { index, p ->
                    val panelId = UUID.nameUUIDFromBytes("nudge-panel:$id:$index".toByteArray(Charsets.UTF_8)).toString()
                    if (p.normalized() != PanelConfig().normalized() || oldTargets.values.any { it == "PANEL_${index + 1}" })
                        panels += SavedPanel(panelId, migratedName(h.optString("name"), handleIndex, index), p)
                    panelId
                }
                val targets = GestureSlot.entries.associateWith { slot ->
                    if (schema == 1) when(val value = oldTargets.getValue(slot)) {
                        "NONE" -> GestureTarget.NONE
                        "VOLUME" -> GestureTarget.VOLUME
                        else -> GestureTarget.panel(ids[value.last().digitToInt() - 1])
                    } else if (!bindings.has(slot.name)) GestureTarget.NONE else bindings.getJSONObject(slot.name).let {
                        GestureTarget(it.optString("panelId").ifBlank { null }, it.optBoolean("volume"),
                            if (it.has("action") && !it.isNull("action")) button(it.getJSONObject("action")) else null, it.optBoolean("brightness")).also { target ->
                            require(target.action == null || (target.panelId == null && (schema >= 4 || !target.volume)))
                            require(target.action?.action != ButtonAction.EMPTY)
                        }
                    }
                }
                HandleConfig(id, h.optString("name"), h.optBoolean("enabled"), trigger(h.getJSONObject("trigger"), schema), targets, h.optLong("volumeSequenceMs", 3_000))
            }
            require(handles.all { it.id.isNotBlank() } && handles.map { it.id }.distinct().size == handles.size)
            require(panels.all { it.id.isNotBlank() } && panels.map { it.id }.distinct().size == panels.size)
            val shortcuts = root.optJSONArray("shortcuts")?.objects()?.map {
                HomeShortcut(it.getString("id"), it.optString("name"), button(it.getJSONObject("button")), it.optString("handleId").ifBlank { null })
            }.orEmpty()
            require(shortcuts.all { it.id.isNotBlank() && it.button.action != ButtonAction.EMPTY } && shortcuts.map { it.id }.distinct().size == shortcuts.size)
            val buttons = shortcuts.map { it.button } + panels.flatMap { it.settings.buttons } + handles.flatMap { h -> h.bindings.values.mapNotNull { it.action } }
            require(buttons.all { it.id.isNotBlank() } && buttons.map { it.id }.distinct().size == buttons.size)
            val panelIds = panels.map { it.id }.toSet()
            require(handles.all { h -> h.bindings.values.all { it.panelId == null || it.panelId in panelIds } })
            AppConfiguration(root.optBoolean("enabled"), root.optBoolean("includePrereleases"), handles, panels, if (schema >= 7) brightness(root.optJSONObject("brightnessPopup") ?: JSONObject(), schema) else BrightnessPopupSettings(), shortcuts).normalized()
        }
    }
    private inline fun <T> guarded(block: () -> T): T = try { block() }
        catch (e: IOException) { throw e }
        catch (e: Exception) { throw IOException("configuration_invalid", e) }
    private fun array(values: List<Any>) = JSONArray().apply { values.forEach(::put) }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.float(name: String, default: Float) = optDouble(name, default.toDouble()).toFloat()
    private fun placement(p: JSONObject) = Placement(enumValueOf(p.getString("edge")), p.float("position", .65f))
    private fun placement(p: Placement) = JSONObject().put("edge", p.edge.name).put("position", p.position)
    private fun layout(p: JSONObject, schema: Int) = PanelLayout(enumValueOf(p.optString("mode", "FIXED")), enumValueOf(p.optString("direction", "VERTICAL")),
        p.optInt("crossCount", 1), p.optInt("maximumLines", 5), p.float("extent", .6f), p.float("buttonSize", 56f),
        p.optBoolean("follow", true), p.float("x", .5f), p.float("y", .5f), p.float("offsetX", 0f), p.float("offsetY", 0f).let { if (schema < 4 && it != 0f) -it else it })
    private fun layout(p: PanelLayout) = JSONObject().put("mode", p.mode.name).put("direction", p.direction.name)
        .put("crossCount", p.crossCount).put("maximumLines", p.maximumLines).put("extent", p.extentFraction)
        .put("buttonSize", p.buttonSizeDp).put("follow", p.followHandle).put("x", p.x).put("y", p.y)
        .put("offsetX", p.offsetXDp).put("offsetY", p.offsetYDp)
    private fun trigger(t: JSONObject, schema: Int) = TriggerSettings(
        portrait = placement(t.getJSONObject("portrait")), landscape = placement(t.getJSONObject("landscape")),
        landscapeEnabled = t.optBoolean("landscapeEnabled"), landscapeOverrideEnabled = t.optBoolean("landscapeOverride"),
        lockScreenEnabled = t.optBoolean("lockScreen"), lengthDp = t.float("length", 120f), depthDp = t.float("depth", 16f),
        indicatorVisible = t.optBoolean("visible"), indicatorLengthFraction = t.float("indicatorLength", 2f / 3f),
        indicatorThicknessDp = if (schema >= 5) t.float("indicatorThicknessDp", 4f)
            else thicknessDpFromFraction(t.float("indicatorThickness", .25f), t.float("depth", 16f)),
        indicatorColor = t.optLong("color", 0xFFB8D982),
        indicatorOpacity = t.float("opacity", .65f), cornerRadiusDp = t.float("corner", 2f),
        swipeDistanceDp = t.float("swipeDistance", 24f), swipeTimeoutMs = t.optLong("swipeTime", 600), haptics = t.optBoolean("haptics"),
        excludedPackages = t.optJSONArray("excluded")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet(),
        longerTouchLength = t.optBoolean("longerTouchLength"), softHideEnabled = t.optBoolean("softHideEnabled"),
        softHideDelayMs = t.optLong("softHideDelayMs", 3_000),
        holdTimeMs = t.optLong("holdTimeMs", 500), swipeHoldTimeMs = t.optLong("swipeHoldTimeMs", 500),
        swipeHoldDistanceDp = t.float("swipeHoldDistanceDp", if (schema < 9) t.float("swipeDistance", 24f) else 24f),
        swipeHoldTimeoutMs = t.optLong("swipeHoldTimeoutMs", if (schema < 9) t.optLong("swipeTime", 600) else 600),
        holdExecution = enumValueOf(t.optString("holdExecution", "ON_RELEASE")),
        swipeHoldExecution = enumValueOf(t.optString("swipeHoldExecution", "ON_RELEASE")),
        swipeFeedback = feedback(t, "swipeFeedback"), swipeHoldFeedback = feedback(t, "swipeHoldFeedback"),
        holdFeedback = feedback(t, "holdFeedback"), tapFeedback = feedback(t, "tapFeedback"))
    private fun feedback(t: JSONObject, key: String): GestureFeedback = t.optJSONObject(key)?.let {
        GestureFeedback(it.optBoolean("enabled"), it.float("strength", .5f))
    } ?: GestureFeedback(t.optBoolean("haptics"))
    private fun feedback(s: GestureFeedback) = JSONObject().put("enabled", s.enabled).put("strength", s.strength)
    private fun handle(h: HandleConfig): JSONObject {
        val s = h.trigger
        val trigger = JSONObject().put("portrait", placement(s.portrait)).put("landscape", placement(s.landscape))
            .put("landscapeEnabled", s.landscapeEnabled).put("landscapeOverride", s.landscapeOverrideEnabled).put("lockScreen", s.lockScreenEnabled)
            .put("length", s.lengthDp).put("depth", s.depthDp).put("visible", s.indicatorVisible)
            .put("indicatorLength", s.indicatorLengthFraction).put("indicatorThicknessDp", s.indicatorThicknessDp)
            .put("color", s.indicatorColor).put("opacity", s.indicatorOpacity).put("corner", s.cornerRadiusDp)
            .put("swipeDistance", s.swipeDistanceDp).put("swipeTime", s.swipeTimeoutMs).put("haptics", s.haptics)
            .put("excluded", array(s.excludedPackages.sorted())).put("longerTouchLength", s.longerTouchLength)
            .put("softHideEnabled", s.softHideEnabled).put("softHideDelayMs", s.softHideDelayMs)
            .put("holdTimeMs", s.holdTimeMs).put("swipeHoldTimeMs", s.swipeHoldTimeMs)
            .put("swipeHoldDistanceDp", s.swipeHoldDistanceDp).put("swipeHoldTimeoutMs", s.swipeHoldTimeoutMs)
            .put("holdExecution", s.holdExecution.name).put("swipeHoldExecution", s.swipeHoldExecution.name)
            .put("swipeFeedback", feedback(s.swipeFeedback)).put("swipeHoldFeedback", feedback(s.swipeHoldFeedback))
            .put("holdFeedback", feedback(s.holdFeedback)).put("tapFeedback", feedback(s.tapFeedback))
        val bindings = JSONObject().apply { GestureSlot.entries.forEach {
            val target = h.target(it)
            put(it.name, JSONObject().put("panelId", target.panelId.orEmpty()).put("volume", target.volume).put("brightness", target.brightness)
                .apply { target.action?.let { put("action", button(it)) } })
        } }
        return JSONObject().put("id", h.id).put("name", h.name).put("enabled", h.enabled).put("trigger", trigger)
            .put("bindings", bindings).put("volumeSequenceMs", h.volumeSequenceMs)
    }
    private fun brightness(p: JSONObject, schema: Int) = BrightnessPopupSettings(
        side = enumValueOf(p.optString("side", "OPPOSITE_VOLUME")), volumeSide = enumValueOf(p.optString("volumeSide", "RIGHT")),
        widthDp = p.float("width", 64f), heightDp = p.float("height", 240f), x = p.float("x", .5f), y = if (schema < 8) 1f - p.float("y", .35f) else p.float("y", .65f),
        background = p.optLong("background", 0xFF202122), track = p.optLong("track", 0xFF3C3C3E), fill = p.optLong("fill", 0xFF999A9C), icon = p.optLong("icon", 0xFFECECEE),
        backgroundOpacity = p.float("backgroundOpacity", 1f), trackOpacity = p.float("trackOpacity", 1f), fillOpacity = p.float("fillOpacity", 1f), iconOpacity = p.float("iconOpacity", 1f),
        timeoutMs = p.optLong("timeoutMs", 3_000), cornerRadiusDp = p.float("cornerRadiusDp", 32f),
        showBackground = p.optBoolean("showBackground", true), showAutomaticToggle = p.optBoolean("showAutomaticToggle", true),
        touchBehavior = enumValueOf(p.optString("touchBehavior", "JUMP_TO_TOUCH")))
    private fun brightness(p: BrightnessPopupSettings) = JSONObject().put("side", p.side.name).put("volumeSide", p.volumeSide.name)
        .put("width", p.widthDp).put("height", p.heightDp).put("x", p.x).put("y", p.y)
        .put("background", p.background).put("track", p.track).put("fill", p.fill).put("icon", p.icon)
        .put("backgroundOpacity", p.backgroundOpacity).put("trackOpacity", p.trackOpacity).put("fillOpacity", p.fillOpacity).put("iconOpacity", p.iconOpacity)
        .put("timeoutMs", p.timeoutMs).put("cornerRadiusDp", p.cornerRadiusDp)
        .put("showBackground", p.showBackground).put("showAutomaticToggle", p.showAutomaticToggle).put("touchBehavior", p.touchBehavior.name)
    private fun button(b: JSONObject) = PanelButton(b.getString("id"), enumValueOf(b.getString("action")), b.optString("appPackage"), b.optString("appActivity"),
        b.optString("appLabel"), IconReference(b.optString("iconPackage"), b.optString("iconDrawable"),
            b.optString("builtInIcon").takeIf { it.isNotBlank() }?.let { enumValueOf<BuiltInIcon>(it) }),
        b.optBoolean("showVolumePopup"), enumValueOf(b.optString("iconContainer", "DEFAULT")), enumValueOf(b.optString("emptyAppearance", "SLASH")),
        enumValueOf(b.optString("handleScope", "ALL")),
        b.optJSONObject("brightnessReturn")?.let { BrightnessReturnSettings(it.optBoolean("usePrevious", true), it.float("fraction", .5f)) }
            ?: BrightnessReturnSettings())
    private fun button(b: PanelButton) = JSONObject().put("id", b.id).put("action", b.action.name)
        .put("appPackage", b.appPackage).put("appActivity", b.appActivity).put("appLabel", b.appLabel)
        .put("iconPackage", b.icon.packageName).put("iconDrawable", b.icon.drawable).put("builtInIcon", b.icon.builtIn?.name.orEmpty())
        .put("showVolumePopup", b.showVolumePopup).put("iconContainer", b.iconContainer.name).put("emptyAppearance", b.emptyAppearance.name)
        .put("handleScope", b.handleScope.name)
        .put("brightnessReturn", JSONObject().put("usePrevious", b.brightnessReturn.usePrevious).put("fraction", b.brightnessReturn.fraction))
    private fun panel(p: JSONObject, schema: Int) = PanelConfig(
        buttons = p.getJSONArray("buttons").objects().map(::button), portrait = layout(p.getJSONObject("portrait"), schema), landscape = layout(p.getJSONObject("landscape"), schema),
        landscapeOverride = p.optBoolean("landscapeOverride"), outsideTouch = enumValueOf(p.optString("outside", "BLOCK")),
        background = p.optLong("background", 0xFF252A21), opacity = p.float("opacity", .98f), cornerDp = p.float("corner", 24f),
        iconShape = enumValueOf(p.optString("shape", "ROUNDED_SQUARE")), iconCornerFraction = p.float("iconCorner", .25f), iconPack = p.optString("iconPack"),
        animation = enumValueOf(p.optString("animation", "NONE")), iconBackground = p.optBoolean("iconBackground", true),
        automaticButtonColor = p.optBoolean("automaticButtonColor", true), buttonColor = p.optLong("buttonColor", 0xFF3C3C3E),
        automaticIconColor = p.optBoolean("automaticIconColor", true), iconColor = p.optLong("iconColor", 0xFFFFFFFF),
        autoCloseEnabled = p.optBoolean("autoCloseEnabled"), autoCloseMs = p.optLong("autoCloseMs", 3_000),
        closeAnimation = enumValueOf(p.optString("closeAnimation", p.optString("animation", "NONE"))),
        buttonOpacity = p.float("buttonOpacity", 1f), iconOpacity = p.float("iconOpacity", 1f))
    private fun panel(p: PanelConfig): JSONObject = JSONObject().put("portrait", layout(p.portrait)).put("landscape", layout(p.landscape)).put("landscapeOverride", p.landscapeOverride)
        .put("outside", p.outsideTouch.name).put("background", p.background).put("opacity", p.opacity).put("corner", p.cornerDp)
        .put("shape", p.iconShape.name).put("iconCorner", p.iconCornerFraction).put("iconPack", p.iconPack)
        .put("animation", p.animation.name).put("iconBackground", p.iconBackground)
        .put("automaticButtonColor", p.automaticButtonColor).put("buttonColor", p.buttonColor)
        .put("automaticIconColor", p.automaticIconColor).put("iconColor", p.iconColor)
        .put("autoCloseEnabled", p.autoCloseEnabled).put("autoCloseMs", p.autoCloseMs)
        .put("closeAnimation", p.closeAnimation.name).put("buttonOpacity", p.buttonOpacity).put("iconOpacity", p.iconOpacity)
        .put("buttons", array(p.buttons.map(::button)))
}
