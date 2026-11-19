package com.sabeel.steps.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StepSourceHealthTest {

    private val minute = 60_000L
    private val stall = 10 * minute

    @Test
    fun noActionBeforeTheStallWindow() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall - 1))
    }

    @Test
    fun stallRequestsAMotionProbeAndThenWaitsForTheInterval() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall))
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall + 1_000))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 5_000, walking = false))
        // The still-probe backoff means the next probe is not one minute later.
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall + minute))
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall + 2 * minute + 10_000))
    }

    @Test
    fun oneWalkingProbeIsNotEnough() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 5_000, walking = true))
    }

    @Test
    fun twoWalkingProbesTriggerFallback() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 5_000, walking = true))
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall + 66_000))
        assertEquals(StepSourceHealth.Action.Fallback, health.onProbeResult(stall + 71_000, walking = true))
    }

    @Test
    fun stillProbesBackOffExponentially() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 5_000, walking = false))
        // The first still probe doubles the interval: the next probe is two minutes later.
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall + minute))
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall + 2 * minute + 10_000))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 2 * minute + 15_000, walking = false))
        // And four minutes after that.
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall + 5 * minute))
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall + 7 * minute))
    }

    @Test
    fun nonWalkingProbeResetsTheWalkingStreak() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 5_000, walking = true))
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall + 66_000))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 71_000, walking = false))
        // The streak restarted: one walking probe after the pause is still not enough.
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(stall + 5 * minute))
        assertEquals(StepSourceHealth.Action.None, health.onProbeResult(stall + 5 * minute + 5_000, walking = true))
    }

    @Test
    fun aRealEventResetsTheStallClock() {
        val health = StepSourceHealth(stallAfterMs = stall)
        health.onSourceStart(StepSource.COUNTER, 0L)
        health.onEvent(0L)
        val eventAt = stall - minute
        health.onEvent(eventAt)
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall + 1_000))
        assertEquals(StepSourceHealth.Action.ProbeMotion, health.onTick(eventAt + stall))
    }

    @Test
    fun initialStartIsNotASwitchButATransitionIs() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.COUNTER, 0L)
        assertEquals(0, health.switches)
        health.onSourceStart(StepSource.DETECTOR, 1_000L)
        assertEquals(1, health.switches)
        health.onSourceStart(StepSource.COUNTER, 2_000L)
        assertEquals(2, health.switches)
    }

    @Test
    fun estimateSourceNeverRequestsProbes() {
        val health = StepSourceHealth()
        health.onSourceStart(StepSource.MOTION_ESTIMATE, 0L)
        assertEquals(StepSourceHealth.Action.None, health.onTick(stall * 2))
    }
}
