package com.sabeel.steps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the manifold state machine. The guiding invariant: independent sources may disagree,
 * but the fused day never falls, never double counts a confirmed step, and the estimated portion is
 * always exactly the unconfirmed remainder.
 */
class StepPipelineTest {

    private val today = "2026-11-16"
    private val yesterday = "2026-11-15"
    private val baseNanos = 1_000_000_000_000L

    private fun counterState(
        raw: Float = 100f,
        counterToday: Int = 0,
        total: Int = counterToday,
        confirmed: Int = counterToday,
        day: String = today,
    ) = StepState(
        day = day,
        total = total,
        confirmed = confirmed,
        counterToday = counterToday,
        counterLastRaw = raw,
        counterLastEventNanos = baseNanos,
        initialized = true,
    )

    // ---- counter ----

    @Test
    fun freshInstallDoesNotTurnTheLifetimeCounterIntoTodaysSteps() {
        val update = StepPipeline.onCounter(StepState.initial(today), raw = 137f, today = today, eventNanos = baseNanos)
        assertEquals(0, update.state.total)
        assertEquals(137.0, update.state.counterLastRaw.toDouble(), 0.001)
        assertEquals(today, update.state.day)
        assertTrue(update.state.initialized)
        assertFalse(update.rolledOver)
    }

    @Test
    fun firstDayOfUseStartsAtZeroButKeepsTheBaseline() {
        val update = StepPipeline.onCounter(StepState.initial(today), raw = 42_000f, today = today)
        assertEquals(0, update.state.total)
        assertEquals(42_000.0, update.state.counterLastRaw.toDouble(), 0.001)
    }

    @Test
    fun restoredTotalSurvivesTheFirstCounterSample() {
        val restored = StepState(day = today, total = 420, confirmed = 420, initialized = false)
        val update = StepPipeline.onCounter(restored, raw = 900f, today = today, eventNanos = baseNanos)
        assertEquals(420, update.state.total)
        assertEquals(900.0, update.state.counterLastRaw.toDouble(), 0.001)
    }

    @Test
    fun monotonicCounterSamplesAddOnlyTheirDelta() {
        var state = counterState(raw = 100f, counterToday = 500, total = 500, confirmed = 500)
        state = StepPipeline.onCounter(state, raw = 140f, today = today, eventNanos = baseNanos + 10_000_000_000L).state
        assertEquals(540, state.total)
        state = StepPipeline.onCounter(state, raw = 141f, today = today, eventNanos = baseNanos + 11_000_000_000L).state
        assertEquals(541, state.total)
    }

    @Test
    fun rebootRebasesWithoutLosingTodaysSteps() {
        val state = counterState(raw = 5000f, counterToday = 3000, total = 3000, confirmed = 3000)
        val update = StepPipeline.onCounter(state, raw = 42f, today = today, eventNanos = 1L)
        assertEquals(3000, update.state.total)
        assertEquals(42.0, update.state.counterLastRaw.toDouble(), 0.001)
        assertEquals(3000, update.state.counterToday)
        assertFalse(update.rolledOver)
    }

    @Test
    fun counterResetMidDayKeepsTheAttributedTotal() {
        val state = counterState(raw = 900f, counterToday = 700, total = 700, confirmed = 700)
        val update = StepPipeline.onCounter(state, raw = 3f, today = today, eventNanos = baseNanos + 1L)
        assertEquals(700, update.state.total)
    }

    @Test
    fun nonFiniteCounterValueIsIgnored() {
        val state = counterState()
        val update = StepPipeline.onCounter(state, raw = Float.NaN, today = today)
        assertEquals(state, update.state)
    }

    @Test
    fun tinyBackwardsJitterDoesNotCountAndUpdatesTheBaseline() {
        val state = counterState(raw = 100f, counterToday = 500, total = 500, confirmed = 500)
        val update = StepPipeline.onCounter(state, raw = 99.4f, today = today, eventNanos = baseNanos + 1L)
        assertEquals(500, update.state.total)
        assertEquals(99.4, update.state.counterLastRaw.toDouble(), 0.001)
    }

    @Test
    fun outOfOrderEventIsIgnoredNotAppliedTwice() {
        val state = counterState(raw = 200f, counterToday = 100, total = 100, confirmed = 100)
            .copy(counterLastEventNanos = baseNanos + 5_000_000_000L)
        val stale = StepPipeline.onCounter(state, raw = 210f, today = today, eventNanos = baseNanos + 1_000_000_000L)
        assertEquals(state, stale.state)
    }

    @Test
    fun catchUpAfterALongSilenceIsCounted() {
        // The process was dead for an hour; the counter aggregate jumped by 500. Never discard the
        // catch-up: this is the exact number the old rebase-based pipeline threw away.
        val state = counterState(raw = 1000f, counterToday = 400, total = 400, confirmed = 400)
        val update = StepPipeline.onCounter(state, raw = 1500f, today = today, eventNanos = baseNanos + 3_600_000_000_000L)
        assertEquals(900, update.state.total)
    }

    @Test
    fun impossibleJumpIsClampedAgainstSensorTime() {
        // 1 s elapsed: 6 steps/s + 5 grace = 11 plausible steps.
        val state = counterState(raw = 100f, counterToday = 0)
        val update = StepPipeline.onCounter(state, raw = 10_100f, today = today, eventNanos = baseNanos + 1_000_000_000L)
        assertEquals(11, update.state.total)
        assertEquals(11, update.state.counterToday)
    }

    @Test
    fun normalFastWalkingIsNotClamped() {
        val state = counterState(raw = 100f, counterToday = 0)
        val update = StepPipeline.onCounter(state, raw = 109f, today = today, eventNanos = baseNanos + 2_000_000_000L)
        assertEquals(9, update.state.total)
    }

    @Test
    fun clampIsSkippedWhenThePreviousTimestampIsUnknown() {
        val state = counterState(raw = 100f, counterToday = 0)
            .copy(counterLastEventNanos = 0L)
        val update = StepPipeline.onCounter(state, raw = 10_100f, today = today, eventNanos = baseNanos)
        assertEquals(10_000, update.state.total)
    }

    // ---- day rollover ----

    @Test
    fun midnightDropsTheUnattributableCounterDelta() {
        val state = counterState(raw = 1200f, counterToday = 5000, total = 5000, confirmed = 5000, day = yesterday)
        val update = StepPipeline.onCounter(state, raw = 1310f, today = today, eventNanos = baseNanos + 1L)
        assertTrue(update.rolledOver)
        assertEquals(0, update.state.total)
        assertEquals(today, update.state.day)
        assertEquals(1310.0, update.state.counterLastRaw.toDouble(), 0.001)
    }

    @Test
    fun backwardClockRolloverResetsTheDay() {
        val state = counterState(raw = 5000f, counterToday = 3000, total = 3000, confirmed = 3000)
        val update = StepPipeline.onCounter(state, raw = 5100f, today = yesterday, eventNanos = baseNanos + 1L)
        assertTrue(update.rolledOver)
        assertEquals(0, update.state.total)
        assertEquals(yesterday, update.state.day)
    }

    @Test
    fun firstEventOnAFreshInstallIsNotARollover() {
        val update = StepPipeline.onDetector(StepState.initial(today), steps = 1, today = today, eventNanos = 1L)
        assertFalse(update.rolledOver)
        assertEquals(today, update.state.day)
    }

    @Test
    fun midnightResetsEveryLedgerIncludingTheEstimateMarker() {
        val state = StepState(
            day = yesterday,
            total = 9000,
            confirmed = 9000,
            counterToday = 8000,
            counterLastRaw = 5000f,
            counterLastEventNanos = baseNanos,
            detectorToday = 8900,
            hcToday = 9000,
            estimateToday = 9000,
            initialized = true,
        )
        val update = StepPipeline.onEstimate(state, steps = 3, today = today)
        assertTrue(update.rolledOver)
        assertEquals(3, update.state.total)
        assertEquals(3, update.state.estimateToday)
        assertEquals(0, update.state.confirmed)
        // Yesterday's confirmed evidence does not carry over, so the new day's 3 steps are an estimate.
        assertEquals(3, update.state.estimated)
    }

    // ---- detector ----

    @Test
    fun detectorAccumulatesEvenWhenTheCounterIsTheDisplayedSource() {
        var state = StepState(day = today, total = 100, confirmed = 100, counterToday = 100,
            counterLastRaw = 100f, counterLastEventNanos = baseNanos, initialized = true)
        state = StepPipeline.onDetector(state, steps = 1, today = today, eventNanos = baseNanos + 100).state
        state = StepPipeline.onDetector(state, steps = 1, today = today, eventNanos = baseNanos + 600_000_000L).state
        assertEquals(2, state.detectorToday)
        // Fused, not summed: the detector has only observed 2 steps while the counter ledger is 100.
        assertEquals(100, state.total)
    }

    @Test
    fun detectorAndCounterAreFusedNotSummed() {
        var state = StepState(day = today, total = 0, initialized = true)
        state = StepPipeline.onCounter(state, raw = 100f, today = today, eventNanos = baseNanos).state
        state = StepPipeline.onCounter(state, raw = 200f, today = today, eventNanos = baseNanos + 20_000_000_000L).state
        state = StepPipeline.onDetector(state, steps = 1, today = today, eventNanos = baseNanos + 21_000_000_000L).state
        assertEquals(100, state.counterToday)
        assertEquals(1, state.detectorToday)
        assertEquals(100, state.total)
    }

    @Test
    fun detectorCatchesUpWhenTheCounterIsStuck() {
        // Both sensors observed the same first 100 steps; then the counter sticks while the detector
        // keeps reporting. The fused total must follow the detector ledger once it passes the counter.
        var state = StepState(day = today, total = 100, confirmed = 100, counterToday = 100,
            detectorToday = 100, counterLastRaw = 100f, counterLastEventNanos = baseNanos,
            initialized = true)
        repeat(50) { i ->
            state = StepPipeline.onDetector(state, steps = 1, today = today, eventNanos = baseNanos + i * 1_000_000_000L).state
        }
        assertEquals(150, state.total)
        assertEquals(150, state.detectorToday)
    }

    @Test
    fun rapidDuplicateDetectorEventsCollapseToOneStep() {
        val state = StepState(day = today, initialized = true)
        val first = StepPipeline.onDetector(state, steps = 5, today = today, eventNanos = 1_000_000_000L)
        assertEquals(5, first.state.total)
        val second = StepPipeline.onDetector(first.state, steps = 5, today = today, eventNanos = 1_050_000_000L)
        assertEquals(6, second.state.total) // at most one step within 100 ms
    }

    @Test
    fun absurdDetectorValueIsCapped() {
        val update = StepPipeline.onDetector(StepState(day = today, initialized = true), steps = 100, today = today, eventNanos = 1L)
        assertEquals(StepPipeline.MAX_DETECTOR_EVENT, update.state.detectorToday)
    }

    @Test
    fun zeroAndNegativeDetectorValuesAreIgnored() {
        val state = StepState(day = today, initialized = true)
        assertEquals(state, StepPipeline.onDetector(state, 0, today).state)
        assertEquals(state, StepPipeline.onDetector(state, -3, today).state)
    }

    // ---- estimate ----

    @Test
    fun estimateStartsFromTheCurrentTotalAndOnlyFillsTheGap() {
        var state = StepState(day = today, total = 500, confirmed = 500, counterToday = 500,
            counterLastRaw = 100f, counterLastEventNanos = baseNanos, initialized = true)
        state = StepPipeline.onEstimate(state, steps = 4, today = today).state
        assertEquals(504, state.total)
        assertEquals(500, state.confirmed)
        assertEquals(4, state.estimated)
        state = StepPipeline.onEstimate(state, steps = 1, today = today).state
        assertEquals(505, state.total)
        assertEquals(5, state.estimated)
    }

    @Test
    fun syncArrivalShrinksTheEstimatedPortionInsteadOfAddingIt() {
        var state = StepState(day = today, total = 0, initialized = true)
        repeat(500) { state = StepPipeline.onEstimate(state, steps = 10, today = today).state }
        assertEquals(5000, state.total)
        assertEquals(5000, state.estimated)
        state = StepPipeline.onExternalTotal(state, totalSteps = 6000, today = today).state
        assertEquals(6000, state.total)
        assertEquals(6000, state.confirmed)
        assertEquals(0, state.estimated)
        assertEquals(0, state.hcToday - 6000)
    }

    @Test
    fun estimateIsNotDoubleCountedWhenTheCounterCatchesUp() {
        var state = StepState(day = today, total = 0, initialized = true)
        repeat(30) { state = StepPipeline.onEstimate(state, steps = 10, today = today).state }
        assertEquals(300, state.total)
        // Counter reports 320 for the same stretch; the max keeps 320, not 620.
        state = StepPipeline.onCounter(state, raw = 100f, today = today, eventNanos = baseNanos).state
        state = StepPipeline.onCounter(state, raw = 420f, today = today, eventNanos = baseNanos + 60_000_000_000L).state
        assertEquals(320, state.counterToday)
        assertEquals(320, state.total)
        assertEquals(0, state.estimated)
    }

    // ---- Health Connect ----

    @Test
    fun externalTotalIsAFloorNeverAnAdditiveDelta() {
        val state = StepState(day = today, total = 1000, confirmed = 1000, initialized = true)
        val first = StepPipeline.onExternalTotal(state, totalSteps = 3000, today = today)
        assertEquals(3000, first.state.total)
        assertEquals(3000, first.state.hcToday)
        val repeated = StepPipeline.onExternalTotal(first.state, totalSteps = 2800, today = today)
        assertEquals(3000, repeated.state.total)
        val higher = StepPipeline.onExternalTotal(repeated.state, totalSteps = 3400, today = today)
        assertEquals(3400, higher.state.total)
        assertEquals(3400, higher.state.confirmed)
    }

    @Test
    fun externalTotalStartsANewDayFromTheProviderTotal() {
        val yesterdayState = StepState(day = yesterday, total = 9000, confirmed = 9000, initialized = true)
        val update = StepPipeline.onExternalTotal(yesterdayState, totalSteps = 250, today = today)
        assertTrue(update.rolledOver)
        assertEquals(250, update.state.total)
        assertEquals(250, update.state.hcToday)
        assertEquals(today, update.state.day)
    }

    @Test
    fun negativeExternalTotalIsIgnored() {
        val state = StepState(day = today, total = 700, confirmed = 700, initialized = true)
        assertEquals(state, StepPipeline.onExternalTotal(state, -1, today).state)
    }

    // ---- reconciliation / resolution ----

    @Test
    fun reconcileRollsTheDayWithoutCreatingSteps() {
        val state = StepState(day = yesterday, total = 500, confirmed = 500, initialized = true)
        val update = StepPipeline.reconcile(state, today)
        assertTrue(update.rolledOver)
        assertEquals(0, update.state.total)
        assertEquals(today, update.state.day)
    }

    @Test
    fun resolveTodayStepsTakesTheMaximum() {
        val state = StepState(day = today, total = 120, confirmed = 120, initialized = true)
        assertEquals(300, StepPipeline.resolveTodaySteps(state, 300))
        assertEquals(120, StepPipeline.resolveTodaySteps(state, null))
        assertEquals(120, StepPipeline.resolveTodaySteps(state, 40))
        assertEquals(0, StepPipeline.resolveTodaySteps(state.copy(total = -5), null))
    }

    @Test
    fun restoredRowAheadOfStateKeepsItsOwnMarker() {
        assertEquals(80, StepPipeline.resolveEstimatedSteps(stateEstimated = 0, existingEstimated = 80, rowIsAhead = true))
    }

    @Test
    fun legacyMigrationTreatsTheOldTotalAsConfirmed() {
        val state = StepPipeline.migrateLegacyState(
            today = today,
            legacyTotal = 420,
            legacyBaselineRaw = 900f,
            legacyBaselineDate = today,
            legacyInitialized = true,
        )
        assertEquals(today, state.day)
        assertEquals(420, state.total)
        assertEquals(420, state.confirmed)
        assertEquals(420, state.counterToday)
        assertEquals(900.0, state.counterLastRaw.toDouble(), 0.001)
        assertEquals(0, state.estimated)
    }

    @Test
    fun legacyMigrationWithoutACounterBaselineKeepsTheTotalConfirmed() {
        // The old estimate source wrote a total but no finite counter baseline.
        val state = StepPipeline.migrateLegacyState(
            today = today,
            legacyTotal = 1500,
            legacyBaselineRaw = Float.NaN,
            legacyBaselineDate = today,
            legacyInitialized = true,
        )
        assertEquals(1500, state.total)
        assertEquals(1500, state.confirmed)
        assertEquals(0, state.counterToday)
        assertTrue(state.counterLastRaw.isNaN())
    }

    @Test
    fun legacyMigrationOfAnEmptyStoreIsAFreshDay() {
        val state = StepPipeline.migrateLegacyState(
            today = today,
            legacyTotal = 0,
            legacyBaselineRaw = 0f,
            legacyBaselineDate = "",
            legacyInitialized = false,
        )
        assertEquals(StepState.initial(today), state)
    }

    @Test
    fun legacyMigrationKeepsTheOldDaySoTheNextSampleRollsOver() {
        val state = StepPipeline.migrateLegacyState(
            today = today,
            legacyTotal = 5000,
            legacyBaselineRaw = 8000f,
            legacyBaselineDate = yesterday,
            legacyInitialized = true,
        )
        assertEquals(yesterday, state.day)
        val rolled = StepPipeline.onCounter(state, raw = 8100f, today = today, eventNanos = baseNanos)
        assertTrue(rolled.rolledOver)
        assertEquals(0, rolled.state.total)
    }

    @Test
    fun legacyUpgradePreservesTheRowsMarkerUntilTheTotalMoves() {
        // v1.1 upgrade: equal totals and a row marker but no marker in the migrated state.
        assertEquals(50, StepPipeline.preserveLegacyMarker(0, 300, existingSteps = 300, existingMarker = 50))
        // New measured evidence moved the total: the normal derived rule wins and the marker shrinks.
        assertEquals(0, StepPipeline.preserveLegacyMarker(0, 340, existingSteps = 300, existingMarker = 50))
        // The state already knows its own marker: never look at the row.
        assertEquals(12, StepPipeline.preserveLegacyMarker(12, 300, existingSteps = 300, existingMarker = 50))
        // Nothing persisted yet.
        assertEquals(0, StepPipeline.preserveLegacyMarker(0, 100, existingSteps = null, existingMarker = null))
        // Corrupt marker is clamped to the row total.
        assertEquals(80, StepPipeline.preserveLegacyMarker(0, 80, existingSteps = 80, existingMarker = 200))
    }

    @Test
    fun markerShrinksWhenTheStateIsAheadOfTheRow() {
        assertEquals(0, StepPipeline.resolveEstimatedSteps(stateEstimated = 0, existingEstimated = 80, rowIsAhead = false))
    }

    @Test
    fun processDeathRestoreCountsTheCatchUp() {
        // The service persisted raw=900 / total=700 before it was killed; the counter kept counting.
        val persisted = StepState(day = today, total = 700, confirmed = 700, counterToday = 700,
            counterLastRaw = 900f, counterLastEventNanos = baseNanos, initialized = true)
        val update = StepPipeline.onCounter(persisted, raw = 950f, today = today, eventNanos = baseNanos + 60_000_000_000L)
        assertEquals(750, update.state.total)
    }

    @Test
    fun rebootBetweenRestartsKeepsTodaysSteps() {
        val persisted = StepState(day = today, total = 400, confirmed = 400, counterToday = 400,
            counterLastRaw = 800f, counterLastEventNanos = baseNanos, initialized = true)
        val update = StepPipeline.onCounter(persisted, raw = 15f, today = today, eventNanos = 1L)
        assertEquals(400, update.state.total)
        assertEquals(15.0, update.state.counterLastRaw.toDouble(), 0.001)
    }

    @Test
    fun dayRolloverThenCounterCatchUpDoesNotAttributeYesterday() {
        // Counter was last seen yesterday; the first sample today is a new baseline, never a delta.
        val state = StepState(day = yesterday, total = 5000, confirmed = 5000, counterToday = 5000,
            counterLastRaw = 8000f, counterLastEventNanos = baseNanos, initialized = true)
        val update = StepPipeline.onCounter(state, raw = 8100f, today = today, eventNanos = baseNanos + 86_400_000_000_000L)
        assertTrue(update.rolledOver)
        assertEquals(0, update.state.total)
        assertEquals(0, update.state.counterToday)
    }
}