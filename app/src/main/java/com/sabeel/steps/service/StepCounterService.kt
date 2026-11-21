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
import com.sabeel.steps.domain.MotionStepDetector
import com.sabeel.steps.domain.SourceAvailability
import com.sabeel.steps.domain.StepSource
import com.sabeel.steps.domain.StepSourceHealth
import com.sabeel.steps.domain.StepSourcePolicy
import com.sabeel.steps.platform.HealthConnect
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.Reminders
import com.sabeel.steps.platform.StepSensors
import com.sabeel.steps.platform.StatusStore
import com.sabeel.steps.platform.StepServiceStatus
import com.sabeel.steps.platform.StepSourceStatus
import com.sabeel.steps.ui.i18n.AppLocale
import kotlin.math.sqrt
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
 * Foreground service (type health) that owns the step source.
 *
 * Contract with the rest of the app:
 *  - one source is active at a time, chosen from the [StepSourcePolicy] ladder (hardware counter ->
 *    hardware detector -> labelled accelerometer estimate) from the sensors the SensorManager
 *    actually reports;
 *  - the hardware counter stays registered even while a fallback is active, so a counter event
 *    recovers the primary source immediately instead of waiting for a restart;
 *  - every event is funnelled through [StepsRepository.recordRawStep] with the active [StepSource],
 *    and the pure [StepPipeline] rebases on any switch (never double counts the gap);
 *  - writes are coalesced (<= 1 write / 1.5 s) because the chip emits faster than Room should write;
 *  - [StepCadence] turns the sensor stream into "active minutes" and is day-aware;
 *  - persisted pipeline state is the single source of truth, so process death or a reboot never
 *    loses or double counts the day;
 *  - [StatusStore] carries the live source, its last event age and switch count for Home and
 *    Diagnostics; a heartbeat keeps the "counting" mark fresh.
 *
 * Silence watchdog: a measured source that has not reported for [STALL_AFTER_MS] is probed with a
 * 5 s accelerometer window. Two consecutive walking probes switch to the next tier; a still phone
 * is never degraded. Only physically impossible deltas are clamped (pipeline), normal walking is
 * never throttled.
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
    private val health = StepSourceHealth()

    private var sensorManager: SensorManager? = null
    private var counterSensor: Sensor? = null
    private var detectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var availability = SourceAvailability(counter = false, detector = false, accelerometer = false)

    private var counterRegistered = false
    private var detectorRegistered = false
    private var accelerometerRegistered = false
    private var probeActive = false
    private var probeSteps = 0
    private var probeDetector: MotionStepDetector? = null
    private var probeJob: Job? = null
    private val estimateDetector = MotionStepDetector()

    private var seeded = false
    private var foregroundStarted = false
    private var blocked = false
    private var writeJob: Job? = null
    private var heartbeatJob: Job? = null
    private var hcPollJob: Job? = null

    /** Provider usable + grant + no hardware step sensor; polls keep watching even while estimating. */
    private var healthConnectReady = false

    /** Last absolute total seen from Health Connect (-1 = never), used to detect real progress. */
    private var hcLastTotal = -1
    private var mode = Mode.COUNTING

    @Volatile private var activeSource = StepSource.NONE

    /** The cumulative raw value of the active source, paired with the source to avoid mixed reads. */
    private val rawLock = Any()
    private var latestRaw = Float.NaN
    private var latestSource = StepSource.NONE

    private var detectorCumulative = 0f
    private var estimateCumulative = 0f

    /** Wall-clock time of the last event from the *active* source (0 = none yet). */
    @Volatile private var lastEventAt = 0L

    @Volatile private var goalSteps = 8000
    @Volatile private var lastStatusMarkAt = 0L
    @Volatile private var lastSourceMarkAt = 0L
    private var switches = 0
    private var statusDay = ""

    private var lastWrittenRaw = Float.NaN
    private var lastWrittenActive = -1
    private var lastWrittenDate = ""
    private var lastWrittenSource = StepSource.NONE

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
        val manager = sensorManager
        if (manager == null) {
            block(Mode.NO_SENSOR)
            return
        }
        counterSensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        detectorSensor = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometerSensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // Health Connect is only a counting source when no hardware step sensor exists; with a real
        // counter/detector it would mix two origins and risk double counting for no benefit.
        healthConnectReady = runCatching { HealthConnect.isReady(this) }.getOrDefault(false) &&
            counterSensor == null && detectorSensor == null
        availability = SourceAvailability(
            counter = counterSensor != null,
            detector = detectorSensor != null,
            accelerometer = accelerometerSensor != null,
            // Prefer a live estimate at startup when the accelerometer exists: Health Connect can be
            // silent for hours, and a zero synced total must not stop real phone counting. The first
            // poll that reports steps above the current total promotes Health Connect.
            healthConnect = healthConnectReady && accelerometerSensor == null,
        )
        if (!availability.any) {
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
            withContext(Dispatchers.Main) { startPrimarySource() }
            startWriter()
            startHeartbeat()
            startHealthConnectPolling()
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
        // START_STICKY restart: onCreate already restored everything; re-check the source here so a
        // permission granted while the app was closed is picked up without a reboot.
        if (seeded && activeSource == StepSource.NONE) startPrimarySource()
        if (seeded && activeSource != StepSource.NONE) {
            runCatching { Reminders.ensureReconcile(this) }
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (e.values.isEmpty()) return
        val now = System.currentTimeMillis()
        when (e.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> onCounterEvent(e.values[0], now)
            Sensor.TYPE_STEP_DETECTOR -> onDetectorEvent(e.values[0], now)
            Sensor.TYPE_ACCELEROMETER -> onAccelerometerEvent(e.values, now)
            else -> Unit
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onCounterEvent(raw: Float, now: Long) {
        if (!raw.isFinite()) return
        if (activeSource != StepSource.COUNTER) {
            // The primary source recovered. Rebase to it and ignore the catch-up delta: the fallback
            // already counted everything it could see.
            activate(StepSource.COUNTER)
            return
        }
        health.onEvent(now)
        lastEventAt = now
        cadence.onEvent(raw, now, DateKey.today(now))
        publishRaw(raw)
        maybeMarkSourceEvent(now)
    }

    private fun onDetectorEvent(value: Float, now: Long) {
        val steps = value.toInt().coerceAtLeast(1)
        if (activeSource == StepSource.MOTION_ESTIMATE) {
            // The measured detector recovered while the estimate was live: prefer the measurement.
            activate(StepSource.DETECTOR)
            return
        }
        if (activeSource != StepSource.DETECTOR) return
        detectorCumulative += steps
        health.onEvent(now)
        lastEventAt = now
        cadence.onEvent(detectorCumulative, now, DateKey.today(now))
        publishRaw(detectorCumulative)
        maybeMarkSourceEvent(now)
    }

    private fun onAccelerometerEvent(values: FloatArray, now: Long) {
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val monotonic = android.os.SystemClock.elapsedRealtime()
        if (probeActive) {
            val detected = probeDetector?.onSample(magnitude, monotonic) ?: 0
            probeSteps += detected
        }
        if (activeSource == StepSource.MOTION_ESTIMATE) {
            estimateCumulative += estimateDetector.onSample(magnitude, monotonic)
            health.onEvent(now)
            lastEventAt = now
            cadence.onEvent(estimateCumulative, now, DateKey.today(now))
            publishRaw(estimateCumulative)
            maybeMarkSourceEvent(now)
        }
    }

    override fun onDestroy() {
        unregisterAll()
        flushPending()
        val wasCounting = mode == Mode.COUNTING
        if (!eraseInProgress && wasCounting) StatusStore.markStopped(this)
        scope.cancel()
        super.onDestroy()
    }

    /** Picks the best source this phone reports and activates it, falling down the ladder on failure. */
    private fun startPrimarySource() {
        var target = StepSourcePolicy.choose(availability)
        while (target != StepSource.NONE) {
            if (activate(target)) return
            availability = when (target) {
                StepSource.COUNTER -> availability.copy(counter = false)
                StepSource.DETECTOR -> availability.copy(detector = false)
                StepSource.HEALTH_CONNECT -> availability.copy(healthConnect = false)
                StepSource.MOTION_ESTIMATE -> availability.copy(accelerometer = false)
                StepSource.NONE -> availability
            }
            target = StepSourcePolicy.choose(availability)
        }
        block(Mode.NO_SENSOR)
    }

    /**
     * Registers [target] (plus the hardware counter whenever it exists, for recovery) and makes it
     * the active source. The old source's pending value is flushed first; the new source's first
     * event rebases the pipeline. Does not crash when a registration is refused: it reports false
     * and removes that sensor from [availability].
     */
    private fun activate(target: StepSource): Boolean {
        if (blocked || eraseInProgress) return false
        if (target == StepSource.NONE) return false
        if (target == activeSource) return true
        val now = System.currentTimeMillis()

        // Flush the old source before its scale is replaced, so its last steps are not lost.
        if (activeSource != StepSource.NONE) flushPending()
        cadence.rebase()

        if (!ensureRegistrations(target)) return false

        // Keep measured sensors registered for fast recovery; stop an accelerometer used only for a
        // previous estimate when a measured source is back.
        if (target != StepSource.MOTION_ESTIMATE && accelerometerRegistered && !probeActive) {
            unregister(accelerometerSensor)
            accelerometerRegistered = false
        }
        when (target) {
            StepSource.DETECTOR -> detectorCumulative = 0f
            StepSource.MOTION_ESTIMATE -> {
                estimateCumulative = 0f
                estimateDetector.reset()
            }
            else -> Unit
        }

        synchronized(rawLock) {
            latestRaw = Float.NaN
            latestSource = target
        }
        lastWrittenRaw = Float.NaN
        lastWrittenSource = StepSource.NONE
        if (activeSource != StepSource.NONE) switches++
        activeSource = target
        lastEventAt = 0L
        health.onSourceStart(target, now)
        markSourceStatus(force = true)
        mode = Mode.COUNTING
        markAlive(force = true)
        updateNotification()
        return true
    }

    private fun ensureRegistrations(target: StepSource): Boolean {
        // Synced source: nothing to register. Polls write the absolute total directly.
        if (target == StepSource.HEALTH_CONNECT) return true
        if (sensorManager == null) return false
        if (availability.counter && !counterRegistered) {
            val sensor = counterSensor
            counterRegistered = sensor != null && registerBatched(sensor, SensorManager.SENSOR_DELAY_NORMAL)
            if (!counterRegistered) availability = availability.copy(counter = false)
        }
        if (target == StepSource.COUNTER) {
            if (!counterRegistered) return false
        }
        if (availability.detector && !detectorRegistered) {
            val sensor = detectorSensor
            detectorRegistered = sensor != null && registerBatched(sensor, SensorManager.SENSOR_DELAY_NORMAL)
            if (!detectorRegistered) availability = availability.copy(detector = false)
        }
        if (target == StepSource.DETECTOR && !detectorRegistered) return false
        if (target == StepSource.MOTION_ESTIMATE) {
            if (!availability.accelerometer) return false
            if (!accelerometerRegistered) {
                val sensor = accelerometerSensor
                accelerometerRegistered = sensor != null &&
                    registerBatched(sensor, SensorManager.SENSOR_DELAY_UI)
                if (!accelerometerRegistered) availability = availability.copy(accelerometer = false)
            }
            if (!accelerometerRegistered) return false
        }
        return true
    }

    /** Batches counter/detector events when the HAL supports it; never delays the writer. */
    private fun registerBatched(sensor: Sensor, samplingUs: Int): Boolean {
        val manager = sensorManager ?: return false
        val batchUs = runCatching { sensor.maxDelay.coerceIn(0, MAX_BATCH_US) }.getOrDefault(0)
        return runCatching {
            if (batchUs > 0) {
                manager.registerListener(this, sensor, samplingUs, batchUs)
            } else {
                manager.registerListener(this, sensor, samplingUs)
            }
        }.getOrDefault(false)
    }

    private fun unregister(sensor: Sensor?) {
        val manager = sensorManager
        if (manager == null || sensor == null) return
        runCatching { manager.unregisterListener(this, sensor) }
    }

    private fun unregisterAll() {
        probeActive = false
        probeJob?.cancel()
        probeJob = null
        runCatching { sensorManager?.unregisterListener(this) }
        counterRegistered = false
        detectorRegistered = false
        accelerometerRegistered = false
    }

    /**
     * Starts a 5 s accelerometer probe after a measured source went silent. The probe runs at a
     * higher rate only for those 5 s; two consecutive walking probes switch tier.
     */
    private fun startMotionProbe() {
        if (probeActive || probeJob?.isActive == true) return
        val sensor = accelerometerSensor ?: return
        if (!availability.accelerometer || activeSource == StepSource.MOTION_ESTIMATE) return
        if (!accelerometerRegistered) {
            accelerometerRegistered = registerBatched(sensor, SensorManager.SENSOR_DELAY_GAME)
            if (!accelerometerRegistered) {
                availability = availability.copy(accelerometer = false)
                return
            }
        }
        probeDetector = MotionStepDetector()
        probeSteps = 0
        probeActive = true
        probeJob = scope.launch {
            delay(PROBE_WINDOW_MS)
            withContext(Dispatchers.Main) {
                val detector = probeDetector
                probeActive = false
                probeDetector = null
                val walking = detector?.isWalking == true || probeSteps >= PROBE_MIN_STEPS
                // The probe-accelerometer is only kept when it is the active estimate source.
                if (activeSource != StepSource.MOTION_ESTIMATE && accelerometerRegistered) {
                    unregister(accelerometerSensor)
                    accelerometerRegistered = false
                }
                val action = health.onProbeResult(System.currentTimeMillis(), walking)
                if (action == StepSourceHealth.Action.Fallback) switchFallback()
            }
        }
    }

    /** Moves to the next tier the phone can actually provide. */
    private fun switchFallback() {
        var target = StepSourcePolicy.fallbackTarget(activeSource, availability)
        while (target != null) {
            if (activate(target)) return
            target = StepSourcePolicy.fallbackTarget(activeSource, availability)
        }
        // Nothing left to switch to: stay on the current source and keep the honest status.
        markSourceStatus(force = true)
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
                val (raw, source) = readSnapshot()
                if (raw.isNaN() || source == StepSource.NONE) continue
                val now = System.currentTimeMillis()
                val today = DateKey.today(now)
                val active = cadence.activeMinutesFor(today)
                if (raw == lastWrittenRaw && active == lastWrittenActive &&
                    today == lastWrittenDate && source == lastWrittenSource
                ) {
                    continue
                }
                val result = runCatching {
                    app.container.steps.recordRawStep(
                        raw = raw,
                        now = now,
                        activeMinutesToday = active,
                        source = source,
                    )
                }
                if (result.isFailure) continue
                lastWrittenRaw = raw
                lastWrittenActive = active
                lastWrittenDate = today
                lastWrittenSource = source
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
                if (!eraseInProgress) {
                    markAlive(force = true)
                    markSourceStatus(force = true)
                    val action = health.onTick(System.currentTimeMillis())
                    if (action == StepSourceHealth.Action.ProbeMotion) {
                        withContext(Dispatchers.Main) { startMotionProbe() }
                    }
                }
                delay(STATUS_HEARTBEAT_MS)
            }
        }
    }

    /**
     * Polls Health Connect while it is the only real source this phone has. The poll is cheap and
     * local (the provider serves the aggregate; Sabeel itself never touches the network).
     *
     * The absolute total is applied as a floor: each successful read raises the day to at least the
     * provider's "steps since midnight" number and never adds a delta, so a repeated/late poll
     * cannot double count. The estimate may keep counting between polls; the floor only ever takes
     * the larger of the two.
     */
    private fun startHealthConnectPolling() {
        if (!healthConnectReady) return
        if (hcPollJob?.isActive == true) return
        hcPollJob = scope.launch {
            delay(HC_FIRST_POLL_MS)
            while (isActive) {
                if (!eraseInProgress && mode == Mode.COUNTING) {
                    pollHealthConnect()
                }
                delay(HC_POLL_MS)
            }
        }
    }

    private suspend fun pollHealthConnect() {
        if (activeSource == StepSource.COUNTER || activeSource == StepSource.DETECTOR) return
        val now = System.currentTimeMillis()
        val snapshot = HealthConnect.readToday(this, now)
        if (snapshot == null) {
            // The provider/read failed. If Health Connect was the live source and nothing has
            // arrived for a long time, fall back to the next tier (the estimate) instead of
            // pretending the stale total is current.
            if (activeSource == StepSource.HEALTH_CONNECT && now - lastEventAt > HC_STALE_MS) {
                withContext(Dispatchers.Main) { switchFallback() }
            }
            return
        }
        // A zero total is a legitimate quiet morning: never let it displace a working estimate.
        if (snapshot.steps <= 0 && activeSource != StepSource.HEALTH_CONNECT) return
        if (activeSource == StepSource.MOTION_ESTIMATE) {
            // Only hand over to Health Connect when it is actually ahead of what the estimate has
            // already counted; otherwise the estimate remains the more informative live source.
            val current = runCatching { app.container.steps.todayFlow().first()?.steps ?: 0 }
                .getOrDefault(0)
            if (snapshot.steps <= current) return
        }
        if (activeSource != StepSource.HEALTH_CONNECT) {
            availability = availability.copy(healthConnect = true)
            withContext(Dispatchers.Main) { activate(StepSource.HEALTH_CONNECT) }
            if (activeSource != StepSource.HEALTH_CONNECT) return
        }
        val today = DateKey.today(now)
        val active = cadence.activeMinutesFor(today)
        val row = runCatching {
            app.container.steps.recordExternalTotal(
                totalSteps = snapshot.steps,
                now = now,
                activeMinutesToday = active,
                source = StepSource.HEALTH_CONNECT,
            )
        }.getOrNull() ?: return
        // Only a rising total proves the sync is alive. A repeated read of yesterday's/morning's
        // number must not keep the watchdog asleep forever, or the live estimate would never resume.
        val increased = snapshot.steps > hcLastTotal
        hcLastTotal = maxOf(hcLastTotal, snapshot.steps)
        if (increased) {
            lastEventAt = now
            health.onEvent(now)
        }
        markSourceStatus(force = true)
        updateNotification()
        maybeNotifyGoal(row.steps)
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

    /** Writes source/last-event/switches at most every [SOURCE_MARK_INTERVAL_MS] (and on switches). */
    private fun markSourceStatus(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSourceMarkAt < SOURCE_MARK_INTERVAL_MS) return
        lastSourceMarkAt = now
        val today = DateKey.today(now)
        if (today != statusDay) {
            statusDay = today
            switches = 0
        }
        val (_, source) = readSnapshot()
        StatusStore.markSource(
            this,
            StepSourceStatus(source = source, lastEventAt = lastEventAt, switches = switches),
        )
    }

    private fun maybeMarkSourceEvent(now: Long) {
        if (now - lastSourceMarkAt < SOURCE_MARK_INTERVAL_MS) return
        markSourceStatus()
    }

    private fun publishRaw(raw: Float) {
        synchronized(rawLock) {
            latestRaw = raw
            latestSource = activeSource
        }
    }

    private fun readSnapshot(): Pair<Float, StepSource> = synchronized(rawLock) {
        latestRaw to latestSource
    }

    /** Final write at a lifecycle boundary (best effort, bounded so onDestroy cannot hang). */
    private fun flushPending() {
        if (eraseInProgress) return
        val (raw, source) = readSnapshot()
        if (raw.isNaN() || source == StepSource.NONE) return
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        val active = cadence.finish(now, today)
        val row = runBlocking {
            withTimeoutOrNull(FLUSH_TIMEOUT_MS) {
                runCatching {
                    app.container.steps.recordRawStep(
                        raw = raw,
                        now = now,
                        activeMinutesToday = active,
                        source = source,
                    )
                }.getOrNull()
            }
        }
        if (row != null) maybeNotifyGoal(row.steps)
    }

    private fun maybeNotifyGoal(steps: Int) {
        if (steps < goalSteps) return
        val estimated = activeSource == StepSource.MOTION_ESTIMATE
        runCatching { Notifications.notifyGoalReached(this, steps, goalSteps, estimated) }
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
        val (title, text) = when {
            mode == Mode.NO_PERMISSION -> localized.getString(R.string.sensor_notif_permission_title) to
                localized.getString(R.string.sensor_notif_permission_text)
            mode == Mode.NO_SENSOR -> localized.getString(R.string.sensor_notif_no_sensor_title) to
                localized.getString(R.string.sensor_notif_no_sensor_text)
            activeSource == StepSource.HEALTH_CONNECT ->
                localized.getString(R.string.sensor_notif_health_connect_title) to
                    localized.getString(R.string.sensor_notif_health_connect_text)
            activeSource == StepSource.MOTION_ESTIMATE ->
                localized.getString(R.string.sensor_notif_estimate_title) to
                    localized.getString(R.string.sensor_notif_estimate_text)
            else -> localized.getString(R.string.sensor_notif_counting_title) to
                localized.getString(R.string.sensor_notif_counting_text)
        }
        return Notifications.stepServiceNotification(this, title, text)
    }

    companion object {
        private const val WRITE_INTERVAL_MS = 1_500L
        private const val FLUSH_TIMEOUT_MS = 800L
        private const val STATUS_HEARTBEAT_MS = 30_000L
        private const val SOURCE_MARK_INTERVAL_MS = 10_000L
        private const val STALL_AFTER_MS = 600_000L
        private const val PROBE_WINDOW_MS = 5_000L
        private const val PROBE_MIN_STEPS = 3
        private const val MAX_BATCH_US = 20_000_000
        private const val HC_FIRST_POLL_MS = 5_000L
        private const val HC_POLL_MS = 120_000L
        private const val HC_STALE_MS = 30 * 60_000L

        /**
         * True while delete-all is suppressing service writes. Volatile because a service instance
         * dying mid-erase must not let the next instance resurrect a row before the wipe finishes.
         */
        @Volatile private var eraseInProgress = false

        /**
         * Starts the step service only when it can legally and usefully run.
         *
         * @return false when ACTIVITY_RECOGNITION or every counting sensor is missing (the status
         * store already holds the reason), or when the platform refuses the start. Never throws:
         * callers include onboarding, app-open and boot entry points where a crash is unacceptable.
         */
        fun ensureStarted(context: Context): Boolean {
            val appContext = context.applicationContext
            if (!Permissions.hasActivityRecognition(appContext)) {
                StatusStore.mark(appContext, StepServiceStatus.NO_PERMISSION)
                return false
            }
            if (!StepSensors.hasAny(appContext) && !HealthConnect.isReady(appContext)) {
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
