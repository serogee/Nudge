package com.modukey.app.core

/** Hover is transient selection, independent of a button's persistent toggle state. */
data class WheelHoverAppearance(
    val enabled: Boolean = true,
    val useIcon: Boolean = false, val icon: Long = 0xFFFFFFFF,
    val useBackground: Boolean = false, val background: Long = 0xFF505052,
    val useHighlight: Boolean = false, val highlight: Long = 0xFFB8D982,
) {
    fun normalized() = copy(icon = icon or 0xFF000000, background = background or 0xFF000000, highlight = highlight or 0xFF000000)
}
