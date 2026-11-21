package com.sabeel.steps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceWatchdogTest {

    private val t0 = 1_000_000L

    @Test
    fun noProbeBeforeTheSilenceThreshold() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 179_999L))
    }

    @Test
    fun probeIsRequestedAfterSilence() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        assertEquals(SilenceWatchdog.Action.ProbeMotion, wd.onTick(t0 + 180_000L))
    }

    @Test
    fun inFlightIsOnlySetWhenTheProbeIsActuallyScheduled() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        wd.onTick(t0 + 180_000L)
        assertFalse(wd.probeInFlight)
        wd.probeScheduled(t0 + 180_000L)
        assertTrue(wd.probeInFlight)
        // A tick while a probe is in flight must not request a second one.
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 181_000L))
    }

    @Test
    fun failedProbeCannotDisableTheWatchdog() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L, probeRetryMs = 30_000L)
        wd.onStart(t0)
        wd.onTick(t0 + 180_000L)
        wd.probeUnavailable(t0 + 180_000L)
        assertFalse(wd.probeInFlight)
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 190_000L))
        assertEquals(SilenceWatchdog.Action.ProbeMotion, wd.onTick(t0 + 210_000L))
    }

    @Test
    fun walkingProbeStartsTheEstimateOnce() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        wd.onTick(t0 + 180_000L)
        wd.probeScheduled(t0 + 180_000L)
        assertEquals(SilenceWatchdog.Action.StartEstimate, wd.onProbeResult(t0 + 188_000L, walking = true))
        assertTrue(wd.estimateActive)
        // No further probes while the estimate is running.
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 600_000L))
    }

    @Test
    fun stillProbeBacksOffExponentially() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        wd.onTick(t0 + 180_000L)
        wd.probeScheduled(t0 + 180_000L)
        wd.onProbeResult(t0 + 188_000L, walking = false)
        // First backoff is 60 s * 2 = 120 s.
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 300_000L))
        assertEquals(SilenceWatchdog.Action.ProbeMotion, wd.onTick(t0 + 308_000L))
    }

    @Test
    fun measuredEventStopsTheEstimateAndResetsSilence() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        wd.onTick(t0 + 180_000L)
        wd.probeScheduled(t0 + 180_000L)
        wd.onProbeResult(t0 + 188_000L, walking = true)
        val action = wd.onMeasuredEvent(t0 + 200_000L)
        assertEquals(SilenceWatchdog.Action.StopEstimate, action)
        assertFalse(wd.estimateActive)
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 210_000L))
    }

    @Test
    fun continuousEstimateNeverProbes() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        wd.startContinuousEstimate(t0)
        assertTrue(wd.estimateActive)
        assertEquals(SilenceWatchdog.Action.None, wd.onTick(t0 + 3_600_000L))
    }

    @Test
    fun probeResultWithoutAScheduledProbeIsIgnored() {
        val wd = SilenceWatchdog(silenceAfterMs = 180_000L)
        wd.onStart(t0)
        assertEquals(SilenceWatchdog.Action.None, wd.onProbeResult(t0 + 200_000L, walking = true))
        assertFalse(wd.estimateActive)
    }
}
