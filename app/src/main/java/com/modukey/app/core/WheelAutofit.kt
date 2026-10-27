package com.modukey.app.core

/** Store fitted values in the layout so disabling autofit never restores an older size. */
object WheelAutofit {
    fun fit(settings: WheelLayoutSettings, screen: Bounds, density: Float, count: Int, edge: Edge): WheelLayoutSettings? {
        if (!settings.autoFit) return settings
        val maximum = WheelGeometry.maximumOuterDp(screen, density, edge)
        val wanted = count.coerceIn(1, 6)
        val s = settings.normalized()
        // Keep the selected size and minimize the entire outside radius.
        // At a fixed outside radius, a 48 dp band puts target centers furthest from the origin.
        // A thicker band cannot fit more targets at a smaller outside radius.
        val scale = s.selectedScale
        for (outer in (WheelLayoutSettings.MIN_AUTOFIT_INNER_RADIUS_DP.toInt() + 48)..336) {
            val inner = outer - 48f
            if (inner + 48f * scale > maximum) break
            if (!WheelGeometry.fitsTargets(inner, 48f, scale, wanted, s.gapDp)) continue
            return s.copy(innerRadiusDp = inner, outerRadiusDp = outer.toFloat(), expandSelection = true,
                iconPosition = WheelLayoutSettings.DEFAULT_ICON_POSITION)
        }
        return null
    }
}

/** Manual radius and band edits cancel fitting. Selected size remains an input to fitting. */
fun PanelLayout.editSize(change: (PanelLayout) -> PanelLayout): PanelLayout {
    val after = change(this)
    val beforeWheel = wheel
    val afterWheel = after.wheel
    val changed = buttonSizeDp != after.buttonSizeDp || beforeWheel.radiusDp != afterWheel.radiusDp ||
        beforeWheel.innerRadiusDp != afterWheel.innerRadiusDp || beforeWheel.outerRadiusDp != afterWheel.outerRadiusDp ||
        beforeWheel.expandSelection != afterWheel.expandSelection
    return if (changed) after.copy(wheel = afterWheel.copy(autoFit = false)) else after
}
