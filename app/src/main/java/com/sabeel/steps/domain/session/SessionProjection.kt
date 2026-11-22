package com.sabeel.steps.domain.session

/** One calendar day's slice of a live session, closed when the local date rolls over. */
data class ProjectedLeg(val date: String, val steps: Int)

/**
 * Projection state for a live custom session.
 *
 * A session never owns a step counter: the global day counter keeps running (the owner's rule) and
 * the session is only the difference from the global total captured at [baselineDayTotal]. That
 * keeps the invariant `session steps for the current date <= global day total` structurally true.
 *
 * The full session is the sum of the closed midnight [legs] plus the current date's steps.
 */
data class SessionProjectionState(
    /** Global day total at session start (or at the last midnight rollover). */
    val baselineDayTotal: Int,
    /** Local ISO date the projection currently belongs to; "" until the first tick supplies one. */
    val date: String = "",
    /** Last observed global day total; needed to close the leg when the date rolls over. */
    val lastDayTotal: Int = baselineDayTotal,
    /** Wall clock of the last accepted tick, so a checkpoint can be aged/discarded. */
    val lastUpdateAt: Long = 0L,
    /** Days already closed by midnight rollovers, oldest first. */
    val legs: List<ProjectedLeg> = emptyList(),
)

/** Result of one projection tick. */
data class SessionProjectionUpdate(
    val state: SessionProjectionState,
    /** Steps attributed to the session for the current local date. */
    val sessionSteps: Int,
    /** Leg closed by this update because the date rolled over, else null. */
    val closedLeg: ProjectedLeg?,
    val rolledOver: Boolean,
)

/**
 * Pure session-to-global-total projection. No Android, no storage, no clock: the caller supplies the
 * global day total, the local date and the tick time, and reads back the state to checkpoint.
 *
 * Rules (council amendment A7):
 * - current-date session steps = max(0, global day total - baseline of that date);
 * - a midnight rollover closes the previous date's leg with the last observed steps and resets the
 *   baseline to the new date's global total, so steps are never counted twice and never negative;
 * - the projection never writes `daily_steps`; it only reads the caller's global total.
 *
 * The session step total is [totalSteps] (or the caller's sum of [SessionProjectionUpdate.state]'s
 * legs plus [SessionProjectionUpdate.sessionSteps]); it is not stored here as an inclusive count,
 * because that would violate the "session steps <= day steps" invariant on a multi-day session.
 */
object SessionProjection {
    /** Starts a session on top of the current global day total. */
    fun start(dayTotal: Int): SessionProjectionState {
        val total = dayTotal.coerceAtLeast(0)
        return SessionProjectionState(baselineDayTotal = total, date = "", lastDayTotal = total)
    }

    /**
     * Recomputes the projection from a fresh global day total.
     *
     * [nowMillis] is only recorded in the state (checkpoint age); it never influences step math.
     * [date] is the device's current ISO local date; an empty date is tolerated (tick continues on
     * the existing date) so a caller with a temporarily unavailable date does not fabricate a leg.
     */
    fun update(
        state: SessionProjectionState,
        dayTotal: Int,
        date: String,
        nowMillis: Long,
    ): SessionProjectionUpdate {
        val total = dayTotal.coerceAtLeast(0)
        val currentDate = date.trim()

        // No usable date yet, or the very first tick: adopt the first date we are told about. The
        // baseline stays the one captured at start(), so steps between start() and this tick count.
        if (currentDate.isEmpty() || state.date.isEmpty()) {
            return SessionProjectionUpdate(
                state = state.copy(
                    date = currentDate.ifEmpty { state.date },
                    lastDayTotal = maxOf(state.lastDayTotal, total),
                    lastUpdateAt = nowMillis,
                ),
                sessionSteps = stepsSince(state.baselineDayTotal, total),
                closedLeg = null,
                rolledOver = false,
            )
        }

        if (currentDate == state.date) {
            return SessionProjectionUpdate(
                state = state.copy(
                    lastDayTotal = maxOf(state.lastDayTotal, total),
                    lastUpdateAt = nowMillis,
                ),
                sessionSteps = stepsSince(state.baselineDayTotal, total),
                closedLeg = null,
                rolledOver = false,
            )
        }

        // Date rollover: close yesterday with the last observed total, then start today at zero
        // session steps (today's baseline is today's current global total).
        val previousSteps = stepsSince(state.baselineDayTotal, state.lastDayTotal)
        val closedLeg = if (previousSteps > 0) ProjectedLeg(date = state.date, steps = previousSteps) else null
        return SessionProjectionUpdate(
            state = SessionProjectionState(
                baselineDayTotal = total,
                date = currentDate,
                lastDayTotal = total,
                lastUpdateAt = nowMillis,
                legs = if (closedLeg == null) state.legs else state.legs + closedLeg,
            ),
            sessionSteps = 0,
            closedLeg = closedLeg,
            rolledOver = true,
        )
    }

    /**
     * Final leg of a finished session, to persist next to the closed midnight legs. Returns null when
     * the session is empty or [state] belongs to an earlier date (call [update] once more first).
     */
    fun closingLeg(state: SessionProjectionState, dayTotal: Int): ProjectedLeg? {
        if (state.date.isEmpty()) return null
        val steps = stepsSince(state.baselineDayTotal, dayTotal.coerceAtLeast(0))
        return if (steps > 0) ProjectedLeg(date = state.date, steps = steps) else null
    }

    /** Full session step total: closed legs plus the current date's steps. Never includes day steps before start. */
    fun totalSteps(state: SessionProjectionState, dayTotal: Int): Int {
        val closed = state.legs.sumOf { it.steps.toLong() }
        val current = stepsSince(state.baselineDayTotal, dayTotal.coerceAtLeast(0)).toLong()
        return (closed + current).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun stepsSince(baseline: Int, total: Int): Int =
        (total - baseline.coerceAtLeast(0)).coerceIn(0, Int.MAX_VALUE)
}
