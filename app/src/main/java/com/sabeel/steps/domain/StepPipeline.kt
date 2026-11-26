package com.sabeel.steps.domain

import kotlin.math.min
import kotlin.math.max

/**
 * Durable state of the step manifold: one fused daily total backed by four independent ledgers.
 *
 * The old pipeline had a single baseline and one active source; switching sources rebased and threw
 * the new source's catch-up away. The manifold keeps a ledger per source instead:
 *
 *  - [counterToday]  hardware step counter deltas (cumulative since boot)
 *  - [detectorToday] hardware step detector events (one per step, accumulated even when the counter
 *                    is the one displayed)
 *  - [hcToday]       absolute floor from Health Connect (never lowered)
 *  - [estimateToday] accelerometer estimate; it starts at the current fused total so it only ever
 *                    fills the part no confirmed source has seen
 *
 * [confirmed] is the best measured/synced evidence and only grows within a day. [total] is the
 * monotonic fused day, and the displayed estimate is derived as `total - confirmed` — never
 * accumulated separately, so a late sync can only shrink the estimated portion, never inflate it.
 *
 * Every function here is pure. The Android layer owns liveness/freshness and passes only numeric
 * evidence; the day string is supplied by the caller so the state machine is testable across
 * midnight and time zones.
 */
data class StepState(
    /** Local ISO date the ledgers belong to. Empty only for a brand-new install. */
    val day: String = "",
    /** Monotonic fused total for [day]. */
    val total: Int = 0,
    /** Best confirmed (measured or synced) evidence for [day]; `total >= confirmed`. */
    val confirmed: Int = 0,
    /** Hardware counter steps attributed to [day]. */
    val counterToday: Int = 0,
    /** Last raw cumulative counter value, or NaN when no counter sample has been seen today. */
    val counterLastRaw: Float = Float.NaN,
    /** Monotonic sensor timestamp (ns) of [counterLastRaw], 0 = unknown. */
    val counterLastEventNanos: Long = 0L,
    /** Hardware detector steps attributed to [day]. */
    val detectorToday: Int = 0,
    /** Monotonic sensor timestamp (ns) of the last detector event, 0 = unknown. */
    val detectorLastEventNanos: Long = 0L,
    /** Absolute Health Connect total for [day]. */
    val hcToday: Int = 0,
    /**
     * Same-day Health Connect baseline captured when counting was resumed; the provider total is
     * applied as `total - hcOffsetToday` so steps taken while counting was off are never backfilled.
     */
    val hcOffsetToday: Int = 0,
    /**
     * True when the Health Connect floor must be ignored until the next local midnight because no
     * same-day baseline exists (for example HC was granted only after the pause started).
     */
    val hcSuppressedForDay: Boolean = false,
    /** Accelerometer estimate accumulator (already includes the baseline it started from). */
    val estimateToday: Int = 0,
    /** True once any real source has contributed today; keeps a fresh install from writing a zero row. */
    val initialized: Boolean = false,
    /** Wall-clock time of the last state update (diagnostics only). */
    val updatedAt: Long = 0L,
) {
    /** Estimated (unconfirmed) portion of [total]; derived, never stored separately in the total. */
    val estimated: Int get() = (total - confirmed).coerceAtLeast(0)

    /** True when the day contains steps that no measured/synced source confirmed. */
    val hasEstimate: Boolean get() = estimated > 0

    companion object {
        fun initial(day: String) = StepState(day = day)
    }
}

/** Result of applying one observation: the new state plus whether the local day changed. */
data class StepUpdate(
    val state: StepState,
    val rolledOver: Boolean = false,
)

/**
 * A decision the pipeline made about one sample that a support report needs to explain a gap.
 * The enum is plain Kotlin (no Android types) so the pure pipeline stays unit-testable; the service
 * maps it to a flight-recorder line.
 */
enum class PipelineEvent {
    /** The local day changed; yesterday's ledgers were replaced by a fresh baseline. */
    DAY_ROLLOVER,

    /** First counter sample of the day: it becomes the baseline and counts nothing. */
    BASELINE,

    /** The counter value went backwards (reboot/reset): rebased without adding. */
    REBOOT_REBASE,

    /** A stale sensor sample arrived after a newer one and was ignored. */
    OUT_OF_ORDER,

    /** A jump exceeded the plausible cadence and part of it was dropped. */
    CLAMPED,

    /** The delta was accepted with no usable timestamp (first sample after a restart). */
    CATCH_UP_UNKNOWN,
}

/** Optional diagnostics hook for [StepPipeline]. Never affects the arithmetic. */
fun interface PipelineObserver {
    fun onEvent(event: PipelineEvent)
}

/**
 * Pure manifold arithmetic. Public constants are the plausibility policy shared with tests/docs.
 */
object StepPipeline {

    /** Fastest humanly plausible sustained cadence, used to clamp a single counter jump. */
    const val MAX_STEPS_PER_SECOND = 6.0

    /** A delta larger than `seconds * MAX_STEPS_PER_SECOND + grace` is treated as a glitch. */
    const val CLAMP_GRACE_STEPS = 5

    /** A detector event carries one step; values above this are a HAL glitch, not 10 steps. */
    const val MAX_DETECTOR_EVENT = 10

    /** Two detector events closer than this are collapsed to at most one step. */
    const val MIN_DETECTOR_EVENT_GAP_NANOS = 100_000_000L

    /**
     * v1.1 -> v1.2 state migration. The old schema kept one counter baseline plus the day total; its
     * estimate marker was persisted only in the durable daily row (see [preserveLegacyMarker]). The
     * migrated state therefore treats the old total as confirmed and keeps the old baseline so the
     * next counter sample adds only its real delta. Pure so the upgrade path is unit-tested.
     */
    fun migrateLegacyState(
        today: String,
        legacyTotal: Int,
        legacyBaselineRaw: Float,
        legacyBaselineDate: String,
        legacyInitialized: Boolean,
    ): StepState {
        val total = legacyTotal.coerceAtLeast(0)
        if (!legacyInitialized && total == 0) return StepState.initial(today)
        val raw = if (legacyBaselineRaw.isFinite()) legacyBaselineRaw else Float.NaN
        return StepState(
            day = legacyBaselineDate.ifEmpty { today },
            total = total,
            confirmed = total,
            counterToday = if (raw.isFinite()) total else 0,
            counterLastRaw = raw,
            detectorToday = 0,
            hcToday = 0,
            estimateToday = 0,
            initialized = legacyInitialized,
        )
    }

    /** Applies one cumulative hardware-counter sample. */
    fun onCounter(
        state: StepState,
        raw: Float,
        today: String,
        eventNanos: Long = 0L,
        nowMillis: Long = 0L,
        observer: PipelineObserver? = null,
    ): StepUpdate {
        if (!raw.isFinite()) return StepUpdate(state)
        val (base, rolled) = dayBase(state, today, nowMillis)
        var next = base.copy(initialized = true, updatedAt = if (nowMillis > 0) nowMillis else base.updatedAt)
        if (rolled) observer?.onEvent(PipelineEvent.DAY_ROLLOVER)

        // First sample of the day/process: establish the baseline, count nothing. On a fresh install
        // this is what prevents the since-boot value from becoming today's steps.
        if (next.counterLastRaw.isNaN()) {
            observer?.onEvent(PipelineEvent.BASELINE)
            return StepUpdate(
                fuse(next.copy(counterLastRaw = raw, counterLastEventNanos = eventNanos.coerceAtLeast(0))),
                rolled,
            )
        }

        val lastRaw = next.counterLastRaw
        val lastNanos = next.counterLastEventNanos

        val delta = raw - lastRaw
        if (delta < -1f) {
            // Reboot, a counter reset, or a restored state from an older boot: the monotonic sensor
            // clock also restarted, so this must be checked BEFORE the out-of-order guard below.
            // Rebase without adding; steps already attributed today stay.
            observer?.onEvent(PipelineEvent.REBOOT_REBASE)
            return StepUpdate(
                fuse(next.copy(counterLastRaw = raw, counterLastEventNanos = eventNanos.coerceAtLeast(0))),
                rolled,
            )
        }

        // Out-of-order sensor delivery must not be re-applied: it would double count the same steps
        // (a stale sample has a HIGHER raw delta, so it cannot be confused with the reboot case).
        if (eventNanos > 0 && lastNanos > 0 && eventNanos <= lastNanos) {
            observer?.onEvent(PipelineEvent.OUT_OF_ORDER)
            return StepUpdate(state)
        }

        val accepted: Int
        if (delta <= 0f) {
            accepted = 0
        } else {
            val whole = delta.toInt().coerceAtLeast(0)
            val elapsedNanos = if (eventNanos > 0 && lastNanos > 0) eventNanos - lastNanos else 0L
            accepted = if (elapsedNanos > 0) {
                val plausible = (elapsedNanos / 1_000_000_000.0 * MAX_STEPS_PER_SECOND).toInt() +
                    CLAMP_GRACE_STEPS
                if (whole > plausible) observer?.onEvent(PipelineEvent.CLAMPED)
                min(whole, plausible)
            } else {
                // Timestamp unknown (first event after a restart on old state): accept the catch-up
                // rather than drop steps. The counter aggregate is trusted; only its rate is not.
                if (whole > 0) observer?.onEvent(PipelineEvent.CATCH_UP_UNKNOWN)
                whole
            }
        }

        next = next.copy(
            counterToday = (next.counterToday + accepted).coerceAtLeast(0),
            counterLastRaw = raw,
            counterLastEventNanos = if (eventNanos > 0) eventNanos else lastNanos,
        )
        return StepUpdate(fuse(next), rolled)
    }

    /** Applies one hardware step-detector event (value is normally 1.0). */
    fun onDetector(
        state: StepState,
        steps: Int,
        today: String,
        eventNanos: Long = 0L,
        nowMillis: Long = 0L,
        observer: PipelineObserver? = null,
    ): StepUpdate {
        if (steps <= 0) return StepUpdate(state)
        val (base, rolled) = dayBase(state, today, nowMillis)
        if (rolled) observer?.onEvent(PipelineEvent.DAY_ROLLOVER)
        var added = steps.coerceAtMost(MAX_DETECTOR_EVENT)
        if (eventNanos > 0 && base.detectorLastEventNanos > 0 &&
            eventNanos > base.detectorLastEventNanos &&
            eventNanos - base.detectorLastEventNanos < MIN_DETECTOR_EVENT_GAP_NANOS
        ) {
            added = min(added, 1)
        }
        val next = base.copy(
            detectorToday = (base.detectorToday + added).coerceAtLeast(0),
            detectorLastEventNanos = if (eventNanos > 0) eventNanos else base.detectorLastEventNanos,
            initialized = true,
            updatedAt = if (nowMillis > 0) nowMillis else base.updatedAt,
        )
        return StepUpdate(fuse(next), rolled)
    }

    /**
     * Applies estimated steps from the accelerometer. The first estimate of the day starts the
     * accumulator at the current fused total, so estimated steps are an extension of the day, not a
     * second count of steps already confirmed.
     */
    fun onEstimate(
        state: StepState,
        steps: Int,
        today: String,
        nowMillis: Long = 0L,
    ): StepUpdate {
        if (steps <= 0) return StepUpdate(state)
        val (base, rolled) = dayBase(state, today, nowMillis)
        val floor = maxOf(base.total, base.confirmed)
        val accumulator = if (base.estimateToday <= 0) floor else base.estimateToday
        val next = base.copy(
            estimateToday = accumulator + steps.coerceAtMost(MAX_DETECTOR_EVENT),
            initialized = true,
            updatedAt = if (nowMillis > 0) nowMillis else base.updatedAt,
        )
        return StepUpdate(fuse(next), rolled)
    }

    /**
     * Applies Health Connect's absolute "steps since local midnight" total as a floor, after
     * removing the same-day baseline captured at resume ([StepState.hcOffsetToday]). While
     * [StepState.hcSuppressedForDay] is set the floor is ignored: subtracting an unknown paused
     * baseline is impossible, and applying the raw total would retroactively add every step taken
     * while counting was off. The suppression clears on the next local midnight (a fresh day has a
     * fresh provider total and no paused gap inside it).
     */
    fun onExternalTotal(
        state: StepState,
        totalSteps: Int,
        today: String,
        nowMillis: Long = 0L,
    ): StepUpdate {
        if (totalSteps < 0) return StepUpdate(state)
        val (base, rolled) = dayBase(state, today, nowMillis)
        if (base.hcSuppressedForDay) return StepUpdate(fuse(base), rolled)
        val floor = (totalSteps - base.hcOffsetToday).coerceAtLeast(0)
        val next = base.copy(
            hcToday = max(base.hcToday, floor),
            total = max(base.total, floor),
            initialized = base.initialized || floor > 0,
            updatedAt = if (nowMillis > 0) nowMillis else base.updatedAt,
        )
        return StepUpdate(fuse(next), rolled)
    }

    /**
     * Freezes the manifold when the user deliberately stops counting.
     *
     * Every day total is history and is kept as-is; only the *evidence baselines* are dropped so
     * the next samples can never be interpreted as steps taken while counting was off:
     *  - the cumulative counter baseline becomes NaN (the same rule as a fresh install), so the
     *    first sample after [resume] only rebases and adds nothing;
     *  - detector/event timestamps are cleared (the sensors are unregistered while paused);
     *  - the Health Connect floor is suppressed until [resume] supplies a same-day baseline or the
     *    local day rolls over.
     *
     * [StepState.total], [StepState.confirmed], the per-source ledgers and the estimate marker are
     * deliberately preserved: nothing already reported as today's steps may disappear from Home.
     */
    fun pause(state: StepState, nowMillis: Long = 0L): StepState = state.copy(
        counterLastRaw = Float.NaN,
        counterLastEventNanos = 0L,
        detectorLastEventNanos = 0L,
        hcSuppressedForDay = true,
        updatedAt = if (nowMillis > 0) nowMillis else state.updatedAt,
    )

    /**
     * Explicit resume entry point (never routed through [onCounter], whose permissive catch-up path
     * would count the paused gap as measured evidence).
     *
     * The counter is rebased to [raw] with a delta of 0; a non-finite [raw] (no sensor sample yet)
     * leaves the NaN baseline in place so the first real sample still only rebases. The detector and
     * estimate ledgers continue from the values pause preserved (their sensors were unregistered, so
     * no off-period events exist to backfill). Health Connect restarts from [hcReading] when the
     * provider answered (>= 0): later provider totals are reduced by that baseline. With no same-day
     * reading the floor is suppressed until midnight rather than guessed.
     *
     * [dayBase] runs first, so a resume after midnight rolls to the new day with zero and the frozen
     * old row is untouched.
     */
    fun resume(
        state: StepState,
        raw: Float,
        today: String,
        hcReading: Int?,
        nowMillis: Long = 0L,
    ): StepState {
        val (base, _) = dayBase(state, today, nowMillis)
        val hasHcBaseline = hcReading != null && hcReading >= 0
        val next = base.copy(
            counterLastRaw = if (raw.isFinite()) raw else Float.NaN,
            counterLastEventNanos = 0L,
            detectorLastEventNanos = 0L,
            hcToday = 0,
            hcOffsetToday = if (hasHcBaseline) hcReading!! else 0,
            hcSuppressedForDay = !hasHcBaseline,
            updatedAt = if (nowMillis > 0) nowMillis else base.updatedAt,
        )
        return fuse(next)
    }

    /** Rolls to [today] without any new evidence (service start, midnight reconcile, boot). */
    fun reconcile(
        state: StepState,
        today: String,
        nowMillis: Long = 0L,
        observer: PipelineObserver? = null,
    ): StepUpdate {
        val (base, rolled) = dayBase(state, today, nowMillis)
        if (rolled) observer?.onEvent(PipelineEvent.DAY_ROLLOVER)
        return StepUpdate(fuse(base), rolled)
    }

    /**
     * Max rule against the durable daily row: a row written before a crash, or restored from CSV,
     * must never be lowered by the in-memory state.
     */
    fun resolveTodaySteps(state: StepState, dbTodaySteps: Int?): Int =
        max(state.total, dbTodaySteps ?: 0).coerceAtLeast(0)

    /**
     * Marker rule: the estimated portion persisted for the row is the state's derived marker, except
     * when the durable row is ahead of the state (CSV restore), in which case its own marker wins.
     */
    fun resolveEstimatedSteps(stateEstimated: Int, existingEstimated: Int?, rowIsAhead: Boolean): Int {
        val marker = if (rowIsAhead) max(stateEstimated, existingEstimated ?: 0) else stateEstimated
        return marker.coerceAtLeast(0)
    }

    /**
     * v1.1 upgrade rule: the old schema never persisted the estimate marker in DataStore, so the
     * first v1.2 write must not erase a marker that only the durable daily row carries. Preserve it
     * only while the totals are equal and the new state has no marker of its own; as soon as new
     * measured evidence moves the total, the normal derived-marker rule applies again.
     */
    fun preserveLegacyMarker(
        marker: Int,
        steps: Int,
        existingSteps: Int?,
        existingMarker: Int?,
    ): Int {
        if (marker > 0 || existingSteps == null || existingMarker == null) return marker
        if (existingSteps != steps || existingMarker <= 0) return marker
        return existingMarker.coerceIn(0, steps.coerceAtLeast(0))
    }

    /**
     * Confirmation invariant: confirmed = max(confirmed, every confirmed ledger), and the total never
     * falls below either the confirmed evidence or the estimate extension. [state.total] is
     * monotonically non-decreasing within a day because the previous total is part of the max.
     */
    private fun fuse(state: StepState): StepState {
        val counter = state.counterToday.coerceAtLeast(0)
        val detector = state.detectorToday.coerceAtLeast(0)
        val hc = state.hcToday.coerceAtLeast(0)
        val confirmed = maxOf(state.confirmed, counter, detector, hc).coerceAtLeast(0)
        val total = maxOf(state.total, confirmed, state.estimateToday).coerceAtLeast(0)
        return state.copy(total = total, confirmed = confirmed)
    }

    /**
     * Resets every ledger when the local day changes. The step delta between the last sample of
     * yesterday and the first sample of today cannot be attributed to either day, so it is dropped
     * (the spec rule) rather than inflating the new day. A new day also clears the pause/resume
     * Health Connect bookkeeping: there is no paused gap inside the new provider day, so a fresh
     * (zero) baseline is correct and suppression must not leak across midnight.
     */
    private fun dayBase(state: StepState, today: String, nowMillis: Long): Pair<StepState, Boolean> {
        if (state.day == today) return state to false
        val rolled = state.day.isNotEmpty()
        return StepState(day = today, updatedAt = nowMillis.coerceAtLeast(0)) to rolled
    }
}