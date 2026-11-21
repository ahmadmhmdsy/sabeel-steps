package com.sabeel.steps.domain

/**
 * Decides when the measured sensors are silent *and the user is still walking*, so the service can
 * start the labelled accelerometer estimate instead of mistaking silence for an idle day.
 *
 * The old [StepSourceHealth] could self-disable: it set "probe in flight" inside `onTick` before a
 * probe was actually scheduled, and the service could return early (no accelerometer, registration
 * refused), so `onProbeResult` never ran and the watchdog stayed in-flight forever. Here in-flight
 * is only set by [probeScheduled], after the service has really registered the probe, and every
 * no-op path has an explicit clearing method.
 *
 * Policy:
 *  - silence is measured from the last real counter/detector event (or service start);
 *  - the first probe starts after [silenceAfterMs];
 *  - a probe that finds no walking backs off exponentially to [maxProbeIntervalMs];
 *  - one walking probe is enough to start the estimate (the old two-probe rule delayed recovery by
 *    minutes and the detector already provides cross-evidence when present);
 *  - any measured event stops the estimate and resets the policy;
 *  - while the estimate is active no probes run: the accelerometer is already registered.
 *
 * Threading: sensor callbacks (main) and the service heartbeat (background) both drive this class,
 * so every entry point is synchronized and the two status flags are volatile.
 */
class SilenceWatchdog(
    private val silenceAfterMs: Long = 180_000L,
    private val minProbeIntervalMs: Long = 60_000L,
    private val maxProbeIntervalMs: Long = 10 * 60_000L,
    private val probeRetryMs: Long = 30_000L,
) {
    sealed interface Action {
        data object None : Action
        data object ProbeMotion : Action
        data object StartEstimate : Action
        data object StopEstimate : Action
    }

    @Volatile
    var estimateActive: Boolean = false
        private set

    @Volatile
    var probeInFlight: Boolean = false
        private set

    private var lastMeasuredAt = 0L
    private var nextProbeAt = 0L
    private var probeInterval = minProbeIntervalMs
    private var walkingProbes = 0

    /** Called once when the service starts counting; the clock starts now, not at epoch zero. */
    @Synchronized
    fun onStart(nowMillis: Long) {
        lastMeasuredAt = nowMillis
        nextProbeAt = nowMillis + silenceAfterMs
        probeInterval = minProbeIntervalMs
        walkingProbes = 0
        probeInFlight = false
        estimateActive = false
    }

    /** A real counter or detector event arrived: both measuring sources are alive again. */
    @Synchronized
    fun onMeasuredEvent(nowMillis: Long): Action {
        lastMeasuredAt = nowMillis
        nextProbeAt = nowMillis + silenceAfterMs
        probeInterval = minProbeIntervalMs
        walkingProbes = 0
        probeInFlight = false
        return if (estimateActive) {
            estimateActive = false
            Action.StopEstimate
        } else {
            Action.None
        }
    }

    /** Phones with no counter and no detector estimate continuously; there is nothing to probe. */
    @Synchronized
    fun startContinuousEstimate(nowMillis: Long) {
        lastMeasuredAt = nowMillis
        estimateActive = true
        probeInFlight = false
    }

    /** Called from the heartbeat. Returns [Action.ProbeMotion] at most once per interval. */
    @Synchronized
    fun onTick(nowMillis: Long): Action {
        if (estimateActive || probeInFlight) return Action.None
        if (lastMeasuredAt > 0L && nowMillis - lastMeasuredAt < silenceAfterMs) return Action.None
        if (nowMillis < nextProbeAt) return Action.None
        return Action.ProbeMotion
    }

    /** The service actually started a probe: from now on a result is expected. */
    @Synchronized
    fun probeScheduled(nowMillis: Long) {
        probeInFlight = true
        nextProbeAt = nowMillis + maxProbeIntervalMs
    }

    /** The probe could not be started (no accelerometer, registration refused): retry soon. */
    @Synchronized
    fun probeUnavailable(nowMillis: Long) {
        probeInFlight = false
        probeInterval = minProbeIntervalMs
        nextProbeAt = nowMillis + probeRetryMs
    }

    /** Result of a completed 8 s probe. */
    @Synchronized
    fun onProbeResult(nowMillis: Long, walking: Boolean): Action {
        if (!probeInFlight) return Action.None
        probeInFlight = false
        if (walking) {
            walkingProbes++
            probeInterval = minProbeIntervalMs
            nextProbeAt = nowMillis + maxProbeIntervalMs
            if (!estimateActive) {
                estimateActive = true
                return Action.StartEstimate
            }
            return Action.None
        }
        walkingProbes = 0
        probeInterval = (probeInterval * 2L).coerceAtMost(maxProbeIntervalMs)
        nextProbeAt = nowMillis + probeInterval
        return Action.None
    }

    /** Exposed for Diagnostics/tests only. */
    val consecutiveWalkingProbes: Int get() = walkingProbes
}