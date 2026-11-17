package com.sabeel.steps.domain

/**
 * Persisted sensor pipeline state (lives in DataStore together with user settings).
 *
 * [initialized] separates a genuinely fresh baseline from a zero counter reading. Until the first
 * real sensor sample arrives we must not treat the lifetime hardware counter as today's steps, and
 * after a delete-all we must not resurrect the previous user's total.
 */
data class StepState(
    val baselineRaw: Float,
    val todaySteps: Int,
    val baselineDate: String,
    val initialized: Boolean = false,
) {
    companion object {
        fun initial(date: String) = StepState(baselineRaw = 0f, todaySteps = 0, baselineDate = date, initialized = false)
    }
}

data class StepUpdate(
    val state: StepState,
    val todaySteps: Int,
    val rolledOver: Boolean,
)

/**
 * Reconciles the cumulative hardware step counter with the local day.
 * Pure function so the tricky cases (first run, reboot, process death, midnight, clock rollback)
 * are unit-testable.
 */
object StepPipeline {

    fun onSensor(state: StepState, raw: Float, today: String): StepUpdate {
        // Sensor hardware never reports non-finite values; if it does, ignore the event rather
        // than poisoning the persisted baseline with NaN/Infinity.
        if (!raw.isFinite()) return StepUpdate(state, state.todaySteps, rolledOver = false)

        // First reading ever (fresh install or after delete-all): the counter is cumulative-since-
        // boot, so it is a baseline, not progress. Keep any today total that was restored from a
        // backup; on a genuine first run that total is already zero.
        if (!state.initialized) {
            return StepUpdate(
                state = StepState(
                    baselineRaw = raw,
                    todaySteps = state.todaySteps.coerceAtLeast(0),
                    baselineDate = today,
                    initialized = true,
                ),
                todaySteps = state.todaySteps.coerceAtLeast(0),
                rolledOver = false,
            )
        }

        // Date changed in either direction (midnight, manual clock change, time zone travel).
        // Steps taken between the last sample and this one cannot be attributed to either day, so
        // per spec section 3.5 they are dropped rather than inflating the new day.
        if (today != state.baselineDate) {
            return StepUpdate(
                state = StepState(baselineRaw = raw, todaySteps = 0, baselineDate = today, initialized = true),
                todaySteps = 0,
                rolledOver = true,
            )
        }

        // Counter reset (reboot, sensor reset): rebase and count nothing for the lost interval.
        val baseline = if (raw < state.baselineRaw - 1f) raw else state.baselineRaw
        val delta = (raw - baseline).coerceAtLeast(0f).toInt()
        val todaySteps = (state.todaySteps + delta).coerceAtLeast(0)
        return StepUpdate(
            state = StepState(baselineRaw = raw, todaySteps = todaySteps, baselineDate = today, initialized = true),
            todaySteps = todaySteps,
            rolledOver = false,
        )
    }

    /**
     * The database is the durable record for a day. A process can die after persisting the state
     * but before the row write lands, so every write resolves against what is already stored and
     * never lowers a day's total.
     */
    fun resolveTodaySteps(state: StepState, dbTodaySteps: Int?): Int =
        maxOf(state.todaySteps.coerceAtLeast(0), dbTodaySteps?.coerceAtLeast(0) ?: 0)
}
