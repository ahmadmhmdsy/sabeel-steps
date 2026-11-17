package com.sabeel.steps.platform

import android.content.Context

/**
 * What the step-counting layer is actually doing right now. Consumed by Home (status card) and by
 * the service/worker code that has to degrade gracefully instead of crashing.
 *
 * COUNTING      the foreground service is alive and the sensor is registered
 * NO_PERMISSION ACTIVITY_RECOGNITION is not granted, so the health FGS must not run
 * NO_SENSOR     the device has no TYPE_STEP_COUNTER (or registration failed)
 * STOPPED       the service is not running (or its heartbeat went stale)
 */
enum class StepServiceStatus { COUNTING, NO_PERMISSION, NO_SENSOR, STOPPED }

/**
 * Single source of truth for the step service's live status, written only by the service (and by
 * [com.sabeel.steps.service.StepCounterService.ensureStarted] when it refuses to start).
 *
 * Implemented as a tiny SharedPreferences file on purpose: it is read synchronously by the UI
 * status poll and by the app shell on every foreground entry, and it must survive process death
 * without touching DataStore (which cannot be read synchronously). Nothing here leaves the device.
 *
 * A COUNTING/NO_SENSOR/NO_PERMISSION mark is only trusted for [STALE_AFTER_MS]; when the service
 * dies without a clean stop (process kill, OEM cleaner), the heartbeat stops and the next read
 * reports [StepServiceStatus.STOPPED] instead of a lie.
 */
object StatusStore {

    private const val PREFS = "sabeel_service_status"
    private const val KEY_MODE = "mode"
    private const val KEY_LAST_ALIVE = "last_alive_ms"

    /** How long a mark stays trustworthy without a heartbeat. */
    const val STALE_AFTER_MS = 120_000L

    fun mark(context: Context, status: StepServiceStatus) {
        prefs(context).edit()
            .putString(KEY_MODE, status.name)
            .putLong(KEY_LAST_ALIVE, System.currentTimeMillis())
            .apply()
    }

    fun markStopped(context: Context) = mark(context, StepServiceStatus.STOPPED)

    fun read(context: Context): StepServiceStatus {
        val prefs = prefs(context)
        val mode = prefs.getString(KEY_MODE, null) ?: return StepServiceStatus.STOPPED
        val lastAlive = prefs.getLong(KEY_LAST_ALIVE, 0L)
        if (System.currentTimeMillis() - lastAlive > STALE_AFTER_MS) return StepServiceStatus.STOPPED
        return runCatching { StepServiceStatus.valueOf(mode) }.getOrDefault(StepServiceStatus.STOPPED)
    }

    /** Delete-all: the whole status file, so the next launch starts from a clean slate. */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
