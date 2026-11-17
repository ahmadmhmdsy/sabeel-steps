package com.sabeel.steps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the sensor -> daily aggregate reconciliation. */
class StepPipelineTest {

    private val today = "2026-11-16"
    private val yesterday = "2026-11-15"

    @Test
    fun freshInstallStartsWithZero() {
        // A fresh install must not report the lifetime hardware counter as today's steps.
        val update = StepPipeline.onSensor(StepState.initial(today), raw = 137f, today = today)
        assertEquals(0, update.todaySteps)
        assertEquals(137.0, update.state.baselineRaw.toDouble(), 0.001)
        assertEquals(today, update.state.baselineDate)
        assertTrue(update.state.initialized)
        assertFalse(update.rolledOver)
    }

    @Test
    fun firstRunKeepsRestoredToday() {
        // After a CSV restore the baseline is unknown but today's restored total must survive the
        // first sensor reading instead of being reset to zero.
        val restored = StepState(baselineRaw = 0f, todaySteps = 420, baselineDate = today, initialized = false)
        val update = StepPipeline.onSensor(restored, raw = 900f, today = today)
        assertEquals(420, update.todaySteps)
        assertEquals(900.0, update.state.baselineRaw.toDouble(), 0.001)
        assertTrue(update.state.initialized)
    }

    @Test
    fun monotonicEventsOnlyAddTheDelta() {
        val state = StepState(baselineRaw = 100f, todaySteps = 500, baselineDate = today, initialized = true)
        val first = StepPipeline.onSensor(state, raw = 140f, today = today)
        assertEquals(540, first.todaySteps)
        val second = StepPipeline.onSensor(first.state, raw = 141f, today = today)
        assertEquals(541, second.todaySteps)
        assertEquals(141.0, second.state.baselineRaw.toDouble(), 0.001)
        assertFalse(second.rolledOver)
    }

    @Test
    fun rebootRebasesAndKeepsTodaysSteps() {
        val state = StepState(baselineRaw = 5000f, todaySteps = 3000, baselineDate = today, initialized = true)
        // The device rebooted: the hardware counter is cumulative-since-boot and starts near zero.
        val update = StepPipeline.onSensor(state, raw = 42f, today = today)
        assertEquals(3000, update.todaySteps)
        assertEquals(42.0, update.state.baselineRaw.toDouble(), 0.001)
        assertEquals(today, update.state.baselineDate)
        assertFalse(update.rolledOver)
    }

    @Test
    fun negativeRawNeverProducesNegativeSteps() {
        val update = StepPipeline.onSensor(StepState.initial(today), raw = -3f, today = today)
        assertEquals(0, update.todaySteps)
        assertEquals(-3.0, update.state.baselineRaw.toDouble(), 0.001)
    }

    @Test
    fun nonFiniteRawIsIgnored() {
        val state = StepState(baselineRaw = 250f, todaySteps = 250, baselineDate = today, initialized = true)
        val update = StepPipeline.onSensor(state, raw = Float.NaN, today = today)
        assertEquals(250, update.todaySteps)
        assertEquals(250.0, update.state.baselineRaw.toDouble(), 0.001)
        assertFalse(update.rolledOver)
    }

    @Test
    fun smallBackwardsJitterIsWithinTolerance() {
        // A decrease of at most 1 unit is treated as noise, not as a reboot.
        val state = StepState(baselineRaw = 100f, todaySteps = 500, baselineDate = today, initialized = true)
        val update = StepPipeline.onSensor(state, raw = 99.4f, today = today)
        assertEquals(500, update.todaySteps)
        assertEquals(99.4, update.state.baselineRaw.toDouble(), 0.001)
    }

    @Test
    fun midnightRolloverDropsUnattributedDelta() {
        // Steps between the last sample of yesterday and the first sample of today cannot be
        // attributed to either day, so the spec drops them instead of inflating the new day.
        val state = StepState(baselineRaw = 1200f, todaySteps = 5000, baselineDate = yesterday, initialized = true)
        val update = StepPipeline.onSensor(state, raw = 1310f, today = today)
        assertTrue(update.rolledOver)
        assertEquals(0, update.todaySteps)
        assertEquals(today, update.state.baselineDate)
        assertEquals(1310.0, update.state.baselineRaw.toDouble(), 0.001)
    }

    @Test
    fun backwardClockRolloverResetsTheDay() {
        // Time zone travel or a manual clock change must not keep adding to yesterday's total.
        val state = StepState(baselineRaw = 5000f, todaySteps = 3000, baselineDate = today, initialized = true)
        val update = StepPipeline.onSensor(state, raw = 5100f, today = yesterday)
        assertTrue(update.rolledOver)
        assertEquals(0, update.todaySteps)
        assertEquals(yesterday, update.state.baselineDate)
        assertEquals(5100.0, update.state.baselineRaw.toDouble(), 0.001)
    }

    @Test
    fun processDeathRestoreDoesNotDoubleCount() {
        // The service persisted raw=900 / today=700 before it was killed; the sensor kept counting.
        val persisted = StepState(baselineRaw = 900f, todaySteps = 700, baselineDate = today, initialized = true)
        val update = StepPipeline.onSensor(persisted, raw = 950f, today = today)
        assertEquals(750, update.todaySteps)
    }

    @Test
    fun rebootBetweenRestartsDoesNotDoubleCountYesterday() {
        val persisted = StepState(baselineRaw = 800f, todaySteps = 400, baselineDate = today, initialized = true)
        val update = StepPipeline.onSensor(persisted, raw = 15f, today = today)
        assertEquals(400, update.todaySteps)
        assertEquals(today, update.state.baselineDate)
    }

    @Test
    fun sameRawValueIsStable() {
        val state = StepState(baselineRaw = 250f, todaySteps = 250, baselineDate = today, initialized = true)
        val update = StepPipeline.onSensor(state, raw = 250f, today = today)
        assertEquals(250, update.todaySteps)
        assertFalse(update.rolledOver)
    }

    @Test
    fun resolveTodayStepsTakesTheMaximum() {
        val state = StepState(baselineRaw = 100f, todaySteps = 120, baselineDate = today, initialized = true)
        assertEquals(300, StepPipeline.resolveTodaySteps(state, dbTodaySteps = 300))
        assertEquals(120, StepPipeline.resolveTodaySteps(state, dbTodaySteps = null))
        assertEquals(120, StepPipeline.resolveTodaySteps(state, dbTodaySteps = 40))
        assertEquals(0, StepPipeline.resolveTodaySteps(state.copy(todaySteps = -5), dbTodaySteps = null))
    }
}
