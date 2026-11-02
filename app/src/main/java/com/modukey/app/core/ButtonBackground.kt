package com.modukey.app.core

enum class ButtonBackgroundMode { AUTOMATIC, TRANSPARENT, CUSTOM }

/** Normal button backgrounds are independent of the artwork and active colors. */
data class ButtonBackground(val mode: ButtonBackgroundMode = ButtonBackgroundMode.AUTOMATIC,
    val color: Long = 0xFF3C3C3E) {
    fun normalized() = copy(color = (color and 0xFFFFFF) or 0xFF000000)
}

/** DEFAULT remains readable for older callers; editors and migrated backups use explicit styles. */
fun PanelButton.explicitAppearance(defaultContainer: Boolean): PanelButton {
    if (iconContainer != IconContainer.DEFAULT) return this
    return copy(iconContainer = if (defaultContainer) IconContainer.CONTAINER else IconContainer.STANDALONE,
        background = if (defaultContainer || (action == ButtonAction.EMPTY && emptyAppearance == EmptyAppearance.NO_ICON)) background
            else background.copy(mode = ButtonBackgroundMode.TRANSPARENT))
}
