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
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.repo.StepSnapshot
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.MotionStepDetector
import com.sabeel.steps.domain.SilenceWatchdog
import com.sabeel.steps.domain.StepPipeline
import com.sabeel.steps.domain.StepSource
import com.sabeel.steps.domain.StepState
import com.sabeel.steps.platform.CountingStore
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Foreground service (type health) that owns the step manifold.
 *
 * Contract with the rest of the app:
 *  - the hardware step counter and step detector are registered together for the whole day and are
 *    never unregistered on a source change: [StepsRepository] keeps one ledger per source and fuses
 *    them with max semantics, so a stalled counter cannot throw away detector steps and a detector
 *    gap cannot throw away the counter's cumulative catch-up;
 *  - Health Connect is polled as an absolute floor whenever the grant exists, regardless of which
 *    hardware sensor is present; the poll applies a never-lowering floor and never adds a delta;
 *  - the accelerometer is used in three bounded ways: continuously only when the phone has no
 *    counter and no detector, as an 8 s probe after both measured sources went silent, and as the
 *    estimate accumulator once a walking probe proves the user is moving;
 *  - every write goes through [StepsRepository.record] as one coalesced snapshot (<= 1 per second);
 *    the writer is the only place that persists, and a failed write marks a visible storage fault;
 *  - [StepCadence] is fed the fused daily total, so active minutes no longer depend on which source
 *    was live;
 *  - crash containment: every sensor callback, lifecycle entry and poll is wrapped with
 *    `runCatching` of `Exception`; a single bad sample can never kill the process;
 *  - recovery: failed registrations are retried every heartbeat, Health Connect readiness is
 *    refreshed on every start, and boot/package-replace/app-open restart the service.
 *
 * Honest limits (see docs/review/2026-11-21/step-resilience/COUNCIL.md): non-wake-up sensors may
 * lose detector events while the application processor is suspended, and no app can restart itself
 * after a force-stop or an OEM deep clean.
 */
class StepCounterService : Service(), SensorEventListener {

    private enum class Mode { COUNTING, NO_PERMISSION, NO_SENSOR }

    /**
     * Outcome of [ensureStarted]. STARTED means "the platform accepted the foreground-service
     * request"; the instance can still stand down later (permission race), which is why boot
     * attempts also carry an aliveAfterCheck from [com.sabeel.steps.platform.BootHealthWorker].
     */
    enum class StartResult { STARTED, DISABLED, NO_PERMISSION, NO_SENSOR, REFUSED }

    private val app: SabeelApp get() = application as SabeelApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cadence = StepCadence()
    private val watchdog = SilenceWatchdog()

    private var sensorManager: SensorManager? = null
    private var counterSensor: Sensor? = null
    private var detectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    private var counterRegistered = false
    private var detectorRegistered = false
    private var accelerometerRegistered = false

    /** Coalesced sensor evidence waiting for the next writer tick. */
    private val pendingLock = Any()
    private var pendingCounterRaw: Float? = null
    private var pendingCounterNanos = 0L
    private var pendingDetectorSteps = 0
    private var pendingDetectorNanos = 0L
    private var pendingEstimateSteps = 0

    private val estimateDetector = MotionStepDetector()
    @Volatile private var estimateActive = false

    @Volatile private var probeActive = false
    private var probeDetector: MotionStepDetector? = null
    private var probeSteps = 0
    private var probeJob: Job? = null

    @Volatile private var activeSource = StepSource.NONE
    @Volatile private var counterEventAt = 0L
    @Volatile private var detectorEventAt = 0L
    @Volatile private var estimateEventAt = 0L
    @Volatile private var hcLastReadAt = 0L
    @Volatile private var hcLastTotal = -1
    @Volatile private var healthConnectReady = false

    @Volatile private var seeded = false
    private var foregroundStarted = false
    private var blocked = false
    private var mode = Mode.COUNTING

    private var writeJob: Job? = null
    private var heartbeatJob: Job? = null
    private var hcPollJob: Job? = null
    private val writeMutex = Mutex()

    @Volatile private var goalSteps = 8000
    @Volatile private var lastStatusMarkAt = 0L
    @Volatile private var lastSourceMarkAt = 0L
    @Volatile private var lastEstimateAt = 0L
    private var consecutiveWriteFailures = 0
    private var switches = 0
    private var statusDay = ""
    private var lastNotificationTitle = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            // The user's intent is the first gate, written before any action: a START_STICKY
            // restart after a deliberate stop must never resurrect the service (the service never
            // self-enables). No foreground start happens here, so stopSelf() is legal.
            if (!countingIntended()) {
                runCatching { StatusStore.mark(this, StepServiceStatus.OFF_BY_USER) }
                stopSelf()
                return
            }
            initialize()
        } catch (e: Exception) {
            // An unexpected failure during startup must degrade, never crash-loop the process.
            runCatching { block(Mode.NO_SENSOR) }
            runCatching { stopSelf() }
        }
    }

    private fun initialize() {
        // Hard gate: never call startForeground(type=health) without its Android 14+ runtime
        // prerequisite. The system throws SecurityException after the call, on the main thread, so a
        // caller-side catch cannot save the process. Degrade visibly instead.
        if (!Permissions.hasActivityRecognition(this)) {
            block(Mode.NO_PERMISSION)
            return
        }
        sensorManager = runCatching { getSystemService(SensorManager::class.java) }.getOrNull()
        if (sensorManager == null) {
            block(Mode.NO_SENSOR)
            return
        }
        counterSensor = runCatching { sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }.getOrNull()
        detectorSensor = runCatching { sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }.getOrNull()
        accelerometerSensor = runCatching { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }.getOrNull()
        healthConnectReady = runCatching { HealthConnect.isReady(this) }.getOrDefault(false)

        if (counterSensor == null && detectorSensor == null && accelerometerSensor == null && !healthConnectReady) {
            block(Mode.NO_SENSOR)
            return
        }
        runCatching { Notifications.ensureChannels(this) }
        if (!startInForeground()) {
            // The platform refused the foreground start (background start restriction, revoked
            // prerequisite race). STOPPED is the truthful state; a permission card would be wrong.
            markStoppedWithoutErase()
            stopSelf()
            return
        }
        mode = Mode.COUNTING
        runningInstance = true
        markAlive(force = true)
        watchdog.onStart(System.currentTimeMillis())

        scope.launch {
            try {
                seedAndStart()
            } catch (e: Exception) {
                // A failed seed (corrupt DataStore, transient Room error) must not leave the service
                // running with no writer: the day would silently stop counting. Start the loops
                // anyway; the writer/repository paths retry and surface storage faults.
                if (!seeded) {
                    seeded = true
                    startWriter()
                    startHeartbeat()
                    startHealthConnectPolling()
                }
            }
        }
        scope.launch {
            app.container.settings.settingsFlow.collect { goalSteps = it.goalSteps }
        }
        runCatching { Reminders.ensureReconcile(this) }
    }

    private suspend fun seedAndStart() {
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        val seededActive = runCatching { app.container.steps.todayFlow().first()?.activeMinutes ?: 0 }
            .getOrDefault(0)
        cadence.seed(today, seededActive, now)
        runCatching {
            app.container.steps.reconcile(activeMinutesToday = cadence.activeMinutesFor(today))
        }
        // A deliberate pause ended: rebase explicitly instead of letting the counter catch-up path
        // count the paused gap. Runs after reconcile (so today's persisted state is current) and
        // before the writer/sensors start (so the first sample can never look like new evidence).
        maybeResumeAfterPause()
        // Phones with no measured sensor estimate continuously; the estimate is always labelled.
        if (counterSensor == null && detectorSensor == null && accelerometerSensor != null) {
            withContext(Dispatchers.Main) { enableEstimate(continuous = true) }
        }
        withContext(Dispatchers.Main) {
            ensureMeasuredRegistrations()
            if (estimateActive) registerAccelerometer(rateUs = SensorManager.SENSOR_DELAY_UI)
        }
        seeded = true
        startWriter()
        startHeartbeat()
        startHealthConnectPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Explicit stop from the controller/UI. The controller has already persisted the off
            // intent; onDestroy writes the pause baseline and marks OFF_BY_USER.
            stopSelf()
            return START_NOT_STICKY
        }
        if (blocked) {
            markBlockedStatus()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!countingIntended()) {
            // A sticky restart or a late start after the user turned counting off: stand down.
            runCatching { StatusStore.mark(this, StepServiceStatus.OFF_BY_USER) }
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching {
            if (!Permissions.hasActivityRecognition(this)) {
                block(Mode.NO_PERMISSION)
                return START_NOT_STICKY
            }
            refreshHealthConnect()
            if (seeded) {
                // A fast off->on toggle can land before the stop was processed: the instance is still
                // alive with a pending pause marker, so run the explicit resume now instead of letting
                // the live counter sample be read as a paused-gap catch-up.
                if (countingIntended() && pausedMarkerPresent()) {
                    scope.launch { maybeResumeAfterPause() }
                }
                // A grant made after the service started, or a registration that failed earlier,
                // must take effect without waiting for a process death.
                ensureMeasuredRegistrations()
                if (estimateActive) registerAccelerometer(rateUs = SensorManager.SENSOR_DELAY_UI)
                markAlive(force = true)
                markSourceStatus(force = true)
            }
            if (intent?.action == ACTION_FLUSH) requestHardwareFlush()
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        try {
            if (e.values.isEmpty()) return
            val now = System.currentTimeMillis()
            when (e.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> onCounterEvent(e.values[0], e.timestamp, now)
                Sensor.TYPE_STEP_DETECTOR -> onDetectorEvent(e.values[0], e.timestamp, now)
                Sensor.TYPE_ACCELEROMETER -> onAccelerometerEvent(e.values, now)
                else -> Unit
            }
        } catch (ignored: Exception) {
            // A malformed HAL sample must never crash the counting process.
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onCounterEvent(raw: Float, eventNanos: Long, now: Long) {
        if (!raw.isFinite()) return
        synchronized(pendingLock) {
            pendingCounterRaw = raw
            pendingCounterNanos = eventNanos
        }
        counterEventAt = now
        onMeasuredEvent(now)
    }

    private fun onDetectorEvent(value: Float, eventNanos: Long, now: Long) {
        val steps = value.toInt().coerceIn(1, StepPipeline.MAX_DETECTOR_EVENT)
        synchronized(pendingLock) {
            pendingDetectorSteps += steps
            pendingDetectorNanos = eventNanos
        }
        detectorEventAt = now
        onMeasuredEvent(now)
    }

    private fun onAccelerometerEvent(values: FloatArray, now: Long) {
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        if (!magnitude.isFinite()) return
        val monotonic = SystemClock.elapsedRealtime()
        if (probeActive) {
            probeSteps += probeDetector?.onSample(magnitude, monotonic) ?: 0
        }
        if (estimateActive) {
            val detected = estimateDetector.onSample(magnitude, monotonic)
            if (detected > 0) {
                synchronized(pendingLock) { pendingEstimateSteps += detected }
                estimateEventAt = now
                lastEstimateAt = now
            }
        }
    }

    /** Every counter/detector event proves the measured side is alive and stops any estimate. */
    private fun onMeasuredEvent(now: Long) {
        when (watchdog.onMeasuredEvent(now)) {
            SilenceWatchdog.Action.StopEstimate -> stopEstimate()
            else -> Unit
        }
        if (now - lastSourceMarkAt >= SOURCE_MARK_INTERVAL_MS) markSourceStatus()
    }

    override fun onDestroy() {
        runningInstance = false
        try {
            writeJob?.cancel()
            heartbeatJob?.cancel()
            hcPollJob?.cancel()
            probeActive = false
            probeJob?.cancel()
            probeJob = null
            runCatching { sensorManager?.unregisterListener(this) }
            counterRegistered = false
            detectorRegistered = false
            accelerometerRegistered = false
            flushPendingOnDestroy()
            when {
                eraseInProgress -> Unit // delete-all owns the state; nothing to report.
                !countingIntended() -> {
                    // Deliberate stop: freeze the manifold baselines while the last saved evidence is
                    // current, so the next start resumes with delta 0 instead of counting the gap.
                    runCatching { flushPauseOnStop() }
                    runCatching { StatusStore.mark(this, StepServiceStatus.OFF_BY_USER) }
                }
                mode == Mode.COUNTING -> StatusStore.markStopped(this)
            }
        } catch (ignored: Exception) {
            // Teardown must never throw.
        } finally {
            scope.cancel()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // The user swiped the app away; the service is sticky, but flush the last seconds now in
        // case the OEM kills the process immediately after the task removal.
        if (!eraseInProgress) scope.launch { writeSnapshot() }
        super.onTaskRemoved(rootIntent)
    }

    // ---- registration ---------------------------------------------------------------------

    private fun ensureMeasuredRegistrations() {
        if (blocked) return
        if (counterSensor != null && !counterRegistered) {
            counterRegistered = registerPlain(counterSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (detectorSensor != null && !detectorRegistered) {
            detectorRegistered = registerPlain(detectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun registerAccelerometer(rateUs: Int): Boolean {
        if (accelerometerRegistered) return true
        accelerometerRegistered = registerPlain(accelerometerSensor, rateUs)
        return accelerometerRegistered
    }

    private fun unregisterAccelerometer() {
        if (!accelerometerRegistered) return
        runCatching { accelerometerSensor?.let { sensorManager?.unregisterListener(this, it) } }
        accelerometerRegistered = false
    }

    /**
     * Deliberately unbatched: a step counter's on-change events are already cheap, and a hardware
     * FIFO delay would only make the display and the watchdog lag behind reality.
     */
    private fun registerPlain(sensor: Sensor?, rateUs: Int): Boolean {
        val manager = sensorManager ?: return false
        val s = sensor ?: return false
        return runCatching { manager.registerListener(this, s, rateUs) }.getOrDefault(false)
    }

    // ---- estimate / probe -----------------------------------------------------------------

    private fun enableEstimate(continuous: Boolean) {
        if (estimateActive || blocked || eraseInProgress) return
        if (accelerometerSensor == null) return
        estimateActive = true
        estimateDetector.reset()
        if (continuous) watchdog.startContinuousEstimate(System.currentTimeMillis())
        unregisterAccelerometer()
        registerAccelerometer(rateUs = SensorManager.SENSOR_DELAY_UI)
        markSourceStatus(force = true)
        updateNotification()
    }

    private fun stopEstimate() {
        if (!estimateActive) return
        estimateActive = false
        estimateDetector.reset()
        // The measured side is alive again; the accelerometer is no longer needed for counting.
        if (!probeActive) unregisterAccelerometer()
        markSourceStatus(force = true)
        updateNotification()
    }

    /**
     * The heartbeat asks the watchdog whether both measured sources have been silent long enough to
     * justify a bounded probe. In-flight is set only after the probe is actually scheduled, so a
     * registration failure can never disable the watchdog.
     */
    private suspend fun maybeStartProbe() {
        if (probeActive || estimateActive || accelerometerSensor == null) return
        val now = System.currentTimeMillis()
        if (watchdog.onTick(now) != SilenceWatchdog.Action.ProbeMotion) return
        val started = withContext(Dispatchers.Main) {
            val ok = accelerometerRegistered || registerAccelerometer(SensorManager.SENSOR_DELAY_GAME)
            if (!ok) {
                false
            } else {
                probeDetector = MotionStepDetector()
                probeSteps = 0
                probeActive = true
                watchdog.probeScheduled(now)
                true
            }
        }
        if (!started) {
            watchdog.probeUnavailable(now)
            return
        }
        probeJob = scope.launch {
            delay(PROBE_WINDOW_MS)
            withContext(Dispatchers.Main) { finishProbe() }
        }
    }

    private suspend fun finishProbe() {
        if (!probeActive) return
        val detector = probeDetector
        val walking = detector?.isWalking == true || probeSteps >= PROBE_MIN_STEPS
        probeActive = false
        probeDetector = null
        probeJob = null
        val action = watchdog.onProbeResult(System.currentTimeMillis(), walking)
        if (action == SilenceWatchdog.Action.StartEstimate) {
            enableEstimate(continuous = false)
        } else if (!estimateActive) {
            // No walking: release the accelerometer until the next (backed-off) probe.
            unregisterAccelerometer()
        }
    }

    /**
     * A physically disconnected/stuck accelerometer delivers no samples while the estimate is
     * active. Re-register it every [ESTIMATE_STALL_MS] of silence; the writer keeps the last saved
     * total, so a re-registration can only add new steps.
     */
    private suspend fun refreshEstimateIfStalled() {
        if (!estimateActive) return
        val now = System.currentTimeMillis()
        if (now - lastEstimateAt < ESTIMATE_STALL_MS) return
        lastEstimateAt = now
        withContext(Dispatchers.Main) {
            unregisterAccelerometer()
            estimateDetector.reset()
            registerAccelerometer(SensorManager.SENSOR_DELAY_UI)
        }
        markSourceStatus(force = true)
    }

    // ---- writer ---------------------------------------------------------------------------

    @Synchronized
    private fun startWriter() {
        if (writeJob?.isActive == true) return
        writeJob = scope.launch {
            while (isActive) {
                delay(WRITE_INTERVAL_MS)
                if (!eraseInProgress) writeSnapshot()
            }
        }
    }

    /** Coalesces every pending source into one repository write; returns after the state is saved. */
    private suspend fun writeSnapshot() = writeMutex.withLock {
        writeSnapshotLocked()
    }

    private suspend fun writeSnapshotLocked() {
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        val snapshot = takePendingSnapshot() ?: return
        val activeBefore = cadence.activeMinutesFor(today)
        val row: com.sabeel.steps.domain.DailyStep? = try {
            app.container.steps.record(snapshot, now, activeBefore)
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (failed: Exception) {
            onWriteFailure()
            return
        }
        onWriteSuccess()
        if (row != null) {
            cadence.onEvent(row.steps.toFloat(), now, today)
            val activeAfter = cadence.activeMinutesFor(today)
            if (activeAfter != row.activeMinutes) {
                runCatching { app.container.steps.updateActiveMinutes(activeAfter, now) }
                    .onFailure { onWriteFailure() }
            }
        } else {
            // A baseline-only snapshot persists the state but creates no zero row; the cadence still
            // advances so a later step in the same minute is attributed correctly.
            cadence.onEvent(0f, now, today)
        }
        updateSourceAndStatus(now)
        markAlive()
        row?.let { maybeNotifyGoal(it.steps, it.estimatedSteps > 0) }
    }

    private fun takePendingSnapshot(): StepSnapshot? {
        synchronized(pendingLock) {
            val counter = pendingCounterRaw
            val counterNanos = pendingCounterNanos
            val detector = pendingDetectorSteps
            val detectorNanos = pendingDetectorNanos
            val estimate = pendingEstimateSteps
            pendingCounterRaw = null
            pendingCounterNanos = 0L
            pendingDetectorSteps = 0
            pendingDetectorNanos = 0L
            pendingEstimateSteps = 0
            if (counter == null && detector <= 0 && estimate <= 0) return null
            return StepSnapshot(
                counterRaw = counter,
                counterEventNanos = counterNanos,
                detectorSteps = detector,
                detectorEventNanos = detectorNanos,
                estimateSteps = estimate,
            )
        }
    }

    private fun onWriteFailure() {
        consecutiveWriteFailures++
        if (consecutiveWriteFailures == 1 || consecutiveWriteFailures % 30 == 0) {
            runCatching { StatusStore.markStorageFault(this, true) }
        }
    }

    private fun onWriteSuccess() {
        if (consecutiveWriteFailures > 0) {
            consecutiveWriteFailures = 0
            runCatching { StatusStore.markStorageFault(this, false) }
        }
    }

    // ---- heartbeat / watchdog -------------------------------------------------------------

    @Synchronized
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (isActive) {
                if (!eraseInProgress) {
                    runCatching {
                        markAlive(force = true)
                        markSourceStatus(force = true)
                        withContext(Dispatchers.Main) {
                            ensureMeasuredRegistrations()
                            if (estimateActive) registerAccelerometer(SensorManager.SENSOR_DELAY_UI)
                        }
                        refreshHealthConnectIfNeeded()
                        refreshEstimateIfStalled()
                        maybeStartProbe()
                    }
                }
                delay(STATUS_HEARTBEAT_MS)
            }
        }
    }

    /** Re-checks the Health Connect grant at most once a minute; a late grant is picked up here. */
    private suspend fun refreshHealthConnectIfNeeded() {
        if (System.currentTimeMillis() - lastHcRefreshAt < HC_REFRESH_INTERVAL_MS) return
        refreshHealthConnect()
    }

    private fun refreshHealthConnect() {
        lastHcRefreshAt = System.currentTimeMillis()
        val ready = runCatching { HealthConnect.isReady(this) }.getOrDefault(false)
        if (ready == healthConnectReady) {
            if (ready) startHealthConnectPolling()
            return
        }
        healthConnectReady = ready
        if (ready) startHealthConnectPolling()
    }

    private var lastHcRefreshAt = 0L

    // ---- Health Connect -------------------------------------------------------------------

    @Synchronized
    private fun startHealthConnectPolling() {
        if (!healthConnectReady) return
        if (hcPollJob?.isActive == true) return
        hcPollJob = scope.launch {
            delay(HC_FIRST_POLL_MS)
            while (isActive) {
                if (!eraseInProgress) runCatching { pollHealthConnect() }
                delay(hcPollInterval())
            }
        }
    }

    private fun hcPollInterval(): Long {
        val now = System.currentTimeMillis()
        val measuredLive = (counterEventAt > 0 && now - counterEventAt <= MEASURED_LIVE_WINDOW_MS) ||
            (detectorEventAt > 0 && now - detectorEventAt <= MEASURED_LIVE_WINDOW_MS)
        return if (measuredLive) HC_POLL_NORMAL_MS else HC_POLL_FAST_MS
    }

    private suspend fun pollHealthConnect() {
        if (eraseInProgress) return
        if (!HealthConnect.hasPermission(this)) {
            healthConnectReady = false
            hcPollJob?.cancel()
            return
        }
        val now = System.currentTimeMillis()
        val snapshot = HealthConnect.readToday(this, now) ?: return
        hcLastReadAt = now
        if (snapshot.steps <= 0) return
        val increased = snapshot.steps > hcLastTotal
        hcLastTotal = maxOf(hcLastTotal, snapshot.steps)
        if (increased) {
            val today = DateKey.today(now)
            val active = cadence.activeMinutesFor(today)
            runCatching {
                app.container.steps.record(
                    StepSnapshot(hcTotal = snapshot.steps),
                    now,
                    active,
                )
            }.onSuccess { row ->
                row?.let { maybeNotifyGoal(it.steps, it.estimatedSteps > 0) }
            }.onFailure { onWriteFailure() }
            updateSourceAndStatus(now)
            updateNotification()
        } else {
            // Repeated read of the same total: freshness is proven (last sync in Diagnostics) but
            // no write is needed.
            markSourceStatus()
        }
        markAlive()
    }

    // ---- status ---------------------------------------------------------------------------

    @Synchronized
    private fun updateSourceAndStatus(now: Long) {
        val today = DateKey.today(now)
        if (today != statusDay) {
            statusDay = today
            switches = 0
        }
        val source = sourceForDisplay(now)
        if (source != activeSource) {
            if (activeSource != StepSource.NONE) switches++
            activeSource = source
            updateNotification()
        }
        if (now - lastSourceMarkAt >= SOURCE_MARK_INTERVAL_MS) markSourceStatus(force = true)
    }

    private fun sourceForDisplay(now: Long): StepSource {
        val counterLive = counterEventAt > 0 && now - counterEventAt <= SOURCE_RECENT_MS
        val detectorLive = detectorEventAt > 0 && now - detectorEventAt <= SOURCE_RECENT_MS
        val hcLive = hcLastReadAt > 0 && now - hcLastReadAt <= HC_FRESH_MS && hcLastTotal > 0
        return when {
            counterLive -> StepSource.COUNTER
            detectorLive -> StepSource.DETECTOR
            hcLive -> StepSource.HEALTH_CONNECT
            estimateActive -> StepSource.MOTION_ESTIMATE
            counterEventAt > 0 -> StepSource.COUNTER
            detectorEventAt > 0 -> StepSource.DETECTOR
            else -> StepSource.NONE
        }
    }

    private fun lastEventAt(): Long =
        maxOf(counterEventAt, detectorEventAt, lastEstimateAt, hcLastReadAt)

    @Synchronized
    private fun markAlive(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastStatusMarkAt < STATUS_HEARTBEAT_MS) return
        lastStatusMarkAt = now
        val status = when (mode) {
            Mode.COUNTING -> StepServiceStatus.COUNTING
            Mode.NO_PERMISSION -> StepServiceStatus.NO_PERMISSION
            Mode.NO_SENSOR -> StepServiceStatus.NO_SENSOR
        }
        runCatching { StatusStore.mark(this, status) }
    }

    @Synchronized
    private fun markSourceStatus(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSourceMarkAt < SOURCE_MARK_INTERVAL_MS) return
        lastSourceMarkAt = now
        val today = DateKey.today(now)
        if (today != statusDay) {
            statusDay = today
            switches = 0
        }
        runCatching {
            StatusStore.markSource(
                this,
                StepSourceStatus(
                    source = activeSource,
                    lastEventAt = lastEventAt(),
                    switches = switches,
                ),
            )
        }
    }

    // ---- teardown -------------------------------------------------------------------------

    /** Final write at a lifecycle boundary; bounded so onDestroy cannot hang the main thread. */
    private fun flushPendingOnDestroy() {
        if (eraseInProgress) return
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        val snapshot = takePendingSnapshot() ?: return
        val active = cadence.finish(now, today)
        var failed = false
        val row = runBlocking {
            withTimeoutOrNull(FLUSH_TIMEOUT_MS) {
                try {
                    writeMutex.withLock {
                        app.container.steps.record(snapshot, now, active)
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    null
                } catch (e: Exception) {
                    failed = true
                    null
                }
            }
        }
        if (failed) runCatching { StatusStore.markStorageFault(this, true) }
        row?.let { maybeNotifyGoal(it.steps, it.estimatedSteps > 0) }
    }

    /**
     * Persists the frozen manifold at a deliberate stop: totals stay, evidence baselines drop and
     * Health Connect is suppressed until the resume baseline (or midnight) is known. Bounded and
     * synchronous by design — onDestroy cannot await a coroutine, and losing this write would let the
     * next start read the paused gap as measured steps. [flushPendingOnDestroy] ran first, so this
     * reads the state that just saved the last real evidence.
     */
    private fun flushPauseOnStop() {
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        runBlocking {
            withTimeoutOrNull(PAUSE_FLUSH_TIMEOUT_MS) {
                try {
                    writeMutex.withLock {
                        val stored = app.container.settings.readStepState(today)
                        app.container.settings.writeStepState(StepPipeline.pause(stored, now))
                    }
                } catch (ignored: Exception) {
                    // A failed pause write is not fatal: CountingStore.pausedAt still forces the
                    // explicit resume path, which rebases both baselines from scratch.
                }
            }
        }
    }

    /**
     * Explicit resume after a deliberate pause (council A4). The raw counter value is not known
     * while the sensors are unregistered, so a NaN baseline is passed when no sample is pending;
     * the first real sample then only rebases, never adds the paused gap. Health Connect restarts
     * from the provider's current total, or is suppressed until midnight when it cannot answer.
     */
    private suspend fun maybeResumeAfterPause() {
        if (!pausedMarkerPresent()) return
        writeMutex.withLock { resumePausedLocked() }
    }

    private suspend fun resumePausedLocked() {
        val now = System.currentTimeMillis()
        val today = DateKey.today(now)
        val stored: StepState = runCatching { app.container.settings.readStepState(today) }
            .getOrNull() ?: StepState(day = today)
        val hcReading = runCatching { HealthConnect.readToday(this, now)?.steps }.getOrNull()
        val raw = synchronized(pendingLock) { pendingCounterRaw } ?: Float.NaN
        val resumed = StepPipeline.resume(stored, raw, today, hcReading, now)
        runCatching { app.container.settings.writeStepState(resumed) }
        // Consume the pending counter sample: it was used only as a baseline, never as a delta.
        synchronized(pendingLock) {
            pendingCounterRaw = null
            pendingCounterNanos = 0L
            pendingDetectorSteps = 0
            pendingDetectorNanos = 0L
            pendingEstimateSteps = 0
        }
        estimateDetector.reset()
        if (hcReading != null && hcReading >= 0) hcLastTotal = hcReading
        runCatching { CountingStore.get(this).clearPaused() }
    }

    private fun pausedMarkerPresent(): Boolean =
        runCatching { CountingStore.get(this).current().pausedAt > 0L }.getOrDefault(false)

    private fun countingIntended(): Boolean =
        runCatching { CountingStore.get(this).current().enabled }.getOrDefault(true)

    private fun maybeNotifyGoal(steps: Int, estimated: Boolean) {
        if (steps < goalSteps) return
        runCatching { Notifications.notifyGoalReached(this, steps, goalSteps, estimated) }
    }

    // ---- foreground / notification --------------------------------------------------------

    private fun requestHardwareFlush() {
        val manager = sensorManager ?: return
        runCatching { manager.flush(this) }
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
        runCatching {
            val localized = AppLocale.wrap(this)
            val (title, text) = notificationContent(localized)
            val key = "$title\u0000$text"
            if (key != lastNotificationTitle) {
                lastNotificationTitle = key
                Notifications.post(this, Notifications.ID_STEP_SERVICE, buildNotification(title, text))
            }
        }
    }

    private fun buildNotification(): Notification {
        val (title, text) = notificationContent(AppLocale.wrap(this))
        return buildNotification(title, text)
    }

    private fun buildNotification(title: String, text: String): Notification =
        Notifications.stepServiceNotification(this, title, text)

    private fun notificationContent(localized: Context): Pair<String, String> = when {
        mode == Mode.NO_PERMISSION -> localized.getString(R.string.sensor_notif_permission_title) to
            localized.getString(R.string.sensor_notif_permission_text)
        mode == Mode.NO_SENSOR -> localized.getString(R.string.sensor_notif_no_sensor_title) to
            localized.getString(R.string.sensor_notif_no_sensor_text)
        activeSource == StepSource.HEALTH_CONNECT ->
            localized.getString(R.string.sensor_notif_health_connect_title) to
                localized.getString(R.string.sensor_notif_health_connect_text)
        estimateActive ->
            localized.getString(R.string.sensor_notif_estimate_title) to
                localized.getString(R.string.sensor_notif_estimate_text)
        else -> localized.getString(R.string.sensor_notif_counting_title) to
            localized.getString(R.string.sensor_notif_counting_text)
    }

    // ---- degraded states -------------------------------------------------------------------

    private fun block(nextMode: Mode) {
        blocked = true
        mode = nextMode
        markBlockedStatus()
        stopSelf()
    }

    private fun markBlockedStatus() {
        markAlive(force = true)
    }

    private fun markStoppedWithoutErase() {
        mode = Mode.NO_SENSOR
        runCatching { StatusStore.markStopped(this) }
    }

    companion object {
        private const val WRITE_INTERVAL_MS = 1_000L
        private const val FLUSH_TIMEOUT_MS = 800L
        private const val STATUS_HEARTBEAT_MS = 30_000L
        private const val SOURCE_MARK_INTERVAL_MS = 10_000L
        private const val SOURCE_RECENT_MS = 120_000L
        private const val MEASURED_LIVE_WINDOW_MS = 180_000L
        private const val HC_FRESH_MS = 5 * 60_000L
        private const val HC_FIRST_POLL_MS = 3_000L
        private const val HC_POLL_NORMAL_MS = 180_000L
        private const val HC_POLL_FAST_MS = 60_000L
        private const val HC_REFRESH_INTERVAL_MS = 60_000L
        private const val PROBE_WINDOW_MS = 8_000L
        private const val PROBE_MIN_STEPS = 2
        private const val ESTIMATE_STALL_MS = 5 * 60_000L

        /** Bounded synchronous pause-baseline write during onDestroy. */
        private const val PAUSE_FLUSH_TIMEOUT_MS = 600L

        /** Used by the UI to distinguish "service instance alive in this process" from a stale mark. */
        @Volatile var runningInstance: Boolean = false
            private set

        /** True while delete-all is suppressing service writes. */
        @Volatile private var eraseInProgress = false

        const val ACTION_FLUSH = "com.sabeel.steps.action.FLUSH"

        /** Explicit user stop; the controller persists the off intent before sending this. */
        const val ACTION_STOP = "com.sabeel.steps.action.STOP"

        /** Explicit re-arm after the user turned counting back on. */
        const val ACTION_START = "com.sabeel.steps.action.START"

        /**
         * Starts the step service only when it can legally and usefully run.
         *
         * Checks the persisted user intent first (the service never self-enables), then the Android
         * preconditions. Never throws: callers include onboarding, app-open and boot entry points
         * where a crash is unacceptable. Every outcome is persisted so Diagnostics can show why the
         * last start did not count.
         */
        fun ensureStarted(context: Context): StartResult {
            val appContext = context.applicationContext
            val result = runCatching {
                if (!CountingStore.get(appContext).current().enabled) {
                    StatusStore.mark(appContext, StepServiceStatus.OFF_BY_USER)
                    return@runCatching StartResult.DISABLED
                }
                if (!Permissions.hasActivityRecognition(appContext)) {
                    StatusStore.mark(appContext, StepServiceStatus.NO_PERMISSION)
                    return@runCatching StartResult.NO_PERMISSION
                }
                if (!StepSensors.hasAny(appContext) && !HealthConnect.isReady(appContext)) {
                    StatusStore.mark(appContext, StepServiceStatus.NO_SENSOR)
                    return@runCatching StartResult.NO_SENSOR
                }
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, StepCounterService::class.java),
                )
                StartResult.STARTED
            }.getOrDefault(StartResult.REFUSED)
            runCatching { StatusStore.recordStartResult(appContext, result.name) }
            return result
        }

        /** The user turned counting back on: the same guarded start, with an explicit outcome. */
        fun requestStart(context: Context): StartResult = ensureStarted(context)

        /** Asks the running service to flush the hardware FIFO and persist immediately (app open). */
        fun requestFlush(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.startService(
                    Intent(appContext, StepCounterService::class.java).setAction(ACTION_FLUSH),
                )
            }
        }

        /**
         * Stops the service for a deliberate off toggle. The caller has already persisted the off
         * intent (write-before-act); onDestroy then writes the pause baseline and marks OFF_BY_USER.
         * stopService is used instead of an ACTION_STOP start because it is always legal, even when
         * the app is in the background.
         */
        fun requestStop(context: Context) {
            val appContext = context.applicationContext
            runCatching { StatusStore.mark(appContext, StepServiceStatus.OFF_BY_USER) }
            runCatching { appContext.stopService(Intent(appContext, StepCounterService::class.java)) }
        }

        /** True while delete-all is suppressing service writes. */
        fun isEraseInProgress(): Boolean = eraseInProgress

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