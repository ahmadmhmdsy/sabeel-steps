package com.sabeel.steps.domain

/**
 * Persisted sensor pipeline state (lives in DataStore together with user settings).
 *
 * [initialized] separates a genuinely fresh baseline from a zero counter reading. Until the first
 * real sensor sample arrives we must not treat the lifetime hardware counter as today's steps, and
 * after a delete-all we must not resurrect the previous user's total.
 *
 * [source] tags which collector produced [baselineRaw], so a fallback can rebase without treating a
 * different sensor's scale as a counter catch-up. [lastRawAt] (wall-clock ms, 0 = unknown) lets the
 * pipeline reject physically impossible jumps only; it never throttles normal walking.
 */
data class StepState(
    val baselineRaw: Float,
    val todaySteps: Int,
    val baselineDate: String,
    val initialized: Boolean = false,
    val source: StepSource = StepSource.COUNTER,
    val lastRawAt: Long = 0L,
) {
    companion object {
        fun initial(date: String) = StepState(
            baselineRaw = 0f,
            todaySteps = 0,
            baselineDate = date,
            initialized = false,
            source = StepSource.COUNTER,
            lastRawAt = 0L,
        )
    }
}

data class StepUpdate(
    val state: StepState,
    val todaySteps: Int,
    val rolledOver: Boolean,
)

/**
 * Reconciles the cumulative step source with the local day.
 * Pure function so the tricky cases (first run, reboot, process death, midnight, clock rollback,
 * source switch, implausible catch-up) are unit-testable.
 */
object StepPipeline {

    /**
     * Upper bound of a plausible single delta. A person cannot take more than ~4 steps per second;
     * the small grace absorbs the batching jitter of a cumulative counter. Anything above the bound
     * is a sensor artifact (or a counter that kept running while another source was live), not steps.
     */
    private const val MAX_STEPS_PER_SECOND = 4.0
    private const val CLAMP_GRACE_STEPS = 3

    fun onSensor(
        state: StepState,
        raw: Float,
        today: String,
        source: StepSource = state.source,
        nowMillis: Long = 0L,
    ): StepUpdate {
        // Sensor hardware never reports non-finite values; if it does, ignore the event rather
        // than poisoning the persisted baseline with NaN/Infinity.
        if (!raw.isFinite()) return StepUpdate(state, state.todaySteps, rolledOver = false)

        // First reading ever (fresh install or after delete-all): the source is cumulative-since-
        // boot (or its synthetic equivalent), so it is a baseline, not progress. Keep any today total
        // that was restored from a backup; on a genuine first run that total is already zero.
        if (!state.initialized) {
            val steps = state.todaySteps.coerceAtLeast(0)
            return StepUpdate(
                state = StepState(
                    baselineRaw = raw,
                    todaySteps = steps,
                    baselineDate = today,
                    initialized = true,
                    source = source,
                    lastRawAt = nowMillis,
                ),
                todaySteps = steps,
                rolledOver = false,
            )
        }

        // Date changed in either direction (midnight, manual clock change, time zone travel).
        // Steps taken between the last sample and this one cannot be attributed to either day, so
        // per spec section 3.5 they are dropped rather than inflating the new day. A source switch
        // that happens across midnight still rolls over; today's total belongs to the old day.
        if (today != state.baselineDate) {
            return StepUpdate(
                state = StepState(
                    baselineRaw = raw,
                    todaySteps = 0,
                    baselineDate = today,
                    initialized = true,
                    source = source,
                    lastRawAt = nowMillis,
                ),
                todaySteps = 0,
                rolledOver = true,
            )
        }

        // Source switch (counter -> detector -> estimate or back). The new source's raw value has a
        // different origin/scale, so it becomes the new baseline and the switch delta is dropped.
        // The day's total is preserved: the user already walked those steps.
        if (source != state.source) {
            val steps = state.todaySteps.coerceAtLeast(0)
            return StepUpdate(
                state = StepState(
                    baselineRaw = raw,
                    todaySteps = steps,
                    baselineDate = today,
                    initialized = true,
                    source = source,
                    lastRawAt = nowMillis,
                ),
                todaySteps = steps,
                rolledOver = false,
            )
        }

        // Counter reset (reboot, sensor reset): rebase and count nothing for the lost interval.
        val baseline = if (raw < state.baselineRaw - 1f) raw else state.baselineRaw
        val rawDelta = (raw - baseline).coerceAtLeast(0f)
        val plausibleDelta = clampDelta(rawDelta, state.lastRawAt, nowMillis)
        val todaySteps = (state.todaySteps + plausibleDelta).coerceAtLeast(0)
        return StepUpdate(
            state = StepState(
                baselineRaw = raw,
                todaySteps = todaySteps,
                baselineDate = today,
                initialized = true,
                source = source,
                lastRawAt = if (nowMillis > 0L) nowMillis else state.lastRawAt,
            ),
            todaySteps = todaySteps,
            rolledOver = false,
        )
    }

    /**
     * Only rejects the physically impossible. With no trusted previous timestamp (fresh state,
     * import, clock oddity) nothing is clamped, because the pipeline's job is not to police time.
     */
    private fun clampDelta(rawDelta: Float, lastRawAt: Long, nowMillis: Long): Int {
        val delta = rawDelta.toInt().coerceAtLeast(0)
        if (nowMillis <= 0L || lastRawAt <= 0L || nowMillis <= lastRawAt) return delta
        val elapsedSeconds = (nowMillis - lastRawAt) / 1_000.0
        val maxPlausible = (elapsedSeconds * MAX_STEPS_PER_SECOND).toInt() + CLAMP_GRACE_STEPS
        return delta.coerceAtMost(maxPlausible)
    }

    /**
     * The database is the durable record for a day. A process can die after persisting the state
     * but before the row write lands, so every write resolves against what is already stored and
     * never lowers a day's total.
     */
    fun resolveTodaySteps(state: StepState, dbTodaySteps: Int?): Int =
        maxOf(state.todaySteps.coerceAtLeast(0), dbTodaySteps?.coerceAtLeast(0) ?: 0)
}
