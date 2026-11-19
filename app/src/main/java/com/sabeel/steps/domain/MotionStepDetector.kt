package com.sabeel.steps.domain

import kotlin.math.exp

/**
 * A conservative, pure-JVM step detector for devices without a hardware step counter/detector.
 *
 * It is deliberately **not** a pedometer: it only counts a rhythm that looks like walking, and it
 * stays quiet for isolated jolts. The output is an estimate and the UI must say so.
 *
 * Signal path:
 *  - one-pole low-pass over the acceleration magnitude ([smoothingTauMs]) removes sensor noise;
 *  - a slower baseline ([baselineTauMs]) tracks gravity and orientation so the difference is the
 *    walking oscillation, not the phone's tilt;
 *  - peaks must cross [threshold] and fall below [hysteresis] to be counted;
 *  - a peak only counts when it is [minStepIntervalMs]..[maxStepIntervalMs] after the previous one
 *    (a walking cadence, not typing or shaking);
 *  - [burstSize] qualified peaks inside [burstWindowMs] are needed before any step is emitted; the
 *    first burst is flushed at once so a short walk is not lost;
 *  - after [stopAfterMs] without a peak the detector leaves walking mode.
 *
 * Smoothing coefficients are derived from elapsed time, not from a fixed per-sample alpha, so the
 * same constants behave the same at 10 Hz and 50 Hz. Synthetic tests cover both.
 */
class MotionStepDetector(
    private val threshold: Float = 1.2f,
    private val hysteresis: Float = 0.15f,
    private val minStepIntervalMs: Long = 250L,
    private val maxStepIntervalMs: Long = 2_000L,
    private val burstSize: Int = 3,
    private val burstWindowMs: Long = 3_000L,
    private val stopAfterMs: Long = 2_500L,
    private val smoothingTauMs: Double = 90.0,
    private val baselineTauMs: Double = 1_000.0,
    private val gravity: Float = 9.81f,
) {
    private var smoothed = gravity
    private var baseline = gravity
    private var lastSampleAt = 0L
    private var aboveThreshold = false
    private var lastPeakAt: Long? = null
    private var candidatePeaks = 0
    private var candidateStart = 0L
    private var walking = false

    /** True while a walking rhythm is being confirmed; exposed for the motion probe, not for UI. */
    val isWalking: Boolean get() = walking

    /**
     * Feeds one acceleration-magnitude sample.
     * @return newly confirmed steps: 0, the flushed first burst, or 1 for later peaks.
     */
    fun onSample(magnitude: Float, nowMillis: Long): Int {
        if (!magnitude.isFinite() || nowMillis <= 0L) return 0

        if (lastSampleAt == 0L) {
            smoothed = magnitude
            baseline = magnitude
            lastSampleAt = nowMillis
            return 0
        }

        val dtMs = (nowMillis - lastSampleAt).coerceIn(MIN_DT_MS, MAX_DT_MS)
        val smoothingAlpha = 1.0 - exp(-dtMs / smoothingTauMs)
        val baselineAlpha = 1.0 - exp(-dtMs / baselineTauMs)
        smoothed += (smoothingAlpha * (magnitude - smoothed)).toFloat()
        baseline += (baselineAlpha * (magnitude - baseline)).toFloat()
        lastSampleAt = nowMillis

        val dynamic = smoothed - baseline
        var newSteps = 0

        if (!aboveThreshold && dynamic > threshold) {
            aboveThreshold = true
        } else if (aboveThreshold && dynamic < hysteresis) {
            aboveThreshold = false
            val gap = lastPeakAt?.let { nowMillis - it }
            val cadenceOk = lastPeakAt == null || (gap != null && gap in minStepIntervalMs..maxStepIntervalMs)
            if (cadenceOk) {
                lastPeakAt = nowMillis
                if (walking) {
                    newSteps = 1
                } else {
                    if (nowMillis - candidateStart > burstWindowMs) {
                        candidatePeaks = 0
                        candidateStart = nowMillis
                    }
                    candidatePeaks++
                    if (candidatePeaks >= burstSize) {
                        walking = true
                        newSteps = candidatePeaks
                        candidatePeaks = 0
                    }
                }
            }
        }

        if (walking && lastPeakAt != null && nowMillis - lastPeakAt!! > stopAfterMs) {
            walking = false
            candidatePeaks = 0
        }
        return newSteps
    }

    /** Clears every filter and rhythm state; call when the sensor is re-registered or a new day starts. */
    fun reset() {
        smoothed = gravity
        baseline = gravity
        lastSampleAt = 0L
        aboveThreshold = false
        lastPeakAt = null
        candidatePeaks = 0
        candidateStart = 0L
        walking = false
    }

    private companion object {
        const val MIN_DT_MS = 10L
        const val MAX_DT_MS = 250L
    }
}
