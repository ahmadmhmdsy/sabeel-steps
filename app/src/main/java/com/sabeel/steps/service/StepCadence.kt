package com.sabeel.steps.service

/**
 * Cadence bookkeeping for the step counter service.
 *
 * Keeps a ring of [slots] one-minute buckets (minute key + steps in that minute). A finished minute
 * counts as an "active minute" when at least [activeSpm] steps were taken in it. Values are pure
 * (no Android, no I/O) so the tricky bits - minute rollover, day rollover, counter resets - are
 * deterministic. Thread confinement: this class is driven from the sensor callback thread; the
 * service reads it only from the same (main) thread or through [finish] during teardown.
 *
 * Day awareness: the ring belongs to exactly one local day ([day]). [activeMinutesFor] rolls the
 * day over before returning, so the writer can never stamp yesterday's minutes onto today's row
 * (spec section 3.5 / decision D5). On rollover and at a lifecycle boundary the minute still in
 * progress is counted when its projected cadence reaches the threshold, then the ring is reset.
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
    private var lastEventMillis = 0L

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
        lastEventMillis = nowMillis
        advanceTo(nowMillis / 60_000L)
    }

    /**
     * Feeds one cumulative step-counter value.
     * @return the active minutes counted for the day so far.
     */
    fun onEvent(raw: Float, nowMillis: Long, today: String): Int {
        rolloverIfNeeded(today, nowMillis)
        lastEventMillis = nowMillis
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

    /**
     * Day-aware read used by the writer: rolls the day over first, then returns today's count.
     * A midnight tick therefore returns 0 for the new day instead of yesterday's total.
     */
    fun activeMinutesFor(today: String): Int {
        rolloverIfNeeded(today, lastEventMillis)
        return activeMinutesToday
    }

    /**
     * Closes the in-progress minute (counts it when its projected cadence reaches [activeSpm]) and
     * returns the day's total. Called when the service is torn down so the last partial minute is
     * not silently lost. Does not roll the day over in a way that would move minutes between days:
     * after a day change it simply returns the new day's zero count.
     */
    fun finish(nowMillis: Long, today: String): Int {
        rolloverIfNeeded(today, nowMillis)
        closeInProgressMinute(nowMillis)
        return activeMinutesToday
    }

    private fun rolloverIfNeeded(today: String, nowMillis: Long) {
        if (today == day) return
        if (day.isNotEmpty()) closeInProgressMinute(nowMillis)
        day = today
        activeMinutesToday = 0
        lastRaw = Float.NaN
        currentMinute = Long.MIN_VALUE
        cursor = 0
        minuteKeys.fill(Long.MIN_VALUE)
        minuteSteps.fill(0)
        lastEventMillis = nowMillis
    }

    /** Counts the current bucket if it is already full or projects at/above the threshold. */
    private fun closeInProgressMinute(nowMillis: Long) {
        if (currentMinute == Long.MIN_VALUE) return
        val steps = minuteSteps[cursor]
        if (steps <= 0) return
        if (steps >= activeSpm) {
            activeMinutesToday++
            minuteSteps[cursor] = 0
            return
        }
        val elapsed = (nowMillis - currentMinute * 60_000L).coerceIn(1L, 60_000L)
        val projected = steps * 60_000.0 / elapsed
        if (projected >= activeSpm) {
            activeMinutesToday++
            minuteSteps[cursor] = 0
        }
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
