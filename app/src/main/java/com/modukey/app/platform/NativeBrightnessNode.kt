package com.modukey.app.platform

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/** Resource IDs identify brightness. Never infer it from arbitrary slider positions or UI text. */
internal object NativeBrightnessNode {
    const val SYSTEM_UI = "com.android.systemui"
    fun read(roots: List<AccessibilityNodeInfo>): NativeBrightnessControl? = use(roots) { _, control -> control }
    fun move(roots: List<AccessibilityNodeInfo>, expected: NativeBrightnessControl, fraction: Float): Boolean = use(roots) { node, control ->
        if (!expected.sameRange(control)) false else node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
            Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, control.target(fraction)) })
    } == true

    @Suppress("DEPRECATION")
    private fun <T> use(roots: List<AccessibilityNodeInfo>, action: (AccessibilityNodeInfo, NativeBrightnessControl) -> T): T? {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Boolean>>()
        val owned = mutableListOf<AccessibilityNodeInfo>()
        roots.forEach { queue.add(it to false); owned += it }
        var selected: Pair<AccessibilityNodeInfo, NativeBrightnessControl>? = null
        var visited = 0
        try {
            while (queue.isNotEmpty()) {
                if (++visited > 512) return null
                val (node, parentBrightness) = queue.removeFirst()
                if (node.packageName?.toString() != SYSTEM_UI) continue
                val id = node.viewIdResourceName.orEmpty()
                val brightness = parentBrightness || (id.startsWith("$SYSTEM_UI:id/") && id.substringAfter(":id/").contains("brightness", true))
                val range = node.rangeInfo
                if (brightness && node.isVisibleToUser && node.isEnabled && range != null &&
                    node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id } &&
                    range.min.isFinite() && range.max.isFinite() && range.current.isFinite() && range.max > range.min &&
                    range.current in range.min..range.max && range.type in 0..2 &&
                    (range.type != AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT || range.max - range.min >= 4f)) {
                    val bounds = Rect(); node.getBoundsInScreen(bounds)
                    val control = NativeBrightnessControl("${node.windowId}:$id:${bounds.flattenToString()}", range.min, range.max, range.current,
                        range.type == AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT)
                    if (selected != null) return null
                    selected = node to control
                }
                if (node.childCount > 512 - owned.size) return null
                repeat(node.childCount) { index -> node.getChild(index)?.let { child -> owned += child; queue.add(child to brightness) } }
            }
            return selected?.let { (node, control) -> action(node, control) }
        } catch (_: RuntimeException) { return null }
        finally { owned.forEach { it.recycle() } }
    }
}
