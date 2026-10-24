package com.modukey.app.core

import kotlin.math.floor

/** Store fitted values in the layout so disabling autofit never restores an older size. */
object WheelAutofit {
    fun fit(settings: WheelLayoutSettings, screen: Bounds, density: Float, count: Int, edge: Edge): WheelLayoutSettings? {
        if (!settings.autoFit) return settings
        val maximum = WheelGeometry.maximumOuterDp(screen, density, edge)
        val wanted = count.coerceIn(1, 6)
        var best: WheelLayoutSettings? = null
        // Earlier priorities win: minimum inner radius, closest to 110%, then maximum outer radius.
        // Whole dp and percentage steps match the editor's displayed precision.
        for (percent in 110 downTo 100) {
            val scale = percent / 100f
            for (band in 335 downTo 48) {
                var high = floor(minOf(288f, 336f - band, maximum - band * scale,
                    best?.innerRadiusDp ?: 288f)).toInt()
                if (high < 1 || !WheelGeometry.fitsTargets(high.toFloat(), band.toFloat(), scale, wanted)) continue
                var low = 1
                while (low < high) {
                    val middle = (low + high) / 2
                    if (WheelGeometry.fitsTargets(middle.toFloat(), band.toFloat(), scale, wanted)) high = middle
                    else low = middle + 1
                }
                val inner = low.toFloat()
                val outer = inner + band
                val previous = best
                if (previous == null || inner < previous.innerRadiusDp!! ||
                    (inner == previous.innerRadiusDp && (scale > previous.selectedScale ||
                        (scale == previous.selectedScale && outer > previous.outerRadiusDp!!)))) {
                    best = settings.copy(innerRadiusDp = inner, outerRadiusDp = outer,
                        expandSelection = true, selectedScale = scale)
                }
                if (inner == 1f) return best
            }
        }
        return best
    }
}

/** Manual size edits cancel fitting. Position, animation, and appearance edits do not. */
fun PanelLayout.editSize(change: (PanelLayout) -> PanelLayout): PanelLayout {
    val after = change(this)
    val beforeWheel = wheel
    val afterWheel = after.wheel
    val changed = buttonSizeDp != after.buttonSizeDp || beforeWheel.radiusDp != afterWheel.radiusDp ||
        beforeWheel.innerRadiusDp != afterWheel.innerRadiusDp || beforeWheel.outerRadiusDp != afterWheel.outerRadiusDp ||
        beforeWheel.expandSelection != afterWheel.expandSelection || beforeWheel.selectedScale != afterWheel.selectedScale
    return if (changed) after.copy(wheel = afterWheel.copy(autoFit = false)) else after
}
