package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Explicit JSON resources only. No executable data, icon bytes, or transient service state. */
object ConfigurationCodec {
    const val MAX_BYTES = 1_048_576
    fun encode(config: AppConfiguration): String = JSONObject().put("schema", 1).put("enabled", config.enabled)
        .put("includePrereleases", config.includePrereleases).put("handles", array(config.handles.map(::handle))).toString()

    fun decode(text: String): AppConfiguration {
        if (text.toByteArray(Charsets.UTF_8).size > MAX_BYTES) throw IOException("backup_size")
        try {
            val root = JSONObject(text)
            if (root.getInt("schema") != 1) throw IOException("configuration_version")
            val handles = root.getJSONArray("handles").objects().map { h ->
                val t = h.getJSONObject("trigger")
                val trigger = TriggerSettings(
                    portrait = placement(t.getJSONObject("portrait")), landscape = placement(t.getJSONObject("landscape")),
                    landscapeEnabled = t.optBoolean("landscapeEnabled"), landscapeOverrideEnabled = t.optBoolean("landscapeOverride"),
                    lockScreenEnabled = t.optBoolean("lockScreen"), lengthDp = t.float("length", 120f), depthDp = t.float("depth", 16f),
                    indicatorVisible = t.optBoolean("visible"), indicatorLengthFraction = t.float("indicatorLength", 2f / 3f),
                    indicatorThicknessFraction = t.float("indicatorThickness", 0.25f), indicatorColor = t.optLong("color", 0xFFB8D982),
                    indicatorOpacity = t.float("opacity", 0.65f), cornerRadiusDp = t.float("corner", 2f),
                    swipeDistanceDp = t.float("swipeDistance", 24f), swipeTimeoutMs = t.optLong("swipeTime", 600), haptics = t.optBoolean("haptics"),
                    excludedPackages = t.optJSONArray("excluded")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet())
                val bindings = h.getJSONObject("bindings")
                val panels = h.getJSONArray("panels").objects().map { p ->
                    PanelConfig(buttons = p.getJSONArray("buttons").objects().map { b ->
                        PanelButton(b.getString("id"), enumValueOf(b.getString("action")), b.optString("appPackage"), b.optString("appActivity"),
                            b.optString("appLabel"), IconReference(b.optString("iconPackage"), b.optString("iconDrawable")))
                    }, portrait = layout(p.getJSONObject("portrait")), landscape = layout(p.getJSONObject("landscape")),
                        landscapeOverride = p.optBoolean("landscapeOverride"), outsideTouch = enumValueOf(p.optString("outside", "BLOCK")),
                        background = p.optLong("background", 0xFF252A21), opacity = p.float("opacity", 0.98f), cornerDp = p.float("corner", 24f),
                        iconShape = enumValueOf(p.optString("shape", "ROUNDED_SQUARE")), iconCornerFraction = p.float("iconCorner", 0.25f), iconPack = p.optString("iconPack"))
                }
                require(panels.size == 3)
                HandleConfig(h.getString("id"), h.optString("name"), h.optBoolean("enabled"), trigger,
                    GestureSlot.entries.associateWith { enumValueOf<GestureTarget>(bindings.optString(it.name, "NONE")) }, h.optLong("volumeSequenceMs", 3_000), panels)
            }
            require(handles.all { it.id.isNotBlank() } && handles.map { it.id }.distinct().size == handles.size)
            val buttons = handles.flatMap { it.panels }.flatMap { it.buttons }
            require(buttons.all { it.id.isNotBlank() } && buttons.map { it.id }.distinct().size == buttons.size)
            return AppConfiguration(root.optBoolean("enabled"), root.optBoolean("includePrereleases"), handles).normalized()
        } catch (e: IOException) { throw e }
        catch (e: Exception) { throw IOException("configuration_invalid", e) }
    }
    private fun array(values: List<Any>) = JSONArray().apply { values.forEach(::put) }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.float(name: String, default: Float) = optDouble(name, default.toDouble()).toFloat()
    private fun placement(p: JSONObject) = Placement(enumValueOf(p.getString("edge")), p.float("position", 0.65f))
    private fun placement(p: Placement) = JSONObject().put("edge", p.edge.name).put("position", p.position)
    private fun layout(p: JSONObject) = PanelLayout(enumValueOf(p.optString("mode", "FIXED")), enumValueOf(p.optString("direction", "VERTICAL")),
        p.optInt("crossCount", 1), p.optInt("maximumLines", 5), p.float("extent", 0.6f), p.float("buttonSize", 56f),
        p.optBoolean("follow", true), p.float("x", 0.5f), p.float("y", 0.5f))
    private fun layout(p: PanelLayout) = JSONObject().put("mode", p.mode.name).put("direction", p.direction.name)
        .put("crossCount", p.crossCount).put("maximumLines", p.maximumLines).put("extent", p.extentFraction)
        .put("buttonSize", p.buttonSizeDp).put("follow", p.followHandle).put("x", p.x).put("y", p.y)
    private fun handle(h: HandleConfig): JSONObject {
        val s = h.trigger
        val trigger = JSONObject().put("portrait", placement(s.portrait)).put("landscape", placement(s.landscape))
            .put("landscapeEnabled", s.landscapeEnabled).put("landscapeOverride", s.landscapeOverrideEnabled).put("lockScreen", s.lockScreenEnabled)
            .put("length", s.lengthDp).put("depth", s.depthDp).put("visible", s.indicatorVisible)
            .put("indicatorLength", s.indicatorLengthFraction).put("indicatorThickness", s.indicatorThicknessFraction)
            .put("color", s.indicatorColor).put("opacity", s.indicatorOpacity).put("corner", s.cornerRadiusDp)
            .put("swipeDistance", s.swipeDistanceDp).put("swipeTime", s.swipeTimeoutMs).put("haptics", s.haptics)
            .put("excluded", array(s.excludedPackages.sorted()))
        val bindings = JSONObject().apply { GestureSlot.entries.forEach { put(it.name, h.target(it).name) } }
        return JSONObject().put("id", h.id).put("name", h.name).put("enabled", h.enabled).put("trigger", trigger)
            .put("bindings", bindings).put("volumeSequenceMs", h.volumeSequenceMs).put("panels", array(h.panels.map { p ->
                JSONObject().put("portrait", layout(p.portrait)).put("landscape", layout(p.landscape)).put("landscapeOverride", p.landscapeOverride)
                    .put("outside", p.outsideTouch.name).put("background", p.background).put("opacity", p.opacity).put("corner", p.cornerDp)
                    .put("shape", p.iconShape.name).put("iconCorner", p.iconCornerFraction).put("iconPack", p.iconPack)
                    .put("buttons", array(p.buttons.map { b -> JSONObject().put("id", b.id).put("action", b.action.name)
                        .put("appPackage", b.appPackage).put("appActivity", b.appActivity).put("appLabel", b.appLabel)
                        .put("iconPackage", b.icon.packageName).put("iconDrawable", b.icon.drawable) }))
            }))
    }
}
