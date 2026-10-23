package com.modukey.app.platform

import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test

class PanelWindowAnimationsTest {
    @Test fun wheelAnimationSwitchAlsoDisablesWindowMotion() {
        val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(animationsEnabled = false))
        val panel = PanelConfig(portrait = layout, animation = PanelAnimation.SCALE, closeAnimation = PanelAnimation.FADE)
        val geometry = PanelGeometry.calculate(Bounds(0f, 0f, 600f, 600f), 1f, layout, 3, Bounds(0f, 200f, 12f, 100f), Edge.LEFT)
        val spec = PanelSpec(panel, layout, geometry, false)
        assertFalse(PanelWindowAnimations.enabled(spec)); assertFalse(PanelWindowAnimations.enabled(spec, closing = true))
    }
    @Test fun `none stays still and fade changes only alpha on every edge`() {
        for (edge in Edge.entries) {
            assertEquals(PanelTransform(), PanelWindowAnimations.hidden(PanelAnimation.NONE, edge))
            assertEquals(PanelTransform(alpha = 0f), PanelWindowAnimations.hidden(PanelAnimation.FADE, edge))
        }
    }
    @Test fun `slide and scale move toward the invoking logical edge on exit`() {
        for (animation in listOf(PanelAnimation.SLIDE, PanelAnimation.SCALE)) {
            val left=PanelWindowAnimations.hidden(animation,Edge.LEFT)
            val right=PanelWindowAnimations.hidden(animation,Edge.RIGHT)
            val bottom=PanelWindowAnimations.hidden(animation,Edge.BOTTOM)
            assertTrue(left.x<0); assertEquals(0f,left.y)
            assertEquals(-left.x,right.x); assertEquals(0f,right.y)
            assertEquals(0f,bottom.x); assertTrue(bottom.y>0)
            for (motion in listOf(left,right,bottom)) {
                assertEquals(0f,motion.alpha)
                assertEquals(if(animation==PanelAnimation.SCALE) .88f else 1f,motion.scale)
            }
        }
    }
}
