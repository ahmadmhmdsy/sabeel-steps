package com.sabeel.steps.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sabeel.steps.R
import com.sabeel.steps.domain.SourceAvailability
import com.sabeel.steps.domain.StepSource

/**
 * The real, SensorManager-backed answer to "what can this phone count with?".
 *
 * The old gate checked only the FEATURE_SENSOR_STEP_COUNTER package feature, which is a
 * manufacturer claim about the hardware, not proof that the sensor is present and usable. Asking
 * the SensorManager for the default sensors is the same check the registration itself will face.
 */
object StepSensors {

    fun availability(context: Context): SourceAvailability {
        val manager = runCatching {
            context.applicationContext.getSystemService(SensorManager::class.java)
        }.getOrNull() ?: return SourceAvailability(counter = false, detector = false, accelerometer = false)
        return SourceAvailability(
            counter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null,
            detector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null,
            accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
        )
    }

    fun hasAny(context: Context): Boolean = availability(context).any

    fun labelRes(source: StepSource): Int = when (source) {
        StepSource.COUNTER -> R.string.diagnostics_source_counter
        StepSource.DETECTOR -> R.string.diagnostics_source_detector
        StepSource.MOTION_ESTIMATE -> R.string.diagnostics_source_estimate
        StepSource.NONE -> R.string.diagnostics_source_none
    }
}
