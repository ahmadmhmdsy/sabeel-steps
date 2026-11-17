package com.sabeel.steps.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.Reminders
import com.sabeel.steps.platform.StatusStore
import com.sabeel.steps.platform.StepServiceStatus
import com.sabeel.steps.ui.i18n.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Foreground service (type health) that owns the hardware step counter.
 *
 * Contract with the rest of the app:
 *  - every sensor event is funnelled through StepsRepository.recordRawStep(raw, now, activeMinutes);
 *  - writes are coalesced (<= 1 write / 1.5 s) because the chip emits faster than Room should write,
 *    and the cumulative counter means the newest raw value always describes the whole day;
 *  - [StepCadence] turns the sensor stream into "active minutes" (minutes with >= 60 spm) and is
 *    day-aware, so a midnight tick never stamps yesterday's minutes onto today;
 *  - persisted pipeline state is the single source of truth, so a process death or a reboot never
 *    loses or double counts the day;
 *  - [StatusStore] is kept fresh by a heartbeat so Home can tell the truth about counting.
 *
 * Android 14+ prerequisite: a foreground service of type health may only call startForeground()
 * when a qualifying health permission is granted. Sabeel declares ACTIVITY_RECOGNITION as its only
 * prerequisite, so this service first checks it (cheap pre-flight in [ensureStarted], hard gate in
 * [onCreate]) and stops itself instead of letting the system throw SecurityException at the end of
 * onboarding.
 */
class StepCounterService : Service(), SensorEventListener {

    private enum class Mode { COUNTING, NO_PERMISSION, NO_SENSOR }

    private val app: SabeelApp get() = application as SabeelApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cadence = StepCadence()

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var registered = false
    private var seeded = false
    private var foregroundStarted = false
    private var blocked = false
    private var writeJob: Job? = null
    private var heartbeatJob: Job? = null
    private var mode = Mode.COUNTING

    @Volatile private var latestRaw = Float.NaN
    @Volatile private var goalSteps = 8000
    @Volatile private var lastStatusMarkAt = 0L

    private var lastWrittenRaw = Float.NaN
    private var lastWrittenActive = -1
    private var lastWrittenDate = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Hard gate: never call startForeground(type=health) without its Android 14+ runtime
        // prerequisite. The system throws SecurityException *after* the call, on this service's
        // main thread, so a caller-side catch cannot save the process. Degrade visibly instead.
        if (!Permissions.hasActivityRecognition(this)) {
            block(Mode.NO_PERMISSION)
            return
        }
        sensorManager = runCatching { getSystemService(SensorManager::class.java) }.getOrNull()
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null || !Permissions.hasStepSensor(this)) {
            block(Mode.NO_SENSOR)
            return
        }
        Notifications.ensureChannels(this)
        if (!startInForeground()) {
            // Permission revoked between the guard and the call (API 34+ race): never crash.
            block(Mode.NO_PERMISSION)
            return
        }
        mode = Mode.COUNTING
        markAlive(force = true)

        // Restore persisted state, seed today's active minutes, then start listening.
        scope.launch {
            val today = DateKey.today()
            val seededActive = runCatching {
                app.container.steps.todayFlow().first()?.activeMinutes ?: 0
            }.getOrDefault(0)
            cadence.seed(today, seededActive, System.currentTimeMillis())
            val reconciled = runCatching {
                app.container.steps.reconcile(activeMinutesToday = cadence.activeMinutesFor(today))
            }.getOrNull()
            if (reconciled != null) maybeNotifyGoal(reconciled.steps)
            seeded = true
            withContext(Dispatchers.Main) { registerSensor() }
            startWriter()
            startHeartbeat()
        }
        // Keep the daily goal in memory so the goal notification needs no extra read per event.
        scope.launch {
            app.container.settings.settingsFlow.collect { goalSteps = it.goalSteps }
        }
        // Guarantee a reconcile within 15 minutes even if this service dies and cannot restart.
        runCatching { Reminders.ensureReconcile(this) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (blocked) {
            markBlockedStatus()
            stopSelf()
            return START_NOT_STICKY
        }
        // START_STICKY restart: onCreate already restored everything; re-check the sensor here so a
        // permission granted while the app was closed is picked up without a reboot.
        if (seeded && !registered) registerSensor()
        if (seeded && registered) {
            runCatching { Reminders.ensureReconcile(this) }
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (e.sensor.type != Sensor.TYPE_STEP_COUNTER || e.values.isEmpty()) return
        val raw = e.values[0]
        if (raw.isNaN() || raw.isInfinite()) return
        val now = System.currentTimeMillis()
        cadence.onEvent(raw, now, DateKey.today(now))
        latestRaw = raw
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        unregisterSensor()
        flushPending()
        val wasCounting = mode == Mode.COUNTING
        if (!eraseInProgress && wasCounting) StatusStore.markStopped(this)
        scope.cancel()
        super.onDestroy()
    }

    private fun registerSensor() {
        if (blocked || eraseInProgress) return
        val manager = sensorManager
        val sensor = stepSensor
        if (manager == null || sensor == null) {
            block(Mode.NO_SENSOR)
            return
        }
        if (!Permissions.hasActivityRecognition(this)) {
            block(Mode.NO_PERMISSION)
            return
        }
        if (registered) return
        registered = runCatching {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }.getOrDefault(false)
        if (!registered) {
            block(Mode.NO_SENSOR)
            return
        }
        mode = Mode.COUNTING
        markAlive(force = true)
        updateNotification()
    }

    private fun unregisterSensor() {
        if (!registered) return
        registered = false
        runCatching { sensorManager?.unregisterListener(this) }
    }

    /** Degraded state: mark it (Home renders the matching card) and stop without crashing. */
    private fun block(nextMode: Mode) {
        blocked = true
        mode = nextMode
        markBlockedStatus()
        stopSelf()
    }

    private fun markBlockedStatus() {
        StatusStore.mark(
            this,
            if (mode == Mode.NO_PERMISSION) StepServiceStatus.NO_PERMISSION else StepServiceStatus.NO_SENSOR,
        )
    }

    /**
     * Coalesced writer: the newest cumulative raw value is written at most every 1.5 s. The active
     * minutes are read from the day-aware cadence for *today* so a midnight tick writes 0, not
     * yesterday's total.
     */
    private fun startWriter() {
        if (writeJob?.isActive == true) return
        writeJob = scope.launch {
            while (isActive) {
                delay(WRITE_INTERVAL_MS)
                if (eraseInProgress) continue
                val raw = latestRaw
                if (raw.isNaN()) continue
                val now = System.currentTimeMillis()
                val today = DateKey.today(now)
                val active = cadence.activeMinutesFor(today)
                if (raw == lastWrittenRaw && active == lastWrittenActive && today == lastWrittenDate) continue
                val result = runCatching {
                    app.container.steps.recordRawStep(raw = raw, now = now, activeMinutesToday = active)
                }
                if (result.isFailure) continue
                lastWrittenRaw = raw
                lastWrittenActive = active
                lastWrittenDate = today
                val row = result.getOrNull() ?: continue
                markAlive()
                maybeNotifyGoal(row.steps)
            }
        }
    }

    /** Keeps [StatusStore] fresh while the service lives (including minutes with no steps). */
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (isActive) {
                if (!eraseInProgress) markAlive(force = true)
                delay(STATUS_HEARTBEAT_MS)
            }
        }
    }

    private fun markAlive(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastStatusMarkAt < STATUS_HEARTBEAT_MS) return
        lastStatusMarkAt = now
        val status = when (mode) {
            Mode.COUNTING -> StepServiceStatus.COUNTING
            Mode.NO_PERMISSION -> StepServiceStatus.NO_PERMISSION
            Mode.NO_SENSOR -> StepServiceStatus.NO_SENSOR
        }
        StatusStore.mark(this, status)
    }

    /** Final write at a lifecycle boundary (best effort, bounded so onDestroy cannot hang). */
    private fun flushPending() {
        if (eraseInProgress) return
        val raw = latestRaw
        if (raw.isNaN()) return
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        val active = cadence.finish(now, today)
        val row = runBlocking {
            withTimeoutOrNull(FLUSH_TIMEOUT_MS) {
                runCatching {
                    app.container.steps.recordRawStep(raw = raw, now = now, activeMinutesToday = active)
                }.getOrNull()
            }
        }
        if (row != null) maybeNotifyGoal(row.steps)
    }

    private fun maybeNotifyGoal(steps: Int) {
        if (steps < goalSteps) return
        runCatching { Notifications.notifyGoalReached(this, steps, goalSteps) }
    }

    /** @return false when the platform refused to put the service in the foreground. */
    private fun startInForeground(): Boolean {
        if (foregroundStarted) return true
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        val started = runCatching {
            ServiceCompat.startForeground(this, Notifications.ID_STEP_SERVICE, buildNotification(), type)
        }.isSuccess
        foregroundStarted = started
        return started
    }

    private fun updateNotification() {
        if (!foregroundStarted) return
        Notifications.post(this, Notifications.ID_STEP_SERVICE, buildNotification())
    }

    private fun buildNotification(): Notification {
        val localized = AppLocale.wrap(this)
        val (title, text) = when (mode) {
            Mode.COUNTING -> localized.getString(R.string.sensor_notif_counting_title) to
                localized.getString(R.string.sensor_notif_counting_text)
            Mode.NO_PERMISSION -> localized.getString(R.string.sensor_notif_permission_title) to
                localized.getString(R.string.sensor_notif_permission_text)
            Mode.NO_SENSOR -> localized.getString(R.string.sensor_notif_no_sensor_title) to
                localized.getString(R.string.sensor_notif_no_sensor_text)
        }
        return Notifications.stepServiceNotification(this, title, text)
    }

    companion object {
        private const val WRITE_INTERVAL_MS = 1_500L
        private const val FLUSH_TIMEOUT_MS = 800L
        private const val STATUS_HEARTBEAT_MS = 30_000L

        /**
         * True while delete-all is suppressing service writes. Volatile because a service instance
         * dying mid-erase must not let the next instance resurrect a row before the wipe finishes.
         */
        @Volatile private var eraseInProgress = false

        /**
         * Starts the step service only when it can legally and usefully run.
         *
         * @return false when ACTIVITY_RECOGNITION or the step sensor is missing (the status store
         * already holds the reason), or when the platform refuses the start. Never throws: callers
         * include onboarding, app-open and boot entry points where a crash is unacceptable.
         */
        fun ensureStarted(context: Context): Boolean {
            val appContext = context.applicationContext
            if (!Permissions.hasActivityRecognition(appContext)) {
                StatusStore.mark(appContext, StepServiceStatus.NO_PERMISSION)
                return false
            }
            if (!Permissions.hasStepSensor(appContext)) {
                StatusStore.mark(appContext, StepServiceStatus.NO_SENSOR)
                return false
            }
            return runCatching {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, StepCounterService::class.java),
                )
                true
            }.getOrDefault(false)
        }

        /** Suppresses every pending/queued service write while delete-all wipes the database. */
        fun beginErase() {
            eraseInProgress = true
        }

        /** Re-enables writes after delete-all has finished. */
        fun endErase() {
            eraseInProgress = false
        }

        /** Stops the service for delete-all; safe when it is not running. */
        fun stopForErase(context: Context) {
            val appContext = context.applicationContext
            runCatching { appContext.stopService(Intent(appContext, StepCounterService::class.java)) }
        }
    }
}
