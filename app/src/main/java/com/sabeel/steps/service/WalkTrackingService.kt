package com.sabeel.steps.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * With no location permission the walk runs step-only and reports gpsActive = false.
 */
class WalkTrackingService : Service(), SensorEventListener, LocationListener {

    private val app: SabeelApp get() = application as SabeelApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepRegistered = false
    private var locationRegistered = false
    private var foregroundStarted = false

    private var running = false
    private var startedAt = 0L
    private var gpsActive = false
    private var distanceMeters = 0.0
    private var steps = 0
    private var lastRaw = Float.NaN
    private var lastLocation: Location? = null
    private val route = ArrayList<RoutePoint>()

    private var tickerJob: Job? = null
    private var checkpointJob: Job? = null
    private var lastNotificationAt = 0L

    @Volatile private var strideCm = 70.0
    @Volatile private var units = Units.METRIC

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        locationManager = getSystemService(LocationManager::class.java)
        sensorManager = getSystemService(SensorManager::class.java)
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        // The foreground notification must exist immediately (5 s rule).
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopWalk(save = intent?.getBooleanExtra(EXTRA_DISCARD, false) != true)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (!running) beginWalk(checkpoint = null)
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
        if (checkpoint != null) {
            startedAt = checkpoint.startedAt
            distanceMeters = checkpoint.distanceMeters
            steps = checkpoint.steps
            lastRaw = checkpoint.lastRaw
            lastLocation = checkpoint.lastLocation
            route.clear()
            route.addAll(checkpoint.points)
        } else {
            deleteCheckpoint()
            startedAt = System.currentTimeMillis()
            distanceMeters = 0.0
            steps = 0
            lastRaw = Float.NaN
            lastLocation = null
            route.clear()
        }
        running = true
        gpsActive = startLocationUpdates()
        registerStepSensor()
        WalkTracker.update {
            it.copy(
                running = true,
                startedAt = startedAt,
                elapsedSec = elapsedSec(),
                distanceMeters = distanceMeters,
                steps = steps,
                secondsPerKm = paceSecPerKm(),
                route = route.toList(),
                gpsActive = gpsActive,
                saving = false,
                savedWalkId = null,
                error = null,
            )
        }
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
            deleteCheckpoint()
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
        val finalRoute = route.toList()
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
            deleteCheckpoint()
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
                deleteCheckpoint()
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
        if (!Permissions.hasLocation(this)) return false
        val providers = ArrayList<String>(2)
        if (isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
        if (isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
        if (providers.isEmpty()) return false
        var registeredAtLeastOne = false
        providers.forEach { provider ->
            val ok = runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_INTERVAL_MS,
                    LOCATION_MIN_DISTANCE_M,
                    this,
                    Looper.getMainLooper(),
                )
                true
            }.getOrDefault(false)
            registeredAtLeastOne = registeredAtLeastOne || ok
        }
        locationRegistered = registeredAtLeastOne
        return registeredAtLeastOne
    }

    private fun stopLocationUpdates() {
        if (!locationRegistered) return
        locationRegistered = false
        runCatching { locationManager.removeUpdates(this) }
    }

    private fun isProviderEnabled(provider: String): Boolean =
        runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)

    override fun onLocationChanged(location: Location) {
        if (!running) return
        if (location.latitude.isNaN() || location.longitude.isNaN()) return
        if (location.hasAccuracy() && location.accuracy > LOCATION_MAX_ACCURACY_M) return

        val previous = lastLocation
        if (previous == null) {
            lastLocation = location
            gpsActive = true
            route.add(RoutePoint(location.latitude, location.longitude, location.timeOrNow()))
            publishLive(routeSnapshot = true)
            return
        }
        val moved = haversineMeters(previous.latitude, previous.longitude, location.latitude, location.longitude)
        if (moved < LOCATION_MIN_STEP_M) return
        val deltaSeconds = ((location.time - previous.time).coerceAtLeast(1L)) / 1000.0
        if (moved / deltaSeconds > LOCATION_MAX_SPEED_MPS) return // GPS jump: ignore it
        distanceMeters += moved
        lastLocation = location
        gpsActive = true
        route.add(RoutePoint(location.latitude, location.longitude, location.timeOrNow()))
        publishLive(routeSnapshot = true)
        if (System.currentTimeMillis() - lastNotificationAt >= NOTIFICATION_REFRESH_MS) updateNotification()
    }

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    // endregion

    // region step sensor (walk delta only)

    private fun registerStepSensor() {
        val sensor = stepSensor ?: return
        if (stepRegistered) return
        if (!Permissions.hasActivityRecognition(this)) return
        stepRegistered = runCatching {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }.getOrDefault(false)
    }

    private fun unregisterStepSensor() {
        if (!stepRegistered) return
        stepRegistered = false
        runCatching { sensorManager.unregisterListener(this) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (!running) return
        if (e.sensor.type != Sensor.TYPE_STEP_COUNTER || e.values.isEmpty()) return
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

    private fun publishLive(routeSnapshot: Boolean = false) {
        val snapshot = if (routeSnapshot) route.toList() else null
        WalkTracker.update { live ->
            live.copy(
                running = running,
                startedAt = startedAt,
                elapsedSec = elapsedSec(),
                distanceMeters = distanceMeters,
                steps = steps,
                secondsPerKm = paceSecPerKm(),
                route = snapshot ?: live.route,
                gpsActive = gpsActive,
            )
        }
    }

    private fun startInForeground() {
        if (foregroundStarted) return
        Notifications.ensureChannels(this)
        // A foreground service type is always supplied on API 30+. Location needs the runtime
        // permission; without it the walk is step-only and the service runs as type health.
        val type = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> 0
            Permissions.hasLocation(this) -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            else -> 0
        }
        ServiceCompat.startForeground(this, Notifications.ID_WALK, buildNotification(), type)
        foregroundStarted = true
        lastNotificationAt = System.currentTimeMillis()
    }

    private fun updateNotification(force: Boolean = false) {
        if (!foregroundStarted) return
        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationAt < NOTIFICATION_REFRESH_MS) return
        lastNotificationAt = now
        runCatching {
            NotificationManagerCompat.from(this).notify(Notifications.ID_WALK, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        val title = getString(R.string.sensor_notif_walk_title)
        val text = if (running) {
            getString(
                R.string.sensor_notif_walk_progress,
                StepMath.formatDuration(elapsedSec().toLong()),
                StepMath.formatDistance(distanceMeters, units),
            )
        } else {
            getString(R.string.sensor_notif_walk_preparing)
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

    private fun saveCheckpoint() {
        if (!running) return
        runCatching {
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
            route.forEach { point ->
                points.put(JSONObject().put("lat", point.lat).put("lng", point.lng).put("t", point.t))
            }
            payload.put("points", points)
            val target = checkpointFile()
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.writeText(payload.toString())
            if (!tmp.renameTo(target)) {
                target.writeText(payload.toString())
                tmp.delete()
            }
        }
    }

    private fun loadCheckpoint(): Checkpoint? {
        val file = checkpointFile()
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

    private fun deleteCheckpoint() {
        val target = checkpointFile()
        runCatching { target.delete() }
        runCatching { File(target.parentFile, target.name + ".tmp").delete() }
    }

    private fun checkpointFile(): File = File(filesDir, CHECKPOINT_FILE)

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
        running = false
        tickerJob?.cancel()
        checkpointJob?.cancel()
        stopLocationUpdates()
        unregisterStepSensor()
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
        private const val CHECKPOINT_FILE = "walk_checkpoint.json"
        private const val CHECKPOINT_INTERVAL_MS = 20_000L
        private const val NOTIFICATION_REFRESH_MS = 5_000L
        private const val LOCATION_INTERVAL_MS = 3_000L
        private const val LOCATION_MIN_DISTANCE_M = 5f
        private const val LOCATION_MAX_ACCURACY_M = 65f
        private const val LOCATION_MIN_STEP_M = 1.0
        private const val LOCATION_MAX_SPEED_MPS = 12.0
        private const val MIN_DISTANCE_FOR_PACE_M = 50.0

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WalkTrackingService::class.java).setAction(ACTION_START),
            )
        }

        /** Asks the service to persist the walk and show the saved summary (save path). */
        fun stop(context: Context) {
            context.startService(
                Intent(context, WalkTrackingService::class.java).setAction(ACTION_STOP),
            )
        }

        /**
         * Ends the current walk WITHOUT saving it: the ACTION_STOP is tagged with [EXTRA_DISCARD],
         * the checkpoint is deleted and [WalkTracker] is reset. A later walk starts clean.
         */
        fun discard(context: Context) {
            context.startService(
                Intent(context, WalkTrackingService::class.java)
                    .setAction(ACTION_STOP)
                    .putExtra(EXTRA_DISCARD, true),
            )
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