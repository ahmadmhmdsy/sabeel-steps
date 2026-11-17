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

    /**
     * Streak of consecutive goal days, ending today or yesterday (a day still in progress never
     * breaks the streak). A day in [restDays] is a pause, not a break: it adds no day to the streak
     * and it does not end it either, so an intentional rest day keeps the number where it was.
     *
     * The rest flag wins over the step count: marking a day as rest never adds a day to the streak,
     * even if the phone counted enough steps that day (the user said the day was not a walk day).
     */
    fun streak(
        days: List<DailyStep>,
        goalSteps: Int,
        today: LocalDate = LocalDate.now(),
        restDays: Set<String> = emptySet(),
    ): Int {
        val hit = days.filter { it.steps >= goalSteps }.map { LocalDate.parse(it.date) }.toSet()
        if (hit.isEmpty()) return 0
        val rest = restDays.mapNotNull { date -> runCatching { LocalDate.parse(date) }.getOrNull() }.toSet()
        var cursor = if (today in hit || today in rest) today else today.minusDays(1)
        var count = 0
        var checked = 0
        while (checked < MAX_STREAK_LOOKBACK_DAYS) {
            checked++
            when {
                // Rest wins over the count, so a marked day can never add to the streak.
                cursor in rest -> cursor = cursor.minusDays(1)

                cursor in hit -> {
                    count++
                    cursor = cursor.minusDays(1)
                }

                else -> break
            }
        }
        return count
    }

    /** Hard stop for a corrupt or synthetic history; a real streak is never a decade long. */
    private const val MAX_STREAK_LOOKBACK_DAYS = 4_000

    fun totalDistanceMeters(days: List<DailyStep>): Double = days.sumOf { it.distanceMeters }

    // --- Windowed aggregates -------------------------------------------------------------
    // Home, History and Insights all use these helpers so an "average" always means the same
    // thing: the sum over the selected calendar window divided by the window length, with days
    // that have no row counted as zero steps. A missing day is never silently dropped.

    /** Oldest-first ISO dates ending at [today]; empty for a non-positive window. */
    fun windowDates(today: String, days: Int): List<String> {
        if (days <= 0) return emptyList()
        return (days - 1 downTo 0).map { DateKey.minusDays(today, it) }
    }

    /** Maps stored rows onto [window], adding a zero-step day wherever no row exists. */
    fun fillWindow(rows: List<DailyStep>, window: List<String>): List<DailyStep> {
        val byDate = rows.associateBy { it.date }
        return window.map { date -> byDate[date] ?: DailyStep(date = date, steps = 0) }
    }

    fun valuesForWindow(rows: List<DailyStep>, window: List<String>): List<Int> =
        fillWindow(rows, window).map { it.steps }

    fun averageOverWindow(rows: List<DailyStep>, window: List<String>): Int =
        if (window.isEmpty()) 0 else valuesForWindow(rows, window).sum() / window.size

    fun goalHitRateOverWindow(rows: List<DailyStep>, window: List<String>, goalSteps: Int): Double =
        if (window.isEmpty()) 0.0
        else valuesForWindow(rows, window).count { it >= goalSteps }.toDouble() / window.size

    fun goalHitDaysOverWindow(rows: List<DailyStep>, window: List<String>, goalSteps: Int): Int =
        valuesForWindow(rows, window).count { it >= goalSteps }

    fun bestDayOverWindow(rows: List<DailyStep>, window: List<String>): DailyStep? =
        fillWindow(rows, window).filter { it.steps > 0 }.maxByOrNull { it.steps }

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