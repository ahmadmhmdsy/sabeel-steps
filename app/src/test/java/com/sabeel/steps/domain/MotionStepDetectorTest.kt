package com.sabeel.steps.domain

import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic accelerometer magnitudes -> deterministic step counts. */
class MotionStepDetectorTest {

    private fun simulate(
        hz: Double,
        seconds: Double,
        magnitude: (Double) -> Float,
    ): Pair<Int, MotionStepDetector> {
        val detector = MotionStepDetector()
        var steps = 0
        val samples = (seconds * hz).toInt()
        for (i in 0 until samples) {
            val t = i / hz
            steps += detector.onSample(magnitude(t), (t * 1000.0).toLong())
        }
        return steps to detector
    }

    private fun walking(hz: Double = 1.8): (Double) -> Float = { t ->
        9.81f + 2.5f * sin(2 * Math.PI * hz * t).toFloat() +
            0.8f * sin(4 * Math.PI * hz * t).toFloat() +
            0.05f * sin(2 * Math.PI * 7.3 * t).toFloat()
    }

    private fun stationary(): (Double) -> Float = { t ->
        9.81f + 0.05f * sin(2 * Math.PI * 3.1 * t).toFloat() +
            0.03f * sin(2 * Math.PI * 11.7 * t).toFloat()
    }

    private fun typing(): (Double) -> Float = { t ->
        val enabled = sin(2 * Math.PI * 0.3 * t) > 0
        9.81f + if (enabled) 0.3f * sin(2 * Math.PI * 3.0 * t).toFloat() else 0f
    }

    @Test
    fun cleanWalkingAt50HzCountsEveryStep() {
        val (steps, detector) = simulate(50.0, 120.0, walking())
        assertTrue("expected ~216, got $steps", abs(steps - 216) <= 3)
        assertTrue(detector.isWalking)
    }

    @Test
    fun cleanWalkingAtLowSamplingRateStillWorks() {
        val (steps, detector) = simulate(16.6, 90.0, walking(1.2))
        assertTrue("expected ~108, got $steps", abs(steps - 108) <= 3)
        assertTrue(detector.isWalking)
    }

    @Test
    fun stationaryPhoneCountsNothing() {
        val (steps, detector) = simulate(50.0, 60.0, stationary())
        assertEquals(0, steps)
        assertFalse(detector.isWalking)
    }

    @Test
    fun typingCountsNothing() {
        val (steps, _) = simulate(50.0, 60.0, typing())
        assertEquals(0, steps)
    }

    @Test
    fun firstBurstFlushesShortWalks() {
        val (steps, detector) = simulate(50.0, 3.0, walking())
        assertTrue("expected at least the first burst, got $steps", steps >= 3)
        assertTrue(detector.isWalking)
    }

    @Test
    fun isolatedJoltsDoNotCount() {
        // Two peaks, one second apart: below the three-peak burst requirement.
        var stepped = 0
        val detector = MotionStepDetector()
        fun walkFor(start: Double, duration: Double) {
            val samples = (duration * 50).toInt()
            for (i in 0 until samples) {
                val t = start + i / 50.0
                stepped += detector.onSample(walking()(t), (t * 1000).toLong())
            }
        }
        walkFor(0.0, 1.2)
        walkFor(2.7, 1.2)
        assertEquals(0, stepped)
    }

    @Test
    fun resetClearsWalkingAndRhythm() {
        val (steps, detector) = simulate(50.0, 10.0, walking())
        assertTrue(steps > 0)
        assertTrue(detector.isWalking)
        detector.reset()
        assertFalse(detector.isWalking)
        assertEquals(0, detector.onSample(9.81f, 20_000L))
    }

    @Test
    fun nonFiniteSamplesAreIgnored() {
        val detector = MotionStepDetector()
        assertEquals(0, detector.onSample(Float.NaN, 100L))
        assertEquals(0, detector.onSample(Float.POSITIVE_INFINITY, 200L))
    }
}
