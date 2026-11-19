package com.sabeel.steps.domain

/**
 * Decides when the active measured source has gone silent while the user is still walking.
 *
 * The policy is pure and deliberately conservative:
 *  - a source is only "stalled" after [stallAfterMs] with no event;
 *  - a motion probe costs at most 5 s of accelerometer, so probes back off exponentially while the
 *    phone stays still (60 s, 2 min, 4 min ... up to [maxProbeIntervalMs]); a night-long stillness
 *    must not mean a probe every minute;
 *  - a single walking probe is not enough (the phone may be in a bag);
 *  - [walkingProbesToSwitch] consecutive walking probes request a fallback;
 *  - any real event resets the stall clock and the probe streak.
 *
 * It does not know which sensor to switch to; the service owns registration and availability.
 */
class StepSourceHealth(
    private val stallAfterMs: Long = 600_000L,
    private val probeIntervalMs: Long = 60_000L,
    private val walkingProbesToSwitch: Int = 2,
    private val maxProbeIntervalMs: Long = 30 * 60_000L,
) {
    sealed interface Action {
        data object None : Action
        data object ProbeMotion : Action
        data object Fallback : Action
    }

    var source: StepSource = StepSource.NONE
        private set

    /** Number of actual source transitions (initial start is not a switch). */
    var switches: Int = 0
        private set

    private var sourceStartedAt = 0L
    private var lastEventAt = 0L
    private var nextProbeAt = 0L
    private var currentProbeInterval = probeIntervalMs
    private var walkingProbes = 0
    private var probeInFlight = false

    fun onSourceStart(source: StepSource, nowMillis: Long) {
        if (this.source != StepSource.NONE && this.source != source) switches++
        this.source = source
        sourceStartedAt = nowMillis
        lastEventAt = nowMillis
        nextProbeAt = nowMillis + stallAfterMs
        currentProbeInterval = probeIntervalMs
        walkingProbes = 0
        probeInFlight = false
    }

    /** A real sensor event arrived for the active source: the source is alive. */
    fun onEvent(nowMillis: Long) {
        lastEventAt = nowMillis
        nextProbeAt = nowMillis + stallAfterMs
        currentProbeInterval = probeIntervalMs
        walkingProbes = 0
        probeInFlight = false
    }

    /** Called from the heartbeat loop. */
    fun onTick(nowMillis: Long): Action {
        if (source == StepSource.NONE || source == StepSource.MOTION_ESTIMATE) return Action.None
        if (probeInFlight) return Action.None
        val anchor = if (lastEventAt > 0L) lastEventAt else sourceStartedAt
        if (nowMillis - anchor < stallAfterMs) {
            walkingProbes = 0
            return Action.None
        }
        if (nowMillis < nextProbeAt) return Action.None
        nextProbeAt = nowMillis + currentProbeInterval
        probeInFlight = true
        return Action.ProbeMotion
    }

    /** Called once after a motion probe window, with the detector's verdict. */
    fun onProbeResult(nowMillis: Long, walking: Boolean): Action {
        if (!probeInFlight) return Action.None
        probeInFlight = false
        if (!walking) {
            // Battery guard: a still phone is probed less and less often.
            walkingProbes = 0
            currentProbeInterval = (currentProbeInterval * 2L).coerceAtMost(maxProbeIntervalMs)
            nextProbeAt = nowMillis + currentProbeInterval
            return Action.None
        }
        walkingProbes++
        currentProbeInterval = probeIntervalMs
        nextProbeAt = nowMillis + probeIntervalMs
        return if (walkingProbes >= walkingProbesToSwitch) {
            walkingProbes = 0
            Action.Fallback
        } else {
            Action.None
        }
    }
}
