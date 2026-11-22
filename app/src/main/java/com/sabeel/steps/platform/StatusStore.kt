package com.sabeel.steps.platform

import android.content.Context
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the step-counting layer is actually doing right now. Consumed by Home (status card) and by
 * the service/worker code that has to degrade gracefully instead of crashing.
 *
 * COUNTING          the foreground service is alive and a source is registered
 * NO_PERMISSION     ACTIVITY_RECOGNITION is not granted, so the health FGS must not run
 * NO_SENSOR         the device has no counter, no detector and no accelerometer (nothing can count)
 * STOPPED           the service is not running (clean internal stop, or stale heartbeat with no
 *                   permission to count)
 * OFF_BY_USER       the user deliberately turned counting off (CountingStore.enabled == false)
 * STOPPED_BY_SYSTEM counting is meant to run, the permission exists, but the heartbeat went stale:
 *                   Android or the phone maker stopped it (the state Home must report as
 *                   "Android stopped counting — tap to restart", never as "you stopped it")
 */
enum class StepServiceStatus { COUNTING, NO_PERMISSION, NO_SENSOR, STOPPED, OFF_BY_USER, STOPPED_BY_SYSTEM }

/** The live source behind the number, how fresh it is, and how often it had to be switched. */
data class StepSourceStatus(
    val source: StepSource = StepSource.NONE,
    val lastEventAt: Long = 0L,
    val switches: Int = 0,
)

/**
 * One boot/package-replace attempt, kept as the evidence the owner can share from Diagnostics.
 *
 * Written *before* the start attempt and completed afterwards: on a device where the OEM silently
 * blocks the service, this record is the only way to tell "the receiver never ran" (no attempt) from
 * "it ran and the start was refused / died" (attempt with a result and nullable [aliveAfterCheck]).
 */
data class BootAttempt(
    /** Wall-clock time the broadcast was received. */
    val at: Long = 0L,
    /** BOOT_COMPLETED or MY_PACKAGE_REPLACED. */
    val action: String = "",
    /** CountingStore.enabled at boot time. */
    val enabled: Boolean = false,
    /** Onboarding completion as persisted in the settings DataStore. */
    val onboarded: Boolean = false,
    /** ACTIVITY_RECOGNITION grant at boot time. */
    val hasActivityRecognition: Boolean = false,
    /** SensorManager reports a counter, detector or accelerometer. */
    val hasSensor: Boolean = false,
    /** Health Connect provider present and step-read granted. */
    val healthConnectReady: Boolean = false,
    /** StepCounterService.StartResult.name, or PENDING / SKIPPED_NOT_ONBOARDED. */
    val result: String = "",
    /** Simple class name of the throwable that broke the attempt, or null. */
    val exception: String? = null,
    /** Result of the ~1 minute self-check; null while the check has not run yet. */
    val aliveAfterCheck: Boolean? = null,
)

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
 * reports [StepServiceStatus.STOPPED_BY_SYSTEM] (intent on, permission granted) or
 * [StepServiceStatus.STOPPED] (nothing could count anyway) instead of a lie. A user-off intent is
 * read from [CountingStore] first and reported as [StepServiceStatus.OFF_BY_USER] regardless of age.
 */
object StatusStore {

    private const val PREFS = "sabeel_service_status"
    private const val KEY_MODE = "mode"
    private const val KEY_LAST_ALIVE = "last_alive_ms"
    private const val KEY_SOURCE = "source"
    private const val KEY_SOURCE_EVENT = "source_last_event_ms"
    private const val KEY_SOURCE_SWITCHES = "source_switches"
    private const val KEY_SOURCE_DAY = "source_day"
    private const val KEY_STORAGE_FAULT = "storage_fault"
    private const val KEY_STORAGE_FAULT_AT = "storage_fault_at"
    private const val KEY_BOOT_ATTEMPTS = "boot_attempts"
    private const val KEY_BOOT_ALIVE = "boot_alive"
    private const val KEY_BOOT_ALIVE_AT = "boot_alive_at"
    private const val KEY_LAST_START_RESULT = "last_start_result"
    private const val KEY_LAST_START_AT = "last_start_at"

    /** How long a mark stays trustworthy without a heartbeat. */
    const val STALE_AFTER_MS = 120_000L

    /** Diagnostics keeps a short history; the newest attempt is the last element. */
    const val MAX_BOOT_ATTEMPTS = 10

    fun mark(context: Context, status: StepServiceStatus) {
        prefs(context).edit()
            .putString(KEY_MODE, status.name)
            .putLong(KEY_LAST_ALIVE, System.currentTimeMillis())
            .apply()
    }

    fun markStopped(context: Context) = mark(context, StepServiceStatus.STOPPED)

    /**
     * Reads the live status. COUNTING/NO_PERMISSION/NO_SENSOR are trusted only while fresh; a stale
     * heartbeat while the user's intent is on is reported as STOPPED_BY_SYSTEM (not STOPPED), so the
     * UI can distinguish "the system stopped it" from "you stopped it".
     */
    fun read(context: Context): StepServiceStatus {
        val countingEnabled = runCatching { CountingStore.get(context).current().enabled }
            .getOrDefault(true)
        if (!countingEnabled) return StepServiceStatus.OFF_BY_USER
        val prefs = prefs(context)
        val lastAlive = prefs.getLong(KEY_LAST_ALIVE, 0L)
        val fresh = lastAlive > 0L && System.currentTimeMillis() - lastAlive <= STALE_AFTER_MS
        val mode = prefs.getString(KEY_MODE, null)
            ?.let { name -> runCatching { StepServiceStatus.valueOf(name) }.getOrNull() }
        if (mode != null && fresh && mode != StepServiceStatus.OFF_BY_USER) return mode
        return staleStatus(context)
    }

    /** Raw heartbeat wall-clock; 0 when never marked. Deliberately not staleness-masked. */
    fun readLastAliveAt(context: Context): Long = prefs(context).getLong(KEY_LAST_ALIVE, 0L)

    /** Persists the active source. The switch counter resets at the local day boundary. */
    fun markSource(context: Context, status: StepSourceStatus) {
        val today = DateKey.today()
        prefs(context).edit()
            .putString(KEY_SOURCE, status.source.name)
            .putLong(KEY_SOURCE_EVENT, status.lastEventAt)
            .putInt(KEY_SOURCE_SWITCHES, status.switches)
            .putString(KEY_SOURCE_DAY, today)
            .apply()
    }

    /** Safe before the first mark: returns [StepSource.NONE] / "never" rather than guessing. */
    fun readSource(context: Context): StepSourceStatus {
        val prefs = prefs(context)
        val source = prefs.getString(KEY_SOURCE, null)
            ?.let { runCatching { StepSource.valueOf(it) }.getOrNull() }
            ?: StepSource.NONE
        val today = DateKey.today()
        val switches = if (prefs.getString(KEY_SOURCE_DAY, null) == today) {
            prefs.getInt(KEY_SOURCE_SWITCHES, 0)
        } else {
            0
        }
        return StepSourceStatus(
            source = source,
            lastEventAt = prefs.getLong(KEY_SOURCE_EVENT, 0L),
            switches = switches,
        )
    }

    /**
     * A failed persistence write is data loss in progress. The service sets this on the first failed
     * write and clears it on the next success, so Diagnostics can show "storage failing" instead of
     * the app pretending the day is saved.
     */
    fun markStorageFault(context: Context, failing: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_STORAGE_FAULT, failing)
            .putLong(KEY_STORAGE_FAULT_AT, System.currentTimeMillis())
            .apply()
    }

    /** Null when writes are healthy, otherwise the wall-clock time the fault was last observed. */
    fun readStorageFault(context: Context): Long? {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_STORAGE_FAULT, false)) return null
        return prefs.getLong(KEY_STORAGE_FAULT_AT, 0L)
    }

    /** The start outcome of the last [com.sabeel.steps.service.StepCounterService.ensureStarted]. */
    fun recordStartResult(context: Context, result: String) {
        prefs(context).edit()
            .putString(KEY_LAST_START_RESULT, result)
            .putLong(KEY_LAST_START_AT, System.currentTimeMillis())
            .apply()
    }

    /** Newest-last list, bounded to [MAX_BOOT_ATTEMPTS]; empty when nothing was ever recorded. */
    fun readBootAttempts(context: Context): List<BootAttempt> {
        val raw = prefs(context).getString(KEY_BOOT_ATTEMPTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(
                        BootAttempt(
                            at = obj.optLong("at", 0L),
                            action = obj.optString("action", ""),
                            enabled = obj.optBoolean("enabled", false),
                            onboarded = obj.optBoolean("onboarded", false),
                            hasActivityRecognition = obj.optBoolean("ar", false),
                            hasSensor = obj.optBoolean("sensor", false),
                            healthConnectReady = obj.optBoolean("hc", false),
                            result = obj.optString("result", ""),
                            exception = if (obj.isNull("exception")) {
                                null
                            } else {
                                obj.optString("exception", "").ifEmpty { null }
                            },
                            aliveAfterCheck = if (obj.isNull("alive")) null else obj.optBoolean("alive"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** The UI-facing alias; the same bounded newest-last list. */
    fun listBootAttempts(context: Context): List<BootAttempt> = readBootAttempts(context)

    /** Appends one attempt, keeping only the newest [MAX_BOOT_ATTEMPTS]. */
    fun recordBootAttempt(context: Context, attempt: BootAttempt) {
        val attempts = readBootAttempts(context).toMutableList()
        attempts.add(attempt)
        while (attempts.size > MAX_BOOT_ATTEMPTS) attempts.removeAt(0)
        // Evidence must survive an immediate process kill: commit, do not apply.
        writeAttempts(context, attempts)
    }

    /** Completes the newest attempt with its start result and Throwable class (if any). */
    fun updateLastBootAttempt(context: Context, result: String, exception: String?) {
        val attempts = readBootAttempts(context).toMutableList()
        if (attempts.isEmpty()) return
        attempts[attempts.lastIndex] = attempts.last().copy(result = result, exception = exception)
        writeAttempts(context, attempts)
    }

    /** Records the ~1 minute post-boot self-check on the newest attempt. */
    fun markBootCheck(context: Context, alive: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_BOOT_ALIVE, alive)
            .putLong(KEY_BOOT_ALIVE_AT, System.currentTimeMillis())
            .apply()
        val attempts = readBootAttempts(context).toMutableList()
        if (attempts.isEmpty()) return
        attempts[attempts.lastIndex] = attempts.last().copy(aliveAfterCheck = alive)
        writeAttempts(context, attempts)
    }

    /** Delete-all: the whole status file, so the next launch starts from a clean slate. */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Encodes the bounded attempt list; commit() so the evidence survives an immediate kill. */
    private fun writeAttempts(context: Context, attempts: List<BootAttempt>) {
        val array = JSONArray()
        attempts.forEach { entry ->
            array.put(
                JSONObject()
                    .put("at", entry.at)
                    .put("action", entry.action)
                    .put("enabled", entry.enabled)
                    .put("onboarded", entry.onboarded)
                    .put("ar", entry.hasActivityRecognition)
                    .put("sensor", entry.hasSensor)
                    .put("hc", entry.healthConnectReady)
                    .put("result", entry.result)
                    .put("exception", entry.exception ?: "")
                    .put("alive", entry.aliveAfterCheck ?: JSONObject.NULL),
            )
        }
        prefs(context).edit().putString(KEY_BOOT_ATTEMPTS, array.toString()).commit()
    }

    /** True when the heartbeat is old enough that the service is not trusted to be alive. */
    private fun staleStatus(context: Context): StepServiceStatus {
        val hasPermission = runCatching { Permissions.hasActivityRecognition(context) }.getOrDefault(false)
        return if (hasPermission) StepServiceStatus.STOPPED_BY_SYSTEM else StepServiceStatus.STOPPED
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
