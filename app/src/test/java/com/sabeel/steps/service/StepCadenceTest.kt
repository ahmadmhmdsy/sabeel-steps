package com.sabeel.steps.service

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM tests for the active-minute cadence ring. */
class StepCadenceTest {

    private val day = "2026-11-16"
    private val start = 1_700_000_000_000L // fixed epoch, any minute boundary works

    @Test
    fun aFullMinuteOfCadenceCountsAsOneActiveMinute() {
        val cadence = StepCadence()
        cadence.seed(day, 0, start)
        assertEquals(0, cadence.onEvent(0f, start, day))
        // 60 steps in the first minute; the next event finalizes that minute.
        assertEquals(0, cadence.onEvent(60f, start + 30_000L, day))
        assertEquals(1, cadence.onEvent(60f, start + 70_000L, day))
        assertEquals(1, cadence.activeMinutesToday)
    }

    @Test
    fun aSlowMinuteIsNotActive() {
        val cadence = StepCadence()
        cadence.seed(day, 0, start)
        cadence.onEvent(0f, start, day)
        cadence.onEvent(59f, start + 30_000L, day)
        assertEquals(0, cadence.onEvent(59f, start + 70_000L, day))
        assertEquals(0, cadence.activeMinutesToday)
    }

    @Test
    fun seedRestoresActiveMinutesAfterProcessDeath() {
        val cadence = StepCadence()
        cadence.seed(day, 7, start)
        assertEquals(7, cadence.activeMinutesToday)
        cadence.onEvent(10f, start + 5_000L, day)
        assertEquals(7, cadence.activeMinutesToday)
    }

    @Test
    fun counterResetNeverCountsBackwardsOrNegative() {
        val cadence = StepCadence()
        cadence.seed(day, 3, start)
        assertEquals(3, cadence.onEvent(0f, start, day))
        assertEquals(3, cadence.onEvent(5f, start + 5_000L, day))
        // Reboot: raw drops to 2; no negative steps are attributed to the ring.
        val active = cadence.onEvent(2f, start + 10_000L, day)
        assertEquals(3, active)
    }

    @Test
    fun newDayResetsTheActiveMinuteCounter() {
        val cadence = StepCadence()
        cadence.seed(day, 12, start)
        val newDay = "2026-11-17"
        assertEquals(0, cadence.onEvent(500f, start + 86_400_000L, newDay))
        assertEquals(0, cadence.activeMinutesToday)
    }
}