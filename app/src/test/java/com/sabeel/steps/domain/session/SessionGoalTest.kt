package com.sabeel.steps.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the custom-session goal evaluator. The service ticks goals every second and the
 * UI shows the same numbers, so evaluation must be deterministic, unit-explicit and inclusive at the
 * boundary.
 */
class SessionGoalTest {

    private fun evaluate(
        type: GoalType,
        target: Double,
        seconds: Long = 0L,
        steps: Int = 0,
        meters: Double = 0.0,
        atPlace: Boolean = false,
    ) = SessionGoal.evaluate(
        type = type,
        target = target,
        elapsedSec = seconds,
        steps = steps,
        distanceMeters = meters,
        atPlace = atPlace,
    )

    @Test
    fun noneGoalIsNeverReachedAndHasNoLabel() {
        val progress = evaluate(GoalType.NONE, 0.0, seconds = 3_600, steps = 10_000, meters = 8_000.0, atPlace = true)
        assertFalse(progress.reached)
        assertEquals("", progress.label)
        assertEquals(0.0, progress.current, 0.0)
        assertEquals(0.0, progress.target, 0.0)
    }

    @Test
    fun timeGoalUsesElapsedSecondsAndIsInclusiveAtTheBoundary() {
        assertFalse(evaluate(GoalType.TIME, 600.0, seconds = 599).reached)
        assertTrue(evaluate(GoalType.TIME, 600.0, seconds = 600).reached)
        assertTrue(evaluate(GoalType.TIME, 600.0, seconds = 601).reached)
        assertEquals(599.0, evaluate(GoalType.TIME, 600.0, seconds = 599).current, 0.0)
        assertEquals("TIME", evaluate(GoalType.TIME, 600.0, seconds = 599).label)
    }

    @Test
    fun stepsGoalUsesSessionStepsAndIsInclusiveAtTheBoundary() {
        assertFalse(evaluate(GoalType.STEPS, 1_000.0, steps = 999).reached)
        assertTrue(evaluate(GoalType.STEPS, 1_000.0, steps = 1_000).reached)
        assertTrue(evaluate(GoalType.STEPS, 1_000.0, steps = 1_001).reached)
        assertEquals(999.0, evaluate(GoalType.STEPS, 1_000.0, steps = 999).current, 0.0)
        assertEquals("STEPS", evaluate(GoalType.STEPS, 1_000.0).label)
    }

    @Test
    fun distanceGoalReachedAtBoundaryAndToleratesFloatingPointNoise() {
        assertFalse(evaluate(GoalType.DISTANCE, 1_000.0, meters = 999.5).reached)
        assertTrue(evaluate(GoalType.DISTANCE, 1_000.0, meters = 1_000.0).reached)
        // A GPS/haversine sum can land a hair under the target; the documented epsilon absorbs that.
        assertTrue(evaluate(GoalType.DISTANCE, 1_000.0, meters = 1_000.0 - 1e-10).reached)
        assertEquals("DISTANCE", evaluate(GoalType.DISTANCE, 1_000.0).label)
    }

    @Test
    fun placeGoalIsBinaryArrivalAndIgnoresTheNumericTarget() {
        val outside = evaluate(GoalType.PLACE, target = 250.0, atPlace = false)
        assertFalse(outside.reached)
        assertEquals(0.0, outside.current, 0.0)
        assertEquals(1.0, outside.target, 0.0)
        assertEquals("PLACE", outside.label)

        val inside = evaluate(GoalType.PLACE, target = 0.0, atPlace = true)
        assertTrue(inside.reached)
        assertEquals(1.0, inside.current, 0.0)
        assertEquals(1.0, inside.target, 0.0)
    }

    @Test
    fun nonPositiveOrNonFiniteTargetsAreUnreachable() {
        assertFalse(evaluate(GoalType.TIME, -1.0, seconds = 10_000).reached)
        assertFalse(evaluate(GoalType.STEPS, 0.0, steps = 100_000).reached)
        assertFalse(evaluate(GoalType.DISTANCE, Double.NaN, meters = 100_000.0).reached)
        assertFalse(evaluate(GoalType.DISTANCE, Double.POSITIVE_INFINITY, meters = 100_000.0).reached)
        assertEquals(0.0, evaluate(GoalType.TIME, -1.0, seconds = 10).target, 0.0)
    }

    @Test
    fun negativeMeasurementsAreClampedAndNeverReached() {
        val time = evaluate(GoalType.TIME, 60.0, seconds = -5)
        assertEquals(0.0, time.current, 0.0)
        assertFalse(time.reached)

        val steps = evaluate(GoalType.STEPS, 100.0, steps = -20)
        assertEquals(0.0, steps.current, 0.0)
        assertFalse(steps.reached)

        val distance = evaluate(GoalType.DISTANCE, 100.0, meters = Double.NEGATIVE_INFINITY)
        assertEquals(0.0, distance.current, 0.0)
        assertFalse(distance.reached)
    }

    @Test
    fun targetValidationMatchesWhatTheSetupScreenCanSave() {
        assertTrue(SessionGoal.isValidTarget(GoalType.NONE, -5.0))
        assertTrue(SessionGoal.isValidTarget(GoalType.PLACE, 0.0))
        assertTrue(SessionGoal.isValidTarget(GoalType.TIME, 600.0))
        assertFalse(SessionGoal.isValidTarget(GoalType.TIME, 0.0))
        assertFalse(SessionGoal.isValidTarget(GoalType.STEPS, -10.0))
        assertFalse(SessionGoal.isValidTarget(GoalType.DISTANCE, Double.NaN))
    }

    @Test
    fun goalEvaluationIsDeterministic() {
        val first = evaluate(GoalType.DISTANCE, 500.0, seconds = 42, steps = 700, meters = 250.0)
        val second = evaluate(GoalType.DISTANCE, 500.0, seconds = 42, steps = 700, meters = 250.0)
        assertEquals(first, second)
    }
}
