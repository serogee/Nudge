package com.modukey.app.platform

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.PathInterpolator
import com.modukey.app.core.Edge
import com.modukey.app.core.PanelAnimation
import com.modukey.app.core.Bounds

internal data class PanelTransform(val alpha: Float = 1f, val x: Float = 0f, val y: Float = 0f, val scale: Float = 1f)

/** Finite native View animations. No animator runs while a panel is idle or after cleanup. */
object PanelWindowAnimations {
    internal fun hidden(animation: PanelAnimation, edge: Edge) = when (animation) {
        PanelAnimation.NONE -> PanelTransform()
        PanelAnimation.FADE -> PanelTransform(alpha = 0f)
        else -> PanelTransform(alpha = 0f, x = when (edge) { Edge.LEFT -> -.08f; Edge.RIGHT -> .08f; else -> 0f },
            y = if (edge == Edge.BOTTOM) .08f else 0f, scale = if (animation == PanelAnimation.SCALE) .88f else 1f)
    }
    fun enabled(spec: PanelSpec, closing: Boolean = false) = enabled(if (closing) spec.panel.closeAnimation else spec.panel.animation, spec.editing || spec.wheelDragging)
    internal fun enabled(animation: PanelAnimation, editing: Boolean = false) = !editing && animation != PanelAnimation.NONE && ValueAnimator.areAnimatorsEnabled()
    fun cancel(view: View) { view.animate().withEndAction(null).cancel() }
    fun enter(view: View, spec: PanelSpec) = enter(view, spec.panel.animation, spec.edge, spec.geometry.bounds, spec.editing || spec.wheelDragging)
    internal fun enter(view: View, animation: PanelAnimation, edge: Edge, bounds: Bounds, editing: Boolean = false) {
        cancel(view)
        val animate = enabled(animation, editing)
        val from = if (animate) hidden(animation, edge) else PanelTransform()
        view.alpha = from.alpha; view.translationX = from.x * bounds.width; view.translationY = from.y * bounds.height
        view.scaleX = from.scale; view.scaleY = from.scale
        view.pivotX = bounds.width / 2; view.pivotY = bounds.height / 2
        if (animate) view.animate().alpha(1f).translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(180).setInterpolator(PathInterpolator(.4f, 0f, .2f, 1f)).start()
    }
    fun exit(view: View, spec: PanelSpec, finished: () -> Unit) = exit(view, spec.panel.closeAnimation, spec.edge, spec.geometry.bounds, spec.editing || spec.wheelDragging, finished)
    internal fun exit(view: View, animation: PanelAnimation, edge: Edge, bounds: Bounds, editing: Boolean = false, finished: () -> Unit) {
        cancel(view)
        if (!enabled(animation, editing)) { finished(); return }
        val to = hidden(animation, edge)
        view.pivotX = bounds.width / 2; view.pivotY = bounds.height / 2
        view.animate().alpha(to.alpha).translationX(to.x * bounds.width).translationY(to.y * bounds.height)
            .scaleX(to.scale).scaleY(to.scale).setDuration(120).setInterpolator(PathInterpolator(.4f, 0f, .2f, 1f))
            .withEndAction(finished).start()
    }
}
