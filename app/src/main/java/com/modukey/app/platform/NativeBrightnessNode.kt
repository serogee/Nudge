package com.modukey.app.platform

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** Use brightness resource IDs or an exact native brightness label, never slider position or arbitrary text. */
internal object NativeBrightnessNode {
    const val SYSTEM_UI = "com.android.systemui"
    const val MIUI_PLUGIN = "miui.systemui.plugin"
    private val brightnessIds = setOf("brightness", "brightness_slider", "brightnessslider", "brightness_seekbar", "brightnessseekbar",
        "brightness_seek_bar", "brightness_slidebar", "brightness_bar", "brightnessbar", "brightness_container", "brightness_frame",
        "brightness_mirror", "brightness_mirror_container", "brightness_slider_container", "brightness_bar_container",
        "qs_brightness", "qs_brightness_slider", "control_center_brightness", "control_center_brightness_slider")
    private val sliderIds = setOf("slider", "seekbar", "seek_bar", "progress", "slider_container", "slider_frame",
        "slider_layout", "toggle_slider", "toggle_slider_inner", "seekbar_container", "seek_bar_container")
    fun read(roots: List<AccessibilityNodeInfo>, owners: Set<String> = setOf(SYSTEM_UI), labels: Set<String> = emptySet(),
        report: ((String) -> Unit)? = null): NativeBrightnessControl? = use(roots, owners, labels, report) { _, control -> control }
    fun move(roots: List<AccessibilityNodeInfo>, expected: NativeBrightnessControl, fraction: Float,
        owners: Set<String> = setOf(SYSTEM_UI), labels: Set<String> = emptySet()): Boolean = use(roots, owners, labels) { node, control ->
        if (!expected.sameRange(control)) false else if (control.matches(fraction)) true else node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
            Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, control.target(fraction)) })
    } == true

    @Suppress("DEPRECATION")
    private fun <T> use(roots: List<AccessibilityNodeInfo>, owners: Set<String>, labels: Set<String>, report: ((String) -> Unit)? = null,
        action: (AccessibilityNodeInfo, NativeBrightnessControl) -> T): T? {
        val queue = ArrayDeque<Triple<AccessibilityNodeInfo, Boolean, String>>()
        val owned = mutableListOf<AccessibilityNodeInfo>()
        roots.forEach { queue.add(Triple(it, false, "")); owned += it }
        var selected: Pair<AccessibilityNodeInfo, NativeBrightnessControl>? = null
        var visited = 0
        var identified = 0; var ranges = 0; var candidates = 0; var outcome = "none"
        val details = mutableListOf<String>()
        try {
            while (queue.isNotEmpty()) {
                if (++visited > 512) { outcome = "node_limit"; return null }
                val (node, parentBrightness, parentPath) = queue.removeFirst()
                if (node.packageName?.toString() !in owners) continue
                val id = node.viewIdResourceName.orEmpty()
                // A generic slider is brightness only inside an explicitly identified brightness branch.
                val name = id.substringAfter(":id/", "").lowercase(Locale.ROOT)
                val path = if (report == null) "" else "$parentPath/${name.take(40)}".takeLast(160)
                val nativeLabel = labels.isNotEmpty() && name in sliderIds && node.className?.toString() == "android.widget.SeekBar" &&
                    node.contentDescription?.toString()?.trim() in labels
                val brightness = name in brightnessIds || nativeLabel || (parentBrightness && (name.isEmpty() || name in sliderIds))
                val range = node.rangeInfo
                val writable = node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id }
                if (brightness) identified++
                if (range != null) ranges++
                if (report != null && (brightness || range != null) && details.size < 6) {
                    // Record widget metadata, never text, descriptions, or the user's current brightness.
                    details += "id=${id.take(100)}; path=$path; class=${node.className?.take(80)}; owner=${node.packageName}; " +
                        "brightness=$brightness; visible=${node.isVisibleToUser}; enabled=${node.isEnabled}; progress=$writable; " +
                        "range=${range?.let { "${it.type}:${it.min}..${it.max}" } ?: "none"}"
                }
                if (brightness && node.isVisibleToUser && node.isEnabled && range != null &&
                    writable &&
                    range.min.isFinite() && range.max.isFinite() && range.current.isFinite() && range.max > range.min &&
                    range.current in range.min..range.max && range.type in 0..2 &&
                    (range.type != AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT || range.max - range.min >= 4f)) {
                    val bounds = Rect(); node.getBoundsInScreen(bounds)
                    val control = NativeBrightnessControl("${node.windowId}:$id:${bounds.flattenToString()}", range.min, range.max, range.current,
                        range.type == AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT)
                    candidates++
                    if (selected != null) { outcome = "ambiguous"; return null }
                    selected = node to control
                }
                if (node.childCount > 512 - owned.size) { outcome = "node_limit"; return null }
                repeat(node.childCount) { index -> node.getChild(index)?.let { child -> owned += child; queue.add(Triple(child, brightness, path)) } }
            }
            if (selected != null) outcome = "found"
            return selected?.let { (node, control) -> action(node, control) }
        } catch (_: RuntimeException) { outcome = "read_error"; return null }
        finally {
            owned.forEach { it.recycle() }
            report?.invoke("result=$outcome; roots=${roots.size}; nodes=$visited; brightness=$identified; ranges=$ranges; candidates=$candidates; " +
                details.joinToString(" | "))
        }
    }
}
