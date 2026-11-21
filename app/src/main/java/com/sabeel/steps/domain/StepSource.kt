package com.sabeel.steps.domain

/**
 * Where the step number currently comes from, in descending order of trust.
 *
 * COUNTER        the hardware step counter (cumulative since boot, measured)
 * DETECTOR       the hardware step detector (one event per step, measured)
 * HEALTH_CONNECT an absolute daily total already held by Health Connect (synced, may lag)
 * MOTION_ESTIMATE an accelerometer estimate, only when no measured source can work; always labelled
 * NONE           nothing can count on this phone (still handled honestly by the UI)
 *
 * The pipeline records which source produced the current total so a switch can rebase instead of
 * treating a different sensor's raw scale as the same counter.
 */
enum class StepSource {
    COUNTER,
    DETECTOR,
    HEALTH_CONNECT,
    MOTION_ESTIMATE,
    NONE,
    ;

    /** Measured sources may be presented as facts; an estimate must always be marked as one. */
    val measured: Boolean
        get() = this == COUNTER || this == DETECTOR

    /** Synced data comes from another app or a watch; it is honest but not live. */
    val synced: Boolean
        get() = this == HEALTH_CONNECT

    /** Values that are not a live hardware measurement and must never masquerade as one. */
    val approximate: Boolean
        get() = this == MOTION_ESTIMATE || this == HEALTH_CONNECT
}

/** What the SensorManager actually reports on this device (never inferred from a feature flag). */
data class SourceAvailability(
    val counter: Boolean,
    val detector: Boolean,
    val accelerometer: Boolean,
    /**
     * True when Health Connect is installed/available, the user granted the step read permission,
     * and this phone has no hardware step counter or detector (the case the tier exists for).
     */
    val healthConnect: Boolean = false,
) {
    val any: Boolean get() = counter || detector || accelerometer || healthConnect

    /** True when no on-device step measurement is possible, so synced/estimated sources are the only options. */
    val noMeasuredStepSensor: Boolean get() = !counter && !detector
}

/**
 * The single source-priority ladder. Pure so the ordering is testable without Android.
 *
 * A counter beats a detector because it is cumulative and survives a stopped service; a detector
 * beats an estimate because it is still a measured per-step event. The estimate is the last resort
 * and is never selected while a measured sensor is available.
 */
object StepSourcePolicy {

    fun choose(available: SourceAvailability): StepSource = when {
        available.counter -> StepSource.COUNTER
        available.detector -> StepSource.DETECTOR
        // Synced data is a real measurement made by another device, so it outranks an estimate.
        available.healthConnect -> StepSource.HEALTH_CONNECT
        available.accelerometer -> StepSource.MOTION_ESTIMATE
        else -> StepSource.NONE
    }

    /** The next measured fallback after [current], or null when there is none. */
    fun nextMeasured(current: StepSource, available: SourceAvailability): StepSource? = when (current) {
        StepSource.COUNTER -> if (available.detector) StepSource.DETECTOR else null
        else -> null
    }

    /** Best measured source that is currently available, or null when only an estimate/none is left. */
    fun bestMeasured(available: SourceAvailability): StepSource? = when {
        available.counter -> StepSource.COUNTER
        available.detector -> StepSource.DETECTOR
        else -> null
    }

    /** The estimate tier exists only when an accelerometer exists. */
    fun canEstimate(available: SourceAvailability): Boolean = available.accelerometer

    /**
     * What to try after [current] has been silent for too long. The hardware counter is not offered
     * again here: it stays registered and recovers on its own event. After a silent detector, the
     * labelled estimate is the only remaining tier.
     */
    fun fallbackTarget(current: StepSource, available: SourceAvailability): StepSource? = when (current) {
        StepSource.COUNTER -> when {
            available.detector -> StepSource.DETECTOR
            available.healthConnect -> StepSource.HEALTH_CONNECT
            available.accelerometer -> StepSource.MOTION_ESTIMATE
            else -> null
        }
        StepSource.DETECTOR -> when {
            available.healthConnect -> StepSource.HEALTH_CONNECT
            available.accelerometer -> StepSource.MOTION_ESTIMATE
            else -> null
        }
        StepSource.HEALTH_CONNECT -> if (available.accelerometer) StepSource.MOTION_ESTIMATE else null
        else -> null
    }
}
