package com.sabeel.steps.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the session projection. The global day counter is the only step source; the
 * session is the difference from a baseline, so these tests pin the two invariants that keep the
 * numbers honest: session steps never exceed the day total, and a midnight rollover never counts a
 * step twice or drops a leg.
 */
class SessionProjectionTest {

    private val day1 = "2026-11-22"
    private val day2 = "2026-11-23"
    private val day3 = "2026-11-24"

    private fun tick(state: SessionProjectionState, total: Int, date: String, now: Long = 0L) =
        SessionProjection.update(state = state, dayTotal = total, date = date, nowMillis = now)

    @Test
    fun startCapturesTheBaselineAndWaitsForTheFirstDate() {
        val state = SessionProjection.start(4_321)
        assertEquals(4_321, state.baselineDayTotal)
        assertEquals(4_321, state.lastDayTotal)
        assertEquals("", state.date)
        assertTrue(state.legs.isEmpty())
    }

    @Test
    fun firstTickAdoptsTheDateAndCountsStepsSinceStart() {
        val update = tick(SessionProjection.start(5_000), 5_120, day1)
        assertEquals(day1, update.state.date)
        assertEquals(120, update.sessionSteps)
        assertFalse(update.rolledOver)
        assertNull(update.closedLeg)
    }

    @Test
    fun sameDateTicksCountOnlyTheGlobalDelta() {
        var state = SessionProjection.start(5_000)
        state = tick(state, 5_120, day1).state
        val update = tick(state, 5_300, day1)
        assertEquals(300, update.sessionSteps)
        assertEquals(5_000, update.state.baselineDayTotal)
    }

    @Test
    fun repeatedTickWithTheSameTotalIsIdempotent() {
        val first = tick(SessionProjection.start(100), 250, day1)
        val second = tick(first.state, 250, day1)
        assertEquals(first.sessionSteps, second.sessionSteps)
        assertEquals(first.state.copy(lastUpdateAt = 0L), second.state.copy(lastUpdateAt = 0L))
    }

    @Test
    fun aGlobalTotalBelowTheBaselineNeverProducesNegativeSteps() {
        val update = tick(SessionProjection.start(5_000), 4_000, day1)
        assertEquals(0, update.sessionSteps)
    }

    @Test
    fun projectionNeverAttributesStepsTheDayDoesNotHave() {
        for (total in listOf(0, 1, 500, 12_345)) {
            val update = tick(SessionProjection.start(0), total, day1)
            assertTrue(update.sessionSteps <= total)
            val afterRollover = tick(update.state, 7, day2)
            assertTrue(afterRollover.sessionSteps <= 7)
        }
    }

    @Test
    fun midnightClosesTheLegAndResetsTheBaseline() {
        var state = SessionProjection.start(1_000)
        state = tick(state, 1_100, day1).state
        val rollover = tick(state, 40, day2)
        assertTrue(rollover.rolledOver)
        assertEquals(ProjectedLeg(day1, 100), rollover.closedLeg)
        assertEquals(listOf(ProjectedLeg(day1, 100)), rollover.state.legs)
        assertEquals(40, rollover.state.baselineDayTotal)
        assertEquals(0, rollover.sessionSteps)
        assertEquals(day2, rollover.state.date)
    }

    @Test
    fun midnightWithNoStepsDoesNotCreateAnEmptyLeg() {
        var state = SessionProjection.start(1_000)
        state = tick(state, 1_000, day1).state
        val rollover = tick(state, 0, day2)
        assertTrue(rollover.rolledOver)
        assertNull(rollover.closedLeg)
        assertTrue(rollover.state.legs.isEmpty())
    }

    @Test
    fun twoMidnightsAccumulateLegsWithoutDoubleCounting() {
        var state = SessionProjection.start(0)
        state = tick(state, 100, day1).state // day1: 100 steps
        state = tick(state, 30, day2).state // closes day1 (100), baseline 30
        state = tick(state, 80, day2).state // day2: 50 steps
        state = tick(state, 10, day3).state // closes day2 (50), baseline 10
        assertEquals(listOf(ProjectedLeg(day1, 100), ProjectedLeg(day2, 50)), state.legs)

        val update = tick(state, 25, day3) // day3: 15 steps
        assertNull(update.closedLeg)
        assertEquals(165, SessionProjection.totalSteps(update.state, 25))
    }

    @Test
    fun missingDateDoesNotFabricateARollover() {
        var state = SessionProjection.start(100)
        state = tick(state, 200, day1).state
        val update = tick(state, 260, "")
        assertEquals(day1, update.state.date)
        assertFalse(update.rolledOver)
        assertNull(update.closedLeg)
        assertEquals(160, update.sessionSteps)
    }

    @Test
    fun tickTimeIsCheckpointedButNeverCountsSteps() {
        val update = tick(SessionProjection.start(0), 10, day1, now = 1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, update.state.lastUpdateAt)
        assertEquals(10, update.sessionSteps)
    }

    @Test
    fun closingLegReturnsTheFinalCurrentDayLeg() {
        var state = SessionProjection.start(500)
        state = tick(state, 900, day1).state
        assertEquals(ProjectedLeg(day1, 400), SessionProjection.closingLeg(state, 900))
        assertNull(SessionProjection.closingLeg(SessionProjection.start(500), 500))
    }

    @Test
    fun emptySessionHasNoClosingLegAndNoSteps() {
        val state = SessionProjection.start(1_000)
        val update = tick(state, 1_000, day1)
        assertEquals(0, update.sessionSteps)
        assertNull(SessionProjection.closingLeg(update.state, 1_000))
        assertEquals(0, SessionProjection.totalSteps(update.state, 1_000))
    }
}
