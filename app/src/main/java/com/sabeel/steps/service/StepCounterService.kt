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
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
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
 *  - StepCadence turns the sensor stream into "active minutes" (minutes with >= 60 spm);
 *  - persisted pipeline state is the single source of truth, so a process death or a reboot never
 *    loses or double counts the day.
 */
class StepCounterService : Service(), SensorEventListener {

    private enum class Mode { COUNTING, NO_PERMISSION, NO_SENSOR }

    private val app: SabeelApp get() = application as SabeelApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cadence = StepCadence()

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var registered = false
    private var seeded = false
    private var foregroundStarted = false
    private var writeJob: Job? = null
    private var mode = Mode.COUNTING

    @Volatile private var latestRaw = Float.NaN
    @Volatile private var pendingActiveMinutes = 0
    @Volatile private var goalSteps = 8000

    private var lastWrittenRaw = Float.NaN
    private var lastWrittenActive = -1
    private var lastWrittenDate = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        mode = if (Permissions.hasActivityRecognition(this)) Mode.COUNTING else Mode.NO_PERMISSION
        startInForeground()

        sensorManager = getSystemService(SensorManager::class.java)
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            mode = Mode.NO_SENSOR
            updateNotification()
        }

        // Restore persisted state, seed today's active minutes from the DB, then start listening.
        scope.launch {
            val today = DateKey.today()
            val seededActive = runCatching {
                app.container.database.dailyStepsDao().getSince(today)
                    .firstOrNull { it.date == today }?.activeMinutes ?: 0
            }.getOrDefault(0)
            cadence.seed(today, seededActive, System.currentTimeMillis())
            pendingActiveMinutes = cadence.activeMinutesToday
            val reconciled = runCatching {
                app.container.steps.reconcile(activeMinutesToday = cadence.activeMinutesToday)
            }.getOrNull()
            if (reconciled != null) maybeNotifyGoal(reconciled.steps)
            seeded = true
            withContext(Dispatchers.Main) { registerSensor() }
            startWriter()
        }
        // Keep the daily goal in memory so the goal notification needs no extra read per event.
        scope.launch {
            app.container.settings.settingsFlow.collect { goalSteps = it.goalSteps }
        }
        // Guarantee a reconcile within 15 minutes even if this service dies and cannot restart.
        runCatching { Reminders.ensureReconcile(this) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        pendingActiveMinutes = cadence.onEvent(raw, now, DateKey.today(now))
        latestRaw = raw
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        unregisterSensor()
        flushPending()
        scope.cancel()
        super.onDestroy()
    }

    private fun registerSensor() {
        val sensor = stepSensor
        if (sensor == null) {
            mode = Mode.NO_SENSOR
            updateNotification()
            return
        }
        if (!Permissions.hasActivityRecognition(this)) {
            mode = Mode.NO_PERMISSION
            updateNotification()
            return
        }
        if (registered) return
        registered = runCatching {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }.getOrDefault(false)
        mode = if (registered) Mode.COUNTING else Mode.NO_SENSOR
        updateNotification()
    }

    private fun unregisterSensor() {
        if (!registered) return
        registered = false
        runCatching { sensorManager.unregisterListener(this) }
    }

    /** Coalesced writer: the newest cumulative raw value is written at most every 1.5 s. */
    private fun startWriter() {
        if (writeJob?.isActive == true) return
        writeJob = scope.launch {
            while (isActive) {
                delay(WRITE_INTERVAL_MS)
                val raw = latestRaw
                if (raw.isNaN()) continue
                val active = pendingActiveMinutes
                val today = DateKey.today()
                if (raw == lastWrittenRaw && active == lastWrittenActive && today == lastWrittenDate) continue
                val row = runCatching {
                    app.container.steps.recordRawStep(
                        raw = raw,
                        now = System.currentTimeMillis(),
                        activeMinutesToday = active,
                    )
                }.getOrNull()
                lastWrittenRaw = raw
                lastWrittenActive = active
                lastWrittenDate = today
                if (row != null) maybeNotifyGoal(row.steps)
            }
        }
    }

    /** Final write at a lifecycle boundary (best effort, bounded so onDestroy cannot hang). */
    private fun flushPending() {
        val raw = latestRaw
        if (raw.isNaN()) return
        val active = pendingActiveMinutes
        val row = runBlocking {
            withTimeoutOrNull(FLUSH_TIMEOUT_MS) {
                runCatching {
                    app.container.steps.recordRawStep(raw = raw, now = System.currentTimeMillis(), activeMinutesToday = active)
                }.getOrNull()
            }
        }
        if (row != null) maybeNotifyGoal(row.steps)
    }

    private fun maybeNotifyGoal(steps: Int) {
        if (steps < goalSteps) return
        runCatching { Notifications.notifyGoalReached(this, steps, goalSteps) }
    }

    private fun startInForeground() {
        if (foregroundStarted) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.ID_STEP_SERVICE, buildNotification(), type)
        foregroundStarted = true
    }

    private fun updateNotification() {
        if (!foregroundStarted) return
        runCatching {
            NotificationManagerCompat.from(this).notify(Notifications.ID_STEP_SERVICE, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        val (title, text) = when (mode) {
            Mode.COUNTING -> getString(R.string.sensor_notif_counting_title) to
                getString(R.string.sensor_notif_counting_text)
            Mode.NO_PERMISSION -> getString(R.string.sensor_notif_permission_title) to
                getString(R.string.sensor_notif_permission_text)
            Mode.NO_SENSOR -> getString(R.string.sensor_notif_no_sensor_title) to
                getString(R.string.sensor_notif_no_sensor_text)
        }
        return Notifications.stepServiceNotification(this, title, text)
    }

    companion object {
        private const val WRITE_INTERVAL_MS = 1_500L
        private const val FLUSH_TIMEOUT_MS = 800L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, StepCounterService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepCounterService::class.java))
        }
    }
}
