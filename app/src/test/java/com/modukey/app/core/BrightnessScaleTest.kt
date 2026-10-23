package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class BrightnessScaleTest {
    @Test fun nativeZeroMinimumIsValidButNegativeAndRepeatedSamplesAreRejected() {
        val scale = BrightnessScale(listOf(0, 16, 64, 144, 255))
        assertEquals(0, scale.level(0f)); assertEquals(0f, scale.fraction(0), .0001f)
        for (level in 0..255) assertEquals(level, scale.level(scale.fraction(level)))
        assertFalse(BrightnessScale.validSamples(listOf(-1, 16, 64, 144, 255)))
        assertFalse(BrightnessScale.validSamples(listOf(0, 0, 64, 144, 255)))
    }
    @Test fun perceptualMidpointDoesNotWriteHalfTheRawRange() {
        val scale = BrightnessScale()
        assertEquals(1, scale.level(0f)); assertEquals(255, scale.level(1f))
        assertEquals(22, scale.level(.5f))
        assertEquals(.5f, scale.fraction(22), .01f)
        for (level in 1..255) assertEquals(level, scale.level(scale.fraction(level)))
    }
    @Test fun calibrationMatchesAnchorsAndIsMonotonicInBothDirections() {
        val values = listOf(2, 20, 70, 150, 255)
        val scale = BrightnessScale(values)
        values.forEachIndexed { index, value ->
            assertEquals(value, scale.level(index / 4f)); assertEquals(index / 4f, scale.fraction(value), .0001f)
        }
        assertTrue((0..1000).map { scale.level(it / 1000f) }.zipWithNext().all { (a, b) -> a <= b })
        assertEquals(2, scale.level(-1f)); assertEquals(255, scale.level(2f))
        assertFalse(BrightnessScale.validSamples(listOf(1, 20, 20, 80, 255)))
        assertFalse(BrightnessScale.validSamples(listOf(1, 20, 80)))
    }
}
