package com.sabeel.steps.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.MotionStepDetector
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.StepSource
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.ui.i18n.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Foreground service (type location) that records a walk without any map SDK, network call or
 * Play Services dependency:
 *  - LocationManager updates (GPS, network fallback) ~ every 3 s / 5 m, distance from haversine;
 *  - step delta during the walk from the hardware counter (when the sensor permission exists);
 *  - [WalkTracker] is the live state the Walk screen collects (this service is the only writer);
 *  - the route and stats are checkpointed to a private file every 20 s so a process death does not
 *    lose the walk; a sticky restart resumes from the checkpoint.
 *
 * Concurrency contract: every mutable field the checkpoint writer reads is @Volatile and the route
 * is an immutable List replaced (never mutated) on each accepted fix, so the writer can serialize a
 * consistent snapshot from Dispatchers.Default while location/sensor callbacks run on the main
 * thread. A bounded flush runs in [onDestroy] so a teardown loses at most the current moment.
 *
 * With no location permission the walk runs step-only and reports gpsActive = false; with neither
 * location nor activity perception permission the service cannot legally run at all and
 * [start] returns false instead of starting a foreground service without a permitted type.
 */
class WalkTrackingService : Service(), SensorEventListener, LocationListener {

    private val app: SabeelApp get() = application as SabeelApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var locationManager: LocationManager? = null
    private var sensorManager: SensorManager? = null
    private var counterSensor: Sensor? = null
    private var detectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var stepSource = StepSource.NONE
    private var stepRegistered = false
    private var locationRegistered = false
    private var foregroundStarted = false
    private var blocked = false

    // Written on the service main thread, read by the checkpoint coroutine: all @Volatile.
    @Volatile private var running = false
    @Volatile private var startedAt = 0L
    @Volatile private var gpsActive = false
    @Volatile private var distanceMeters = 0.0
    @Volatile private var steps = 0
    @Volatile private var lastRaw = Float.NaN
    private var detectorCumulative = 0f
    private val motionDetector = MotionStepDetector()
    @Volatile private var lastLocation: Location? = null

    /**
     * Immutable route snapshot: a callback publishes "route = route + point", it never mutates a
     * shared ArrayList. The checkpoint writer only ever iterates a list that cannot change under it.
     */
    @Volatile private var route: List<RoutePoint> = emptyList()

    private var tickerJob: Job? = null
    private var checkpointJob: Job? = null
    private var lastNotificationAt = 0L

    @Volatile private var strideCm = 70.0
    @Volatile private var units = Units.METRIC

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Android 14+ enforces the foreground-service-type prerequisite at startForeground() time.
        // A walk can legitimately run as type location, or as type health when ACTIVITY_RECOGNITION
        // is granted (step-only walk). With neither there is no legal type: stop before
        // startForeground instead of letting the system throw SecurityException on the main thread.
        if (!Permissions.hasLocation(this) && !Permissions.hasActivityRecognition(this)) {
            blocked = true
            stopSelf()
            return
        }
        Notifications.ensureChannels(this)
        locationManager = runCatching { getSystemService(LocationManager::class.java) }.getOrNull()
        sensorManager = runCatching { getSystemService(SensorManager::class.java) }.getOrNull()
        counterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        detectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // The foreground notification must exist immediately (5 s rule).
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (blocked) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_STOP -> {
                stopWalk(save = intent?.getBooleanExtra(EXTRA_DISCARD, false) != true)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                // A pending checkpoint means an interrupted walk: resume it. The Walk screen only
                // offers Start when no checkpoint is pending (resume/discard is a separate card), so
                // this never silently resurrects a walk the user chose to abandon.
                if (!running) beginWalk(loadCheckpoint())
                return START_STICKY
            }
            else -> {
                // START_STICKY restart without an intent: resume an interrupted walk if one was saved.
                val checkpoint = loadCheckpoint()
                if (checkpoint != null) {
                    beginWalk(checkpoint)
                    return START_STICKY
                }
                finishService()
                return START_NOT_STICKY
            }
        }
    }

    // region walk lifecycle

    private fun beginWalk(checkpoint: Checkpoint?) {
        if (running) return
        // A new walk may checkpoint again even right after delete-all reset the session.
        WalkTracker.allowCheckpoints()
        if (checkpoint != null) {
            startedAt = checkpoint.startedAt
            distanceMeters = checkpoint.distanceMeters
            steps = checkpoint.steps
            lastRaw = checkpoint.lastRaw
            lastLocation = checkpoint.lastLocation
            route = checkpoint.points
        } else {
            WalkTracker.deleteCheckpoint(this)
            startedAt = System.currentTimeMillis()
            distanceMeters = 0.0
            steps = 0
            lastRaw = Float.NaN
            lastLocation = null
            route = emptyList()
        }
        running = true
        gpsActive = startLocationUpdates()
        registerStepSensor()
        publishLive(resetTransientState = true)
        startTicker()
        startCheckpointing()
        updateNotification(force = true)
        // Read once for notification formatting and for the stride fallback distance.
        scope.launch {
            runCatching {
                val settings = app.container.settings.current()
                strideCm = StepMath.strideCm(settings)
                units = settings.units
            }
        }
    }

    private fun stopWalk(save: Boolean) {
        if (!running) {
            WalkTracker.deleteCheckpoint(this)
            if (!save) WalkTracker.reset()
            finishService()
            return
        }
        running = false
        tickerJob?.cancel()
        checkpointJob?.cancel()
        stopLocationUpdates()
        unregisterStepSensor()

        val elapsed = elapsedSec()
        val endedAt = System.currentTimeMillis()
        var finalDistance = distanceMeters
        if (finalDistance <= 0.0 && steps > 0) {
            // No GPS: fall back to the stride estimate (honest, labelled as estimate in the UI).
            finalDistance = StepMath.distanceMeters(steps, strideCm)
        }
        val pace = if (finalDistance >= MIN_DISTANCE_FOR_PACE_M) {
            (elapsed * 1000.0 / finalDistance).roundToInt()
        } else {
            0
        }
        val finalRoute = route
        WalkTracker.update {
            it.copy(
                running = false,
                saving = save,
                elapsedSec = elapsed,
                distanceMeters = finalDistance,
                steps = steps,
                secondsPerKm = pace,
                route = finalRoute,
                gpsActive = gpsActive,
            )
        }
        if (!save) {
            // Discard: no Walk row is written and no checkpoint survives, History stays clean.
            WalkTracker.deleteCheckpoint(this)
            WalkTracker.reset()
            finishService()
            return
        }
        scope.launch {
            val walk = Walk(
                startedAt = startedAt,
                endedAt = endedAt,
                durationSec = elapsed,
                steps = steps,
                distanceMeters = finalDistance,
                secondsPerKm = pace,
                route = finalRoute,
            )
            val id = runCatching { app.container.walks.save(walk) }.getOrNull()
            if (id == null) {
                WalkTracker.update {
                    it.copy(saving = false, savedWalkId = null, error = getString(R.string.sensor_walk_save_failed))
                }
            } else {
                WalkTracker.deleteCheckpoint(this@WalkTrackingService)
                WalkTracker.update { it.copy(saving = false, savedWalkId = id, error = null) }
            }
            withContext(Dispatchers.Main) { finishService() }
        }
    }

    private fun finishService() {
        stopLocationUpdates()
        unregisterStepSensor()
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    // endregion

    // region location

    private fun startLocationUpdates(): Boolean {
        val manager = locationManager ?: return false
        if (!Permissions.hasLocation(this)) return false
        val providers = ArrayList<String>(2)
        if (isProviderEnabled(manager, LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
        if (isProviderEnabled(manager, LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
        if (providers.isEmpty()) return false
        var registeredAtLeastOne = false
        providers.forEach { provider ->
            val ok =
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    runCatching {
                        manager.requestLocationUpdates(
                            provider,
                            LOCATION_INTERVAL_MS,
                            LOCATION_MIN_DISTANCE_M,
                            this,
                            Looper.getMainLooper(),
                        )
                        true
                    }.getOrDefault(false)
                } else {
                    // Approximate-only grants cannot feed the route; the walk still counts steps.
                    false
                }
            registeredAtLeastOne = registeredAtLeastOne || ok
        }
        locationRegistered = registeredAtLeastOne
        return registeredAtLeastOne
    }

    private fun stopLocationUpdates() {
        if (!locationRegistered) return
        locationRegistered = false
        runCatching { locationManager?.removeUpdates(this) }
    }

    private fun isProviderEnabled(manager: LocationManager, provider: String): Boolean =
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)

    override fun onLocationChanged(location: Location) {
        if (!running) return
        if (location.latitude.isNaN() || location.longitude.isNaN()) return
        if (location.hasAccuracy() && location.accuracy > LOCATION_MAX_ACCURACY_M) return

        val previous = lastLocation
        if (previous == null) {
            lastLocation = location
            gpsActive = true
            route = route + RoutePoint(location.latitude, location.longitude, location.timeOrNow())
            publishLive()
            return
        }
        val moved = haversineMeters(previous.latitude, previous.longitude, location.latitude, location.longitude)
        if (moved < LOCATION_MIN_STEP_M) return
        val deltaSeconds = ((location.time - previous.time).coerceAtLeast(1L)) / 1000.0
        if (moved / deltaSeconds > LOCATION_MAX_SPEED_MPS) return // GPS jump: ignore it
        distanceMeters += moved
        lastLocation = location
        gpsActive = true
        route = route + RoutePoint(location.latitude, location.longitude, location.timeOrNow())
        publishLive()
        if (System.currentTimeMillis() - lastNotificationAt >= NOTIFICATION_REFRESH_MS) updateNotification()
    }

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    // endregion

    // region step sensor (walk delta only)

    /**
     * Registers the best source this phone reports for walk step deltas: the hardware counter, then
     * the hardware detector, then a labelled accelerometer estimate. Walk distance without GPS is
     * already a stride estimate, so the walk UI's wording still says "estimate" for it.
     */
    private fun registerStepSensor() {
        val manager = sensorManager ?: return
        if (stepRegistered) return
        if (!Permissions.hasActivityRecognition(this)) return
        val (target, sensor) = when {
            counterSensor != null -> StepSource.COUNTER to counterSensor
            detectorSensor != null -> StepSource.DETECTOR to detectorSensor
            accelerometerSensor != null -> StepSource.MOTION_ESTIMATE to accelerometerSensor
            else -> return
        }
        val chosen = sensor ?: return
        val sampling = if (target == StepSource.MOTION_ESTIMATE) {
            SensorManager.SENSOR_DELAY_UI
        } else {
            SensorManager.SENSOR_DELAY_NORMAL
        }
        stepRegistered = runCatching {
            manager.registerListener(this, chosen, sampling)
        }.getOrDefault(false)
        if (!stepRegistered) return
        stepSource = target
        when (target) {
            StepSource.DETECTOR -> {
                detectorCumulative = 0f
                lastRaw = Float.NaN
            }
            StepSource.MOTION_ESTIMATE -> {
                motionDetector.reset()
                lastRaw = Float.NaN
            }
            else -> Unit
        }
    }

    private fun unregisterStepSensor() {
        if (!stepRegistered) return
        stepRegistered = false
        runCatching { sensorManager?.unregisterListener(this) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (!running || e.values.isEmpty()) return
        when (e.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                if (stepSource != StepSource.COUNTER) return
                val raw = e.values[0]
                if (raw.isNaN() || raw.isInfinite()) return
                if (lastRaw.isNaN() || raw < lastRaw - 1f) {
                    lastRaw = raw
                    return
                }
                val delta = (raw - lastRaw).toInt()
                if (delta <= 0) return
                lastRaw = raw
                steps += delta
                publishLive()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (stepSource != StepSource.DETECTOR) return
                val detected = e.values[0].toInt().coerceAtLeast(1)
                detectorCumulative += detected
                steps += detected
                publishLive()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (stepSource != StepSource.MOTION_ESTIMATE) return
                val magnitude = kotlin.math.sqrt(
                    e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2],
                )
                val detected = motionDetector.onSample(magnitude, android.os.SystemClock.elapsedRealtime())
                if (detected <= 0) return
                steps += detected
                publishLive()
            }
            else -> Unit
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // endregion

    // region ticker, notification, state

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000L)
                if (!running) break
                publishLive()
                if (System.currentTimeMillis() - lastNotificationAt >= NOTIFICATION_REFRESH_MS) {
                    updateNotification()
                }
            }
        }
    }

    /**
     * Publishes the live state. The route is an immutable snapshot, so it is handed to the UI as-is;
     * [resetTransientState] clears the save/error fields at walk start.
     */
    private fun publishLive(resetTransientState: Boolean = false) {
        WalkTracker.update { live ->
            live.copy(
                running = running,
                startedAt = startedAt,
                elapsedSec = elapsedSec(),
                distanceMeters = distanceMeters,
                steps = steps,
                secondsPerKm = paceSecPerKm(),
                route = route,
                gpsActive = gpsActive,
                saving = if (resetTransientState) false else live.saving,
                savedWalkId = if (resetTransientState) null else live.savedWalkId,
                error = if (resetTransientState) null else live.error,
            )
        }
    }

    /**
     * Puts the service in the foreground with a type whose Android 14+ runtime prerequisite is
     * actually granted: location when present, otherwise health with ACTIVITY_RECOGNITION (guarded
     * in [onCreate]). A refusal is a permission race, never a crash.
     */
    private fun startInForeground() {
        if (foregroundStarted) return
        Notifications.ensureChannels(this)
        val type = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> 0
            Permissions.hasLocation(this) -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Permissions.hasActivityRecognition(this) ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            else -> 0
        }
        val started = runCatching {
            ServiceCompat.startForeground(this, Notifications.ID_WALK, buildNotification(), type)
        }.isSuccess
        if (!started) {
            blocked = true
            stopSelf()
            return
        }
        foregroundStarted = true
        lastNotificationAt = System.currentTimeMillis()
    }

    private fun updateNotification(force: Boolean = false) {
        if (!foregroundStarted) return
        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationAt < NOTIFICATION_REFRESH_MS) return
        lastNotificationAt = now
        Notifications.post(this, Notifications.ID_WALK, buildNotification())
    }

    private fun buildNotification(): Notification {
        val localized = AppLocale.wrap(this)
        val title = localized.getString(R.string.sensor_notif_walk_title)
        val text = if (running) {
            localized.getString(
                R.string.sensor_notif_walk_progress,
                StepMath.formatDuration(elapsedSec().toLong()),
                StepMath.formatDistance(distanceMeters, units),
            )
        } else {
            localized.getString(R.string.sensor_notif_walk_preparing)
        }
        return Notifications.walkNotification(this, title, text, stopPendingIntent())
    }

    private fun stopPendingIntent(): PendingIntent {
        val intent = Intent(this, WalkTrackingService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getForegroundService(
            this,
            REQUEST_STOP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // endregion

    // region checkpoint

    private fun startCheckpointing() {
        checkpointJob?.cancel()
        checkpointJob = scope.launch {
            while (isActive) {
                delay(CHECKPOINT_INTERVAL_MS)
                if (!running) break
                saveCheckpoint()
            }
        }
    }

    /**
     * Serializes the walk from immutable/volatile snapshots. Runs on Dispatchers.Default: a location
     * fix arriving mid-write publishes a new route list, it can never mutate the one being written.
     * Every failure is swallowed on purpose - a missed checkpoint must never kill the walk.
     */
    private fun saveCheckpoint(force: Boolean = false) {
        if (!running && !force) return
        runCatching {
            val snapshotRoute = route
            val payload = JSONObject()
                .put("version", 1)
                .put("startedAt", startedAt)
                .put("distanceMeters", distanceMeters)
                .put("steps", steps)
                .put("lastRaw", if (lastRaw.isNaN()) JSONObject.NULL else lastRaw.toDouble())
                .put("lastLat", lastLocation?.let { it.latitude } ?: JSONObject.NULL)
                .put("lastLng", lastLocation?.let { it.longitude } ?: JSONObject.NULL)
                .put("lastTime", lastLocation?.time ?: 0L)
            val points = JSONArray()
            snapshotRoute.forEach { point ->
                points.put(JSONObject().put("lat", point.lat).put("lng", point.lng).put("t", point.t))
            }
            payload.put("points", points)
            val target = WalkTracker.checkpointFile(this)
            val tmp = File(target.parentFile, target.name + CHECKPOINT_TMP_SUFFIX)
            tmp.writeText(payload.toString())
            if (!tmp.renameTo(target)) {
                target.writeText(payload.toString())
                tmp.delete()
            }
        }
    }

    private fun loadCheckpoint(): Checkpoint? {
        val file = WalkTracker.checkpointFile(this)
        if (!file.exists()) return null
        return runCatching {
            val payload = JSONObject(file.readText())
            val pointsArray = payload.optJSONArray("points") ?: JSONArray()
            val points = buildList {
                for (index in 0 until pointsArray.length()) {
                    val point = pointsArray.getJSONObject(index)
                    add(RoutePoint(point.getDouble("lat"), point.getDouble("lng"), point.optLong("t")))
                }
            }
            val lastLat = if (payload.isNull("lastLat")) Double.NaN else payload.optDouble("lastLat", Double.NaN)
            val lastLng = if (payload.isNull("lastLng")) Double.NaN else payload.optDouble("lastLng", Double.NaN)
            val savedLocation = if (lastLat.isNaN() || lastLng.isNaN()) {
                null
            } else {
                Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = lastLat
                    longitude = lastLng
                    time = payload.optLong("lastTime", 0L)
                    accuracy = LOCATION_MAX_ACCURACY_M
                }
            }
            Checkpoint(
                startedAt = payload.getLong("startedAt"),
                distanceMeters = payload.optDouble("distanceMeters", 0.0),
                steps = payload.optInt("steps", 0),
                lastRaw = if (payload.isNull("lastRaw")) Float.NaN else payload.optDouble("lastRaw", Double.NaN).toFloat(),
                lastLocation = savedLocation,
                points = points,
            )
        }.getOrNull()
    }

    private data class Checkpoint(
        val startedAt: Long,
        val distanceMeters: Double,
        val steps: Int,
        val lastRaw: Float,
        val lastLocation: Location?,
        val points: List<RoutePoint>,
    )

    // endregion

    // region helpers

    private fun elapsedSec(): Int {
        if (startedAt <= 0L) return 0
        return ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L).toInt()
    }

    private fun paceSecPerKm(): Int {
        val elapsed = elapsedSec()
        if (elapsed <= 0 || distanceMeters < MIN_DISTANCE_FOR_PACE_M) return 0
        return (elapsed * 1000.0 / distanceMeters).roundToInt()
    }

    private fun Location.timeOrNow(): Long = if (time > 0L) time else System.currentTimeMillis()

    override fun onDestroy() {
        val wasRunning = running
        running = false
        tickerJob?.cancel()
        checkpointJob?.cancel()
        stopLocationUpdates()
        unregisterStepSensor()
        if (wasRunning) {
            if (WalkTracker.isCheckpointSuppressed()) {
                // Delete-all stopped the service after dropping the session: never resurrect the
                // checkpoint it just wiped.
                WalkTracker.deleteCheckpoint(this)
            } else {
                // Bounded synchronous flush on teardown: a system kill of the service loses at most
                // the current moment, not the last 20 s checkpoint window. If this returns late the
                // last good checkpoint is still on disk, so the walk stays resumable either way.
                runBlocking {
                    withTimeoutOrNull(CHECKPOINT_FLUSH_TIMEOUT_MS) { saveCheckpoint(force = true) }
                }
            }
        }
        scope.cancel()
        WalkTracker.update { it.copy(running = false) }
        super.onDestroy()
    }

    // endregion

    companion object {
        const val ACTION_START = "com.sabeel.steps.action.WALK_START"
        const val ACTION_STOP = "com.sabeel.steps.action.WALK_STOP"

        /** Boolean extra on [ACTION_STOP]: true = discard the walk instead of saving it. */
        private const val EXTRA_DISCARD = "com.sabeel.steps.extra.WALK_DISCARD"
        private const val REQUEST_STOP = 7101
        private const val CHECKPOINT_INTERVAL_MS = 20_000L
        private const val CHECKPOINT_FLUSH_TIMEOUT_MS = 1_500L
        private const val CHECKPOINT_TMP_SUFFIX = ".tmp"
        private const val NOTIFICATION_REFRESH_MS = 5_000L
        private const val LOCATION_INTERVAL_MS = 3_000L
        private const val LOCATION_MIN_DISTANCE_M = 5f
        private const val LOCATION_MAX_ACCURACY_M = 65f
        private const val LOCATION_MIN_STEP_M = 1.0
        private const val LOCATION_MAX_SPEED_MPS = 12.0
        private const val MIN_DISTANCE_FOR_PACE_M = 50.0

        /**
         * Starts a walk only when it can legally run.
         *
         * @return false when neither location nor ACTIVITY_RECOGNITION is granted: with neither
         * permission there is no foreground-service type the app may use (the health fallback needs
         * ACTIVITY_RECOGNITION on Android 14+ and the location type needs a location permission),
         * so the caller shows the blocked message instead of the process crashing.
         */
        fun start(context: Context): Boolean {
            val appContext = context.applicationContext
            if (!Permissions.hasLocation(appContext) && !Permissions.hasActivityRecognition(appContext)) {
                return false
            }
            return runCatching {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, WalkTrackingService::class.java).setAction(ACTION_START),
                )
                true
            }.getOrDefault(false)
        }

        /** Asks the service to persist the walk and show the saved summary (save path). */
        fun stop(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.startService(
                    Intent(appContext, WalkTrackingService::class.java).setAction(ACTION_STOP),
                )
            }
        }

        /**
         * Ends the current walk WITHOUT saving it: the ACTION_STOP is tagged with [EXTRA_DISCARD],
         * the checkpoint is deleted and [WalkTracker] is reset. A later walk starts clean.
         */
        fun discard(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.startService(
                    Intent(appContext, WalkTrackingService::class.java)
                        .setAction(ACTION_STOP)
                        .putExtra(EXTRA_DISCARD, true),
                )
            }
        }

        /**
         * Deletes the interrupted-walk checkpoint and resets the live process state (delete-all and
         * the Walk screen's "discard pending" action). Does not touch the database.
         */
        fun resetSession(context: Context) {
            WalkTracker.resetSession(context)
        }
    }
}

/** Great-circle distance in metres. */
private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * earthRadiusMeters * asin(min(1.0, sqrt(a)))
}
