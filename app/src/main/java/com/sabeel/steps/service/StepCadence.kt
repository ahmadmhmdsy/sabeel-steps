package com.sabeel.steps.service

/**
 * Cadence bookkeeping for the step counter service.
 *
 * Keeps a ring of [slots] one-minute buckets (minute key + steps in that minute). A finished minute
 * counts as an "active minute" when at least [activeSpm] steps were taken in it. Values are pure
 * (no Android, no I/O) so the tricky bits - minute rollover, day rollover, counter resets - are
 * deterministic. Thread confinement: this class is driven from the sensor callback thread only.
 */
class StepCadence(
    private val slots: Int = 60,
    private val activeSpm: Int = 60,
) {
    private val minuteKeys = LongArray(slots) { Long.MIN_VALUE }
    private val minuteSteps = IntArray(slots)
    private var cursor = 0
    private var currentMinute = Long.MIN_VALUE
    private var lastRaw = Float.NaN
    private var day = ""

    /** Active minutes accumulated for [day], seeded from the persisted DailySteps row. */
    var activeMinutesToday: Int = 0
        private set

    /** Loads the day's already-counted active minutes (process-death restore) and anchors the ring. */
    fun seed(date: String, activeMinutes: Int, nowMillis: Long) {
        day = date
        activeMinutesToday = activeMinutes.coerceAtLeast(0)
        lastRaw = Float.NaN
        currentMinute = Long.MIN_VALUE
        cursor = 0
        minuteKeys.fill(Long.MIN_VALUE)
        minuteSteps.fill(0)
        advanceTo(nowMillis / 60_000L)
    }

    /**
     * Feeds one cumulative step-counter value.
     * @return the active minutes counted for the day so far.
     */
    fun onEvent(raw: Float, nowMillis: Long, today: String): Int {
        if (today != day) {
            // New local day: the ring and the day counter start over.
            day = today
            activeMinutesToday = 0
            lastRaw = Float.NaN
            currentMinute = Long.MIN_VALUE
            cursor = 0
            minuteKeys.fill(Long.MIN_VALUE)
            minuteSteps.fill(0)
        }
        val delta: Int = when {
            lastRaw.isNaN() -> {
                lastRaw = raw
                0
            }
            raw < lastRaw - 1f -> {
                // Counter reset (reboot / sensor reset): rebase, do not count backwards.
                lastRaw = raw
                0
            }
            else -> {
                val d = (raw - lastRaw).toInt().coerceAtLeast(0)
                lastRaw = raw
                d
            }
        }
        advanceTo(nowMillis / 60_000L)
        if (delta > 0) minuteSteps[cursor] = minuteSteps[cursor] + delta
        return activeMinutesToday
    }

    private fun advanceTo(minute: Long) {
        if (currentMinute == Long.MIN_VALUE) {
            currentMinute = minute
            cursor = 0
            minuteKeys[0] = minute
            minuteSteps[0] = 0
            return
        }
        if (minute <= currentMinute) return
        while (currentMinute < minute) {
            if (minuteSteps[cursor] >= activeSpm) activeMinutesToday++
            currentMinute++
            cursor = (cursor + 1) % slots
            minuteKeys[cursor] = currentMinute
            minuteSteps[cursor] = 0
        }
    }
}
