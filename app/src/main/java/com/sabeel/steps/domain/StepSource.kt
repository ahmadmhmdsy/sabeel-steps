package com.sabeel.steps.domain

/**
 * Where the step number currently comes from, in descending order of trust.
 *
 * COUNTER        the hardware step counter (cumulative since boot, measured)
 * DETECTOR       the hardware step detector (one event per step, measured)
 * HEALTH_CONNECT an absolute daily total already held by Health Connect (synced, may lag)
 * MOTION_ESTIMATE an accelerometer estimate, only the unconfirmed remainder of a day; always labelled
 * NONE           nothing can count on this phone (still handled honestly by the UI)
 *
 * In the manifold all measured/synced sources accumulate in parallel; this enum only names the
 * strongest contributor for display. It is not used to select or rebase a counting source anymore.
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
