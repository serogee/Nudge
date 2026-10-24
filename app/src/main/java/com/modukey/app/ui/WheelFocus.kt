package com.modukey.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.modukey.app.core.WheelFocusAnimation
import com.modukey.app.core.WheelLayoutSettings

@Composable internal fun wheelFocusWeights(settings: WheelLayoutSettings, count: Int, selected: Int?): List<Float> {
    val s = settings.normalized()
    val spec: AnimationSpec<Float> = if (!s.animationsEnabled || !ValueAnimator.areAnimatorsEnabled()) snap() else when (s.focusAnimation) {
        WheelFocusAnimation.NONE -> snap()
        WheelFocusAnimation.SMOOTH -> tween(s.focusDurationMs, easing = FastOutSlowInEasing)
        WheelFocusAnimation.SPRING -> spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    }
    // Each running animation retargets from its current value; hit testing uses the completed target geometry.
    return List(count) { index ->
        val weight by animateFloatAsState(if (index == selected) 1f else 0f, spec, label = "wheel-focus-$index")
        weight.coerceIn(0f, 1f)
    }
}
