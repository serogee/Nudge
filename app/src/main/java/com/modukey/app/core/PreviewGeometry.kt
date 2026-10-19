package com.modukey.app.core

/** Logical editor coordinates mapped into the display's current rotation. */
data class PreviewGeometry(val width: Float, val height: Float, val insets: Insets, val landscape: Boolean, val quarterTurns: Int) {
    fun toDisplay(b: Bounds): Bounds = when (quarterTurns) {
        1 -> Bounds(height - b.y - b.height, b.x, b.height, b.width)
        2 -> Bounds(width - b.x - b.width, height - b.y - b.height, b.width, b.height)
        3 -> Bounds(b.y, width - b.x - b.width, b.height, b.width)
        else -> b
    }
    companion object {
        fun calculate(width: Float, height: Float, insets: Insets, rotation: Int, landscape: Boolean): PreviewGeometry {
            val actualLandscape = width > height
            val targetRotation = if (actualLandscape == landscape) rotation else if (landscape) 1 else 0
            val turns = (targetRotation - rotation + 4) % 4
            val logicalInsets = when (turns) {
                1 -> Insets(insets.top, insets.right, insets.bottom, insets.left)
                2 -> Insets(insets.right, insets.bottom, insets.left, insets.top)
                3 -> Insets(insets.bottom, insets.left, insets.top, insets.right)
                else -> insets
            }
            return PreviewGeometry(if (turns % 2 == 0) width else height, if (turns % 2 == 0) height else width,
                logicalInsets, landscape, turns)
        }
    }
}
