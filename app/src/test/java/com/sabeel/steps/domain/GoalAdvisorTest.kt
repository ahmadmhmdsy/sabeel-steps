package com.sabeel.steps.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-JVM tests for the goal suggestion rule. */
class GoalAdvisorTest {

    private val today = LocalDate.of(2026, 11, 18)

    private fun daysBefore(count: Int, end: LocalDate = today, stepsFor: (Int) -> Int): List<DailyStep> =
        (0 until count).map { offset ->
            DailyStep(date = end.minusDays(offset.toLong()).toString(), steps = stepsFor(offset))
        }

    @Test
    fun medianHandlesOddEvenAndEmptyInput() {
        assertEquals(200, GoalAdvisor.median(listOf(300, 100, 200)))
        assertEquals(250, GoalAdvisor.median(listOf(400, 100, 200, 300)))
        assertNull(GoalAdvisor.median(emptyList()))
    }

    @Test
    fun fewerThanTwoWeeksIsNotEnoughToSuggest() {
        val advice = GoalAdvisor.advise(daysBefore(13) { 7_400 }, currentGoal = 8_000, today = today)
        assertEquals(13, advice.recordedDays)
        assertEquals(7_400, advice.medianSteps)
        assertNull(advice.suggestion)
    }

    @Test
    fun zeroStepDaysAreNotEvidence() {
        // 30 calendar days in the window, but only 10 of them were actually recorded.
        val sparse = (0 until 30).map { offset ->
            DailyStep(date = today.minusDays(offset.toLong()).toString(), steps = if (offset % 3 == 0) 9_000 else 0)
        }
        val advice = GoalAdvisor.advise(sparse, currentGoal = 8_000, today = today)
        assertEquals(10, advice.recordedDays)
        assertNull(advice.suggestion)
    }

    @Test
    fun suggestionUsesTheMedianRoundedToHundreds() {
        // 7 x 7400 and 7 x 7480 -> median 7440 -> 7400, a 600-step change from 8000.
        val days = daysBefore(14) { offset -> if (offset < 7) 7_400 else 7_480 }
        val suggestion = GoalAdvisor.advise(days, currentGoal = 8_000, today = today).suggestion
        requireNotNull(suggestion)
        assertEquals(7_440, suggestion.medianSteps)
        assertEquals(7_400, suggestion.proposedGoal)
        assertEquals(14, suggestion.recordedDays)
    }

    @Test
    fun suggestionIsCappedAtTenPercentInEitherDirection() {
        val high = GoalAdvisor.advise(daysBefore(14) { 20_000 }, currentGoal = 8_000, today = today).suggestion
        assertEquals(8_800, high?.proposedGoal)

        val low = GoalAdvisor.advise(daysBefore(14) { 2_000 }, currentGoal = 8_000, today = today).suggestion
        assertEquals(7_200, low?.proposedGoal)
    }

    @Test
    fun noSuggestionWhenTheRoutineAlreadyMatchesTheGoal() {
        val advice = GoalAdvisor.advise(daysBefore(14) { 8_000 }, currentGoal = 8_000, today = today)
        assertEquals(14, advice.recordedDays)
        assertEquals(8_000, advice.medianSteps)
        assertNull(advice.suggestion)
    }

    @Test
    fun noSuggestionWhenTheChangeIsOnlyNoise() {
        // median 8050 -> 8100, just 100 steps from the current goal: not worth a dialog.
        val days = daysBefore(14) { offset -> if (offset < 7) 8_000 else 8_100 }
        val advice = GoalAdvisor.advise(days, currentGoal = 8_000, today = today)
        assertEquals(8_050, advice.medianSteps)
        assertNull(advice.suggestion)
    }

    @Test
    fun daysOutsideTheFourWeekWindowAreIgnored() {
        val recent = daysBefore(14) { 7_000 }
        val stale = daysBefore(10, end = today.minusDays(40), stepsFor = { 20_000 })
        val advice = GoalAdvisor.advise(recent + stale, currentGoal = 8_000, today = today)
        assertEquals(14, advice.recordedDays)
        assertEquals(7_000, advice.medianSteps)
    }

    @Test
    fun duplicateDatesAreCountedOnce() {
        val day = today.minusDays(1).toString()
        val duplicated = List(14) { DailyStep(date = day, steps = 5_000) }
        val advice = GoalAdvisor.advise(duplicated, currentGoal = 8_000, today = today)
        assertEquals(1, advice.recordedDays)
        assertNull(advice.suggestion)
    }
}
