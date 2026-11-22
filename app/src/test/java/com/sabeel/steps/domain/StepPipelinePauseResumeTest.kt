package com.sabeel.steps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pause/resume contract (council A3/A4): a deliberate stop freezes today's totals, drops the
 * evidence baselines, subtracts a same-day Health Connect baseline on resume, and never attributes
 * steps taken while counting was off.
 */
class StepPipelinePauseResumeTest {

    private val today = "2026-11-22"
    private val tomorrow = "2026-11-23"

    /** Builds a state with 100 fused counter steps on today. */
    private fun counterStateSteps100(day: String = today): StepState {
        val baseline = StepPipeline.onCounter(StepState.initial(day), raw = 1_000f, today = day).state
        return StepPipeline.onCounter(
            baseline,
            raw = 1_100f,
            today = day,
            eventNanos = 1_000_000_000L,
        ).state
    }

    @Test
    fun pauseKeepsTotalsAndDropsCounterBaseline() {
        val before = counterStateSteps100()
        assertEquals(100, before.total)

        val paused = StepPipeline.pause(before, nowMillis = 5_000L)

        assertEquals(before.total, paused.total)
        assertEquals(before.confirmed, paused.confirmed)
        assertEquals(before.day, paused.day)
        assertTrue(paused.counterLastRaw.isNaN())
        assertEquals(0L, paused.counterLastEventNanos)
        assertEquals(0L, paused.detectorLastEventNanos)
        assertTrue(paused.hcSuppressedForDay)
        assertEquals(5_000L, paused.updatedAt)
    }

    @Test
    fun firstSampleAfterNaNResumeOnlyRebases() {
        val paused = StepPipeline.pause(counterStateSteps100())
        val resumed = StepPipeline.resume(paused, raw = Float.NaN, today = today, hcReading = null)
        assertTrue(resumed.counterLastRaw.isNaN())

        // The counter kept running through the pause: the absolute value jumped to 9_000.
        val rebase = StepPipeline.onCounter(
            resumed,
            raw = 9_000f,
            today = today,
            eventNanos = 10_000_000_000L,
        )
        assertEquals(100, rebase.state.total)
        assertEquals(9_000f, rebase.state.counterLastRaw)

        // Only steps measured after the rebase count.
        val after = StepPipeline.onCounter(
            rebase.state,
            raw = 9_005f,
            today = today,
            eventNanos = 11_000_000_000L,
        )
        assertEquals(105, after.state.total)
    }

    @Test
    fun resumeWithLiveCounterBaselineKeepsNextDelta() {
        val paused = StepPipeline.pause(counterStateSteps100())
        // The fast off->on path: the service is still alive and passes its last sample as baseline.
        val resumed = StepPipeline.resume(paused, raw = 1_100f, today = today, hcReading = null)
        assertEquals(1_100f, resumed.counterLastRaw)

        val next = StepPipeline.onCounter(
            resumed,
            raw = 1_110f,
            today = today,
            eventNanos = 20_000_000_000L,
        )
        assertEquals(110, next.state.total)
    }

    @Test
    fun resumeSubtractsPausedHealthConnectSteps() {
        val paused = StepPipeline.pause(counterStateSteps100())
        // 8_000 provider steps at resume: the paused gap is inside that number and must be discarded.
        val resumed = StepPipeline.resume(paused, raw = Float.NaN, today = today, hcReading = 8_000)
        assertEquals(8_000, resumed.hcOffsetToday)
        assertFalse(resumed.hcSuppressedForDay)
        assertEquals(0, resumed.hcToday)

        val floor = StepPipeline.onExternalTotal(resumed, totalSteps = 8_500, today = today)
        assertEquals(500, floor.state.hcToday)
        assertEquals(500, floor.state.total)

        // A provider hiccup that reports less must never lower the day.
        val lower = StepPipeline.onExternalTotal(floor.state, totalSteps = 8_400, today = today)
        assertEquals(500, lower.state.hcToday)
        assertEquals(500, lower.state.total)
    }

    @Test
    fun resumeWithoutHealthConnectSuppressesFloorUntilMidnight() {
        val paused = StepPipeline.pause(counterStateSteps100())
        val resumed = StepPipeline.resume(paused, raw = Float.NaN, today = today, hcReading = null)
        assertTrue(resumed.hcSuppressedForDay)
        assertEquals(0, resumed.hcOffsetToday)

        val ignored = StepPipeline.onExternalTotal(resumed, totalSteps = 9_999, today = today)
        assertEquals(100, ignored.state.total)
        assertEquals(0, ignored.state.hcToday)

        // The next local day has no paused gap inside it, so a fresh provider total is a real floor.
        val nextDay = StepPipeline.onExternalTotal(ignored.state, totalSteps = 500, today = tomorrow)
        assertTrue(nextDay.rolledOver)
        assertFalse(nextDay.state.hcSuppressedForDay)
        assertEquals(500, nextDay.state.total)
        assertEquals(500, nextDay.state.hcToday)
    }

    @Test
    fun resumeAfterMidnightStartsZeroDay() {
        val pausedYesterday = StepPipeline.pause(
            StepState.initial(today).copy(total = 4_000, confirmed = 4_000),
        )
        val resumed = StepPipeline.resume(
            pausedYesterday,
            raw = Float.NaN,
            today = tomorrow,
            hcReading = 200,
        )
        assertEquals(tomorrow, resumed.day)
        assertEquals(0, resumed.total)
        assertEquals(0, resumed.confirmed)
        assertEquals(200, resumed.hcOffsetToday)
        assertFalse(resumed.hcSuppressedForDay)
    }

    @Test
    fun pausePreservesDetectorAndEstimateLedgers() {
        val state = StepState(
            day = today,
            total = 500,
            confirmed = 40,
            detectorToday = 40,
            estimateToday = 500,
            initialized = true,
        )
        val paused = StepPipeline.pause(state)
        assertEquals(40, paused.detectorToday)
        assertEquals(500, paused.estimateToday)

        val resumed = StepPipeline.resume(paused, raw = Float.NaN, today = today, hcReading = null)
        assertEquals(40, resumed.detectorToday)
        assertEquals(500, resumed.estimateToday)
        assertEquals(500, resumed.total)
    }

    @Test
    fun pauseNeverLowersAnAlreadyPersistedDay() {
        val state = StepState(
            day = today,
            total = 3_000,
            confirmed = 3_000,
            counterToday = 3_000,
            counterLastRaw = 12_000f,
            counterLastEventNanos = 4_000_000_000L,
            initialized = true,
        )
        val paused = StepPipeline.pause(state)
        assertEquals(3_000, paused.total)
        assertEquals(3_000, paused.confirmed)
        assertEquals(3_000, paused.counterToday)
    }
}
