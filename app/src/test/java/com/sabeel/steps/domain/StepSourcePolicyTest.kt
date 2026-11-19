package com.sabeel.steps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepSourcePolicyTest {

    private val counterOnly = SourceAvailability(counter = true, detector = false, accelerometer = true)
    private val detectorOnly = SourceAvailability(counter = false, detector = true, accelerometer = true)
    private val estimateOnly = SourceAvailability(counter = false, detector = false, accelerometer = true)
    private val nothing = SourceAvailability(counter = false, detector = false, accelerometer = false)

    @Test
    fun counterWinsOverEverything() {
        assertEquals(StepSource.COUNTER, StepSourcePolicy.choose(counterOnly))
        assertEquals(StepSource.COUNTER, StepSourcePolicy.choose(SourceAvailability(true, true, true)))
    }

    @Test
    fun detectorIsTheSecondChoice() {
        assertEquals(StepSource.DETECTOR, StepSourcePolicy.choose(detectorOnly))
    }

    @Test
    fun estimateIsOnlyForPhonesWithNoMeasuredSource() {
        assertEquals(StepSource.MOTION_ESTIMATE, StepSourcePolicy.choose(estimateOnly))
        assertEquals(StepSource.NONE, StepSourcePolicy.choose(nothing))
        assertTrue(StepSourcePolicy.canEstimate(estimateOnly))
        assertFalse(StepSourcePolicy.canEstimate(nothing))
    }

    @Test
    fun nextMeasuredOnlyFallsFromCounterToDetector() {
        assertEquals(StepSource.DETECTOR, StepSourcePolicy.nextMeasured(StepSource.COUNTER, detectorOnly))
        assertNull(StepSourcePolicy.nextMeasured(StepSource.COUNTER, counterOnly))
        assertNull(StepSourcePolicy.nextMeasured(StepSource.DETECTOR, detectorOnly))
    }

    @Test
    fun fallbackTargetNeverRepeatsTheSilentCounter() {
        assertEquals(StepSource.DETECTOR, StepSourcePolicy.fallbackTarget(StepSource.COUNTER, detectorOnly))
        assertEquals(
            StepSource.MOTION_ESTIMATE,
            StepSourcePolicy.fallbackTarget(StepSource.COUNTER, estimateOnly),
        )
        assertEquals(
            StepSource.MOTION_ESTIMATE,
            StepSourcePolicy.fallbackTarget(StepSource.DETECTOR, detectorOnly),
        )
        assertNull(StepSourcePolicy.fallbackTarget(StepSource.DETECTOR, SourceAvailability(false, true, false)))
        assertNull(StepSourcePolicy.fallbackTarget(StepSource.MOTION_ESTIMATE, estimateOnly))
    }

    @Test
    fun measuredFlagOnlyTrustsHardwareSensors() {
        assertTrue(StepSource.COUNTER.measured)
        assertTrue(StepSource.DETECTOR.measured)
        assertFalse(StepSource.MOTION_ESTIMATE.measured)
        assertFalse(StepSource.NONE.measured)
    }
}
