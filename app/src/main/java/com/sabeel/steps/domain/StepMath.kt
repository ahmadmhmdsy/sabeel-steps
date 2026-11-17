package com.sabeel.steps.domain

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** Pure, unit-tested math and formatting. No Android imports. */
object StepMath {

    fun strideCm(settings: UserSettings): Double {
        if (settings.strideCm > 0) return settings.strideCm.toDouble().coerceIn(40.0, 110.0)
        val factor = if (settings.gender == Gender.FEMALE) 0.413 else 0.415
        return (settings.heightCm * factor).coerceIn(40.0, 110.0)
    }

    fun distanceMeters(steps: Int, strideCm: Double): Double =
        (steps.coerceAtLeast(0) * strideCm / 100.0)

    /** Rough walking energy estimate; labelled as an estimate in the UI. */
    fun calories(steps: Int, weightKg: Int): Double =
        (steps.coerceAtLeast(0) * 0.04 * (weightKg.coerceAtLeast(30) / 70.0)).roundToHalf()

    private fun Double.roundToHalf(): Double = (this * 10).roundToInt() / 10.0

    /** Streak of consecutive days (ending today, or yesterday if today is not done yet). */
    fun streak(days: List<DailyStep>, goalSteps: Int, today: LocalDate = LocalDate.now()): Int {
        val hit = days.filter { it.steps >= goalSteps }.map { LocalDate.parse(it.date) }.toSet()
        if (hit.isEmpty()) return 0
        var cursor = if (today in hit) today else today.minusDays(1)
        var count = 0
        while (cursor in hit) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun goalHitRate(days: List<DailyStep>, goalSteps: Int): Double =
        if (days.isEmpty()) 0.0 else days.count { it.steps >= goalSteps }.toDouble() / days.size

    fun averageSteps(days: List<DailyStep>): Int =
        if (days.isEmpty()) 0 else days.sumOf { it.steps } / days.size

    fun totalDistanceMeters(days: List<DailyStep>): Double = days.sumOf { it.distanceMeters }

    fun formatSteps(steps: Int): String =
        NumberFormat.getIntegerInstance(Locale.US).format(steps.coerceAtLeast(0))

    fun formatDistance(meters: Double, units: Units): String {
        val safe = meters.coerceAtLeast(0.0)
        return if (units == Units.METRIC) {
            if (safe < 1000) "${safe.roundToInt()} m" else String.format(Locale.US, "%.1f km", safe / 1000)
        } else {
            val miles = safe / 1609.344
            if (miles < 0.1) String.format(Locale.US, "%.0f ft", safe * 3.28084)
            else String.format(Locale.US, "%.1f mi", miles)
        }
    }

    fun formatPace(secondsPerKm: Int, units: Units): String {
        if (secondsPerKm <= 0) return "--"
        val secs = if (units == Units.METRIC) secondsPerKm.toDouble() else secondsPerKm * 1.609344
        val total = secs.roundToInt()
        val suffix = if (units == Units.METRIC) "/km" else "/mi"
        return String.format(Locale.US, "%d:%02d %s", total / 60, total % 60, suffix)
    }

    fun formatDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }

    fun formatCalories(kcal: Double): String =
        String.format(Locale.US, "%.0f kcal", kcal.coerceAtLeast(0.0))
}

object DateKey {
    fun today(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()

    fun minusDays(date: String, days: Int): String =
        LocalDate.parse(date).minusDays(days.toLong()).toString()

    fun epochDay(date: String): Long = LocalDate.parse(date).toEpochDay()
}