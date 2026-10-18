package com.modukey.app.core

enum class Edge { LEFT, RIGHT, BOTTOM }
enum class Activation { SWIPE, TAP, LONG_PRESS }

data class Placement(val edge: Edge = Edge.RIGHT, val position: Float = 0.65f)

data class TriggerSettings(
    val enabled: Boolean = false,
    val portrait: Placement = Placement(),
    val landscape: Placement = Placement(),
    val landscapeEnabled: Boolean = false,
    val landscapeOverrideEnabled: Boolean = false,
    val lockScreenEnabled: Boolean = false,
    val lengthDp: Float = 120f,
    val depthDp: Float = 16f,
    val indicatorVisible: Boolean = false,
    val indicatorLengthFraction: Float = 2f / 3f,
    val indicatorThicknessFraction: Float = 0.25f,
    val indicatorColor: Long = 0xFFB8D982,
    val indicatorOpacity: Float = 0.65f,
    val cornerRadiusDp: Float = 2f,
    val activation: Activation = Activation.SWIPE,
    val swipeDistanceDp: Float = 24f,
    val swipeTimeoutMs: Long = 600,
    val haptics: Boolean = false,
    val excludedPackages: Set<String> = emptySet(),
    val includePrereleases: Boolean = false,
    val longerTouchLength: Boolean = false,
    val softHideEnabled: Boolean = false,
    val softHideDelayMs: Long = 3_000,
) {
    fun placement(isLandscape: Boolean) = if (isLandscape && landscapeOverrideEnabled) landscape else portrait

    fun normalized() = copy(
        portrait = portrait.copy(position = portrait.position.finite(0.65f).coerceIn(0f, 1f)),
        landscape = landscape.copy(position = landscape.position.finite(0.65f).coerceIn(0f, 1f)),
        lengthDp = lengthDp.finite(120f).coerceAtLeast(48f),
        depthDp = depthDp.finite(16f).coerceIn(8f, 32f),
        indicatorLengthFraction = indicatorLengthFraction.finite(2f / 3f).coerceIn(0f, 1f),
        indicatorThicknessFraction = indicatorThicknessFraction.finite(0.25f).coerceIn(0f, 1f),
        indicatorColor = indicatorColor or 0xFF000000,
        indicatorOpacity = indicatorOpacity.finite(0.65f).coerceIn(0f, 1f),
        cornerRadiusDp = cornerRadiusDp.finite(2f).coerceIn(0f, 16f),
        swipeDistanceDp = swipeDistanceDp.finite(24f).coerceIn(12f, 80f),
        swipeTimeoutMs = swipeTimeoutMs.coerceIn(200, 1200),
        softHideDelayMs = softHideDelayMs.coerceIn(1_000, 30_000),
    )
}

private fun Float.finite(fallback: Float) = if (isFinite()) this else fallback

data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
data class Insets(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f)

object TriggerGeometry {
    fun start(position: Float, length: Float, edgeLength: Float): Float =
        (position * edgeLength - length / 2).coerceIn(0f, (edgeLength - length).coerceAtLeast(0f))

    /** Inverse of center placement, including the initial pointer-to-center grab offset. */
    fun draggedPosition(pointer: Float, grabOffset: Float, edgeLength: Float): Float =
        if (edgeLength > 0f) ((pointer - grabOffset) / edgeLength).coerceIn(0f, 1f) else 0f

    fun maximumLengthDp(width: Float, height: Float, density: Float, insets: Insets, longer: Boolean): Float {
        val w = (width - insets.left - insets.right).coerceAtLeast(0f)
        val h = (height - insets.top - insets.bottom).coerceAtLeast(0f)
        return ((if (longer) maxOf(w, h) else minOf(w, h)) / density.coerceAtLeast(.01f)).coerceAtLeast(48f)
    }

    fun bounds(width: Float, height: Float, density: Float, insets: Insets, settings: TriggerSettings, landscape: Boolean): Bounds {
        val config = settings.normalized()
        val placement = config.placement(landscape)
        val usableWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
        val usableHeight = (height - insets.top - insets.bottom).coerceAtLeast(0f)
        val horizontal = placement.edge == Edge.BOTTOM
        val maximum = maximumLengthDp(width, height, density, insets, config.longerTouchLength)
        val length = (minOf(config.lengthDp, maximum) * density).coerceAtMost(if (horizontal) usableWidth else usableHeight)
        val depth = (config.depthDp * density).coerceAtMost(if (horizontal) usableHeight else usableWidth)
        return when (placement.edge) {
            Edge.LEFT -> Bounds(insets.left, insets.top + start(placement.position, length, usableHeight), depth, length)
            Edge.RIGHT -> Bounds(width - insets.right - depth, insets.top + start(placement.position, length, usableHeight), depth, length)
            Edge.BOTTOM -> Bounds(insets.left + start(placement.position, length, usableWidth), height - insets.bottom - depth, length, depth)
        }
    }

    fun indicator(width: Float, height: Float, settings: TriggerSettings, edge: Edge): Bounds {
        val horizontal = edge == Edge.BOTTOM
        val length = settings.indicatorLengthFraction * (if (horizontal) width else height)
        val thickness = settings.indicatorThicknessFraction * (if (horizontal) height else width)
        return when (edge) {
            Edge.LEFT -> Bounds(0f, (height - length) / 2, thickness, length)
            Edge.RIGHT -> Bounds(width - thickness, (height - length) / 2, thickness, length)
            Edge.BOTTOM -> Bounds((width - length) / 2, height - thickness, length, thickness)
        }
    }
}
