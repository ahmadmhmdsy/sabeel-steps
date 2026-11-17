package com.sabeel.steps.domain

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Pure, unit-tested guidance for the daily goal.
 *
 * The rule is deliberately narrow:
 *  - only *recorded* days count as evidence (a missing row means "not measured", never zero steps);
 *  - the median - not the mean - describes a routine, so one long hike cannot move the goal;
 *  - nothing changes on its own: the caller shows the median and the proposed goal, and the user
 *    either confirms it or keeps the current goal;
 *  - a single suggestion moves the goal by at most [MAX_WEEKLY_CHANGE_FRACTION], so trying a new
 *    goal is a small adjustment rather than a new target.
 */
object GoalAdvisor {

    const val MIN_GOAL = 1_000
    const val MAX_GOAL = 60_000

    /** Recorded days needed before a suggestion is made at all (roughly two weeks). */
    const val MIN_RECORDED_DAYS = 14

    /** The suggestion looks at the last four weeks. */
    const val WINDOW_DAYS = 28

    /** A suggestion never changes the goal by more than this fraction in one step. */
    const val MAX_WEEKLY_CHANGE_FRACTION = 0.10

    /** Below this change the suggestion is noise rather than guidance. */
    const val MIN_CHANGE_STEPS = 200

    data class Advice(
        val recordedDays: Int,
        val medianSteps: Int?,
        val suggestion: Suggestion?,
    )

    data class Suggestion(
        val medianSteps: Int,
        val proposedGoal: Int,
        val recordedDays: Int,
    )

    /** Median of [values], or null when there is nothing to measure. */
    fun median(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            ((sorted[middle - 1].toLong() + sorted[middle]) / 2L).toInt()
        }
    }

    /**
     * Advice for [currentGoal] from the stored [days]. [today] is injected so the rule is testable.
     * Days outside the window, zero-step days and duplicate dates are ignored.
     */
    fun advise(days: List<DailyStep>, currentGoal: Int, today: LocalDate = LocalDate.now()): Advice {
        val goal = currentGoal.coerceIn(MIN_GOAL, MAX_GOAL)
        val from = today.minusDays((WINDOW_DAYS - 1).toLong()).toString()
        val to = today.toString()
        val recorded = days
            .asSequence()
            .filter { it.steps > 0 && it.date >= from && it.date <= to }
            .distinctBy { it.date }
            .toList()
        val medianSteps = median(recorded.map { it.steps })
        val suggestion = if (recorded.size >= MIN_RECORDED_DAYS && medianSteps != null) {
            propose(goal, medianSteps, recorded.size)
        } else {
            null
        }
        return Advice(recordedDays = recorded.size, medianSteps = medianSteps, suggestion = suggestion)
    }

    private fun propose(currentGoal: Int, medianSteps: Int, recordedDays: Int): Suggestion? {
        val comfortable = roundToNearestHundred(medianSteps)
        val lower = (currentGoal * (1.0 - MAX_WEEKLY_CHANGE_FRACTION)).roundToInt()
            .coerceIn(MIN_GOAL, MAX_GOAL)
        val upper = (currentGoal * (1.0 + MAX_WEEKLY_CHANGE_FRACTION)).roundToInt()
            .coerceIn(MIN_GOAL, MAX_GOAL)
        val proposed = roundToNearestHundred(comfortable.coerceIn(lower, upper))
            .coerceIn(MIN_GOAL, MAX_GOAL)
        if (kotlin.math.abs(proposed - currentGoal) < MIN_CHANGE_STEPS) return null
        return Suggestion(medianSteps = medianSteps, proposedGoal = proposed, recordedDays = recordedDays)
    }

    private fun roundToNearestHundred(value: Int): Int =
        ((value.coerceIn(0, MAX_GOAL) + 50) / 100) * 100
}
