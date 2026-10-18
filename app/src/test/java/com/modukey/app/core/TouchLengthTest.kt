package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class TouchLengthTest {
    @Test fun `short and long limits cover the safe edges at different densities`() {
        for (density in listOf(1f, 2.5f, 3f)) {
            val insets = Insets(10f * density, 20f * density, 10f * density, 30f * density)
            assertEquals(380f, TriggerGeometry.maximumLengthDp(400f * density, 850f * density, density, insets, false), .001f)
            assertEquals(800f, TriggerGeometry.maximumLengthDp(400f * density, 850f * density, density, insets, true), .001f)
            val saved = TriggerSettings(lengthDp = 800f, longerTouchLength = true, portrait = Placement(Edge.BOTTOM, .5f))
            val bottom = TriggerGeometry.bounds(400f * density, 850f * density, density, insets, saved, false)
            assertEquals(380f * density, bottom.width, .001f)
            assertEquals(800f, saved.normalized().lengthDp)
        }
    }

    @Test fun `long length survives mode and orientation constraints`() {
        val saved = TriggerSettings(lengthDp = 800f, longerTouchLength = true)
        assertEquals(800f, TriggerGeometry.bounds(400f, 850f, 1f, Insets(), saved, false).height)
        assertEquals(400f, TriggerGeometry.bounds(400f, 850f, 1f, Insets(), saved.copy(longerTouchLength = false), false).height)
        assertEquals(400f, TriggerGeometry.bounds(850f, 400f, 1f, Insets(), saved, true).height)
        assertEquals(800f, saved.lengthDp)
    }
}
