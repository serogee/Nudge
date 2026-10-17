package com.modukey.app.core

enum class Edge { LEFT, RIGHT, BOTTOM }
enum class Activation { SWIPE, TAP, LONG_PRESS }

data class Placement(val edge: Edge = Edge.RIGHT, val position: Float = 0.65f)

data class TriggerSettings(
    val enabled: Boolean = false,
    val portrait: Placement = Placement(),
    val landscape: Placement = Placement(),
    val landscapeEnabled: Boolean = false,
    val lengthDp: Float = 120f,
    val depthDp: Float = 16f,
    val indicatorVisible: Boolean = false,
    val indicatorLengthDp: Float = 80f,
    val indicatorThicknessDp: Float = 4f,
    val indicatorColor: Long = 0xFFB8D982,
    val indicatorOpacity: Float = 0.65f,
    val cornerRadiusDp: Float = 2f,
    val activation: Activation = Activation.SWIPE,
    val swipeDistanceDp: Float = 24f,
    val swipeTimeoutMs: Long = 600,
    val haptics: Boolean = false,
    val excludedPackages: Set<String> = emptySet(),
) {
    fun placement(isLandscape: Boolean) = if (isLandscape) landscape else portrait

    fun normalized() = copy(
        portrait = portrait.copy(position = portrait.position.finite(0.65f).coerceIn(0f, 1f)),
        landscape = landscape.copy(position = landscape.position.finite(0.65f).coerceIn(0f, 1f)),
        lengthDp = lengthDp.finite(120f).coerceIn(48f, 200f),
        depthDp = depthDp.finite(16f).coerceIn(8f, 32f),
        indicatorLengthDp = indicatorLengthDp.finite(80f).coerceIn(12f, lengthDp.finite(120f).coerceIn(48f, 200f)),
        indicatorThicknessDp = indicatorThicknessDp.finite(4f).coerceIn(1f, depthDp.finite(16f).coerceIn(8f, 32f)),
        indicatorColor = indicatorColor or 0xFF000000,
        indicatorOpacity = indicatorOpacity.finite(0.65f).coerceIn(0f, 1f),
        cornerRadiusDp = cornerRadiusDp.finite(2f).coerceIn(0f, 16f),
        swipeDistanceDp = swipeDistanceDp.finite(24f).coerceIn(12f, 80f),
        swipeTimeoutMs = swipeTimeoutMs.coerceIn(200, 1200),
    )
}

private fun Float.finite(fallback: Float) = if (isFinite()) this else fallback

data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
data class Insets(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f)

object TriggerGeometry {
    fun bounds(width: Float, height: Float, density: Float, insets: Insets, settings: TriggerSettings, landscape: Boolean): Bounds {
        val config = settings.normalized()
        val placement = config.placement(landscape)
        val usableWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
        val usableHeight = (height - insets.top - insets.bottom).coerceAtLeast(0f)
        val horizontal = placement.edge == Edge.BOTTOM
        val length = (config.lengthDp * density).coerceAtMost(if (horizontal) usableWidth else usableHeight)
        val depth = (config.depthDp * density).coerceAtMost(if (horizontal) usableHeight else usableWidth)
        return when (placement.edge) {
            Edge.LEFT -> Bounds(insets.left, insets.top + (usableHeight - length) * placement.position, depth, length)
            Edge.RIGHT -> Bounds(width - insets.right - depth, insets.top + (usableHeight - length) * placement.position, depth, length)
            Edge.BOTTOM -> Bounds(insets.left + (usableWidth - length) * placement.position, height - insets.bottom - depth, length, depth)
        }
    }

    fun indicator(width: Float, height: Float, density: Float, settings: TriggerSettings, edge: Edge): Bounds {
        val horizontal = edge == Edge.BOTTOM
        val length = (settings.indicatorLengthDp * density).coerceAtMost(if (horizontal) width else height)
        val thickness = (settings.indicatorThicknessDp * density).coerceAtMost(if (horizontal) height else width)
        return when (edge) {
            Edge.LEFT -> Bounds(0f, (height - length) / 2, thickness, length)
            Edge.RIGHT -> Bounds(width - thickness, (height - length) / 2, thickness, length)
            Edge.BOTTOM -> Bounds((width - length) / 2, height - thickness, length, thickness)
        }
    }
}
