package com.modukey.app.core

/** Shared by the toggle buttons in a panel. Images retain their original artwork. */
data class ActiveButtonAppearance(
    val useBackground: Boolean = true, val useIcon: Boolean = true,
    val automaticBackground: Boolean = true, val background: Long = 0xFF3482FF,
    val automaticIcon: Boolean = true, val icon: Long = 0xFFFFFFFF,
) {
    fun normalized() = copy(background = background or 0xFF000000, icon = icon or 0xFF000000)
}
