package com.sabeel.steps.diagnostics

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class MidnightBaselineTest {

    private val zone = ZoneId.of("Asia/Baghdad")

    private fun local(day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun beforeMidnightTriggersAtFivePastTheSameNight() {
        val now = local(26, 22, 30)
        val next = MidnightBaseline.nextTriggerAt(now, zone)
        // 00:05 on Nov 27 in Asia/Baghdad (UTC+3) is 21:05 UTC on Nov 26.
        assertEquals("2026-11-26T21:05:00Z", Instant.ofEpochMilli(next).toString())
    }

    @Test
    fun afterFivePastMidnightTriggersTheFollowingNight() {
        val now = local(26, 0, 10)
        val next = MidnightBaseline.nextTriggerAt(now, zone)
        assertEquals("2026-11-26T21:05:00Z", Instant.ofEpochMilli(next).toString())
    }

    @Test
    fun exactlyAtTriggerTimeMovesToTheNextDay() {
        val now = local(26, 0, 5)
        val next = MidnightBaseline.nextTriggerAt(now, zone)
        assertEquals("2026-11-26T21:05:00Z", Instant.ofEpochMilli(next).toString())
    }

    @Test
    fun resultIsStrictlyInTheFutureForAnyNow() {
        val samples = listOf(0L, 1_000L, local(26, 0, 0), local(26, 12, 0), local(26, 23, 59))
        for (now in samples) {
            val next = MidnightBaseline.nextTriggerAt(now, zone)
            assert(next > now) { "next trigger must be in the future: now=$now next=$next" }
        }
    }
}
