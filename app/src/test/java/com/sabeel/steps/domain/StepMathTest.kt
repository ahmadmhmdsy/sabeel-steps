package com.sabeel.steps.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM tests for the estimate math and the formatters. */
class StepMathTest {

    private val metric = Units.METRIC
    private val imperial = Units.IMPERIAL

    private fun defaultSettings() = UserSettings()

    @Test
    fun strideDerivesFromHeightAndGender() {
        assertEquals(70.55, StepMath.strideCm(defaultSettings()), 0.001)
        assertEquals(70.21, StepMath.strideCm(defaultSettings().copy(gender = Gender.FEMALE)), 0.001)
        assertEquals(70.55, StepMath.strideCm(defaultSettings().copy(gender = Gender.MALE)), 0.001)
    }

    @Test
    fun explicitStrideWinsAndIsClamped() {
        assertEquals(80.0, StepMath.strideCm(defaultSettings().copy(strideCm = 80)), 0.001)
        assertEquals(40.0, StepMath.strideCm(defaultSettings().copy(strideCm = 10)), 0.001)
        assertEquals(110.0, StepMath.strideCm(defaultSettings().copy(strideCm = 400)), 0.001)
    }

    @Test
    fun distanceIsStepsTimesStride() {
        assertEquals(700.0, StepMath.distanceMeters(1000, 70.0), 0.001)
        assertEquals(0.0, StepMath.distanceMeters(-5, 70.0), 0.001)
    }

    @Test
    fun caloriesScaleWithWeightAndClampNegativeSteps() {
        assertEquals(400.0, StepMath.calories(10000, 70), 0.001)
        assertEquals(200.0, StepMath.calories(10000, 35), 0.001)
        assertEquals(0.0, StepMath.calories(-100, 70), 0.001)
    }

    @Test
    fun streakCountsConsecutiveGoalDaysEndingToday() {
        val day = LocalDate.of(2026, 11, 16)
        val days = listOf(
            DailyStep(date = "2026-11-16", steps = 9000),
            DailyStep(date = "2026-11-15", steps = 8000),
            DailyStep(date = "2026-11-14", steps = 8100),
            DailyStep(date = "2026-11-13", steps = 100),
        )
        assertEquals(3, StepMath.streak(days, goalSteps = 8000, today = day))
    }

    @Test
    fun streakStillCountsWhenTodayIsNotDoneYet() {
        val day = LocalDate.of(2026, 11, 16)
        val days = listOf(
            DailyStep(date = "2026-11-16", steps = 120),
            DailyStep(date = "2026-11-15", steps = 8000),
            DailyStep(date = "2026-11-14", steps = 9000),
        )
        assertEquals(2, StepMath.streak(days, goalSteps = 8000, today = day))
    }

    @Test
    fun streakIsZeroForEmptyHistory() {
        assertEquals(0, StepMath.streak(emptyList(), goalSteps = 8000, today = LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun goalHitRateOverWindowUsesTheWindowAsDenominator() {
        val window = listOf("2026-11-13", "2026-11-14", "2026-11-15", "2026-11-16")
        val rows = listOf(
            DailyStep("2026-11-16", 9000),
            DailyStep("2026-11-15", 3000),
            DailyStep("2026-11-14", 8000),
        )
        // 2 goal days out of 4 window days, even though only 3 rows exist.
        assertEquals(0.5, StepMath.goalHitRateOverWindow(rows, window, goalSteps = 8000), 0.0001)
        assertEquals(2, StepMath.goalHitDaysOverWindow(rows, window, goalSteps = 8000))
        assertEquals(0.0, StepMath.goalHitRateOverWindow(emptyList(), window, goalSteps = 8000), 0.0001)
        assertEquals(0.0, StepMath.goalHitRateOverWindow(rows, emptyList(), goalSteps = 8000), 0.0001)
    }

    @Test
    fun averageOverWindowZeroFillsMissingDays() {
        val window = listOf("2026-11-13", "2026-11-14", "2026-11-15", "2026-11-16")
        val rows = listOf(DailyStep("2026-11-16", 9000), DailyStep("2026-11-15", 3000))
        assertEquals(3000, StepMath.averageOverWindow(rows, window))
        assertEquals(0, StepMath.averageOverWindow(emptyList(), window))
        assertEquals(0, StepMath.averageOverWindow(rows, emptyList()))
    }

    @Test
    fun fillWindowIsOldestFirstAndAddsZeroDays() {
        val window = listOf("2026-11-14", "2026-11-15", "2026-11-16")
        val rows = listOf(DailyStep("2026-11-16", 900, distanceMeters = 600.0))
        val filled = StepMath.fillWindow(rows, window)
        assertEquals(listOf("2026-11-14", "2026-11-15", "2026-11-16"), filled.map { it.date })
        assertEquals(listOf(0, 0, 900), filled.map { it.steps })
        assertEquals(listOf(0, 0, 900), StepMath.valuesForWindow(rows, window))
        assertEquals(900, StepMath.bestDayOverWindow(rows, window)?.steps)
        assertEquals(null, StepMath.bestDayOverWindow(emptyList(), window))
    }

    @Test
    fun windowDatesEndsTodayAndHandlesEmptyWindows() {
        assertEquals(
            listOf("2026-11-14", "2026-11-15", "2026-11-16"),
            StepMath.windowDates("2026-11-16", 3),
        )
        assertEquals(emptyList<String>(), StepMath.windowDates("2026-11-16", 0))
        assertEquals(emptyList<String>(), StepMath.windowDates("2026-11-16", -2))
    }

    @Test
    fun totalsAndDistance() {
        val days = listOf(DailyStep("2026-11-16", 9000, distanceMeters = 6000.0), DailyStep("2026-11-15", 3000, distanceMeters = 2000.0))
        assertEquals(8000.0, StepMath.totalDistanceMeters(days), 0.001)
    }

    @Test
    fun stepsAreGroupedWithoutLocaleSurprises() {
        assertEquals("1,234,567", StepMath.formatSteps(1234567))
        assertEquals("0", StepMath.formatSteps(-4))
    }

    @Test
    fun distanceFormatting() {
        assertEquals("450 m", StepMath.formatDistance(450.0, metric))
        assertEquals("1.2 km", StepMath.formatDistance(1234.5, metric))
        assertEquals("1.0 mi", StepMath.formatDistance(1609.344, imperial))
        assertEquals("33 ft", StepMath.formatDistance(10.0, imperial))
    }

    @Test
    fun paceFormatting() {
        assertEquals("5:00 /km", StepMath.formatPace(300, metric))
        assertEquals("8:03 /mi", StepMath.formatPace(300, imperial))
        assertEquals("--", StepMath.formatPace(0, metric))
    }

    @Test
    fun durationFormatting() {
        assertEquals("0:00", StepMath.formatDuration(0))
        assertEquals("1:05", StepMath.formatDuration(65))
        assertEquals("1:01:01", StepMath.formatDuration(3661))
        assertEquals("0:00", StepMath.formatDuration(-10))
    }

    @Test
    fun caloriesFormatting() {
        assertEquals("400 kcal", StepMath.formatCalories(400.04))
        assertEquals("0 kcal", StepMath.formatCalories(-1.0))
    }

    @Test
    fun dateKeysUseTheDeviceZone() {
        assertEquals("2026-11-16", DateKey.minusDays("2026-11-17", 1))
        assertEquals(0L, DateKey.epochDay("1970-01-01"))
    }
}
