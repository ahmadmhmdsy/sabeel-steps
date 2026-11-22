package com.sabeel.steps.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.domain.session.Place
import com.sabeel.steps.domain.session.PlaceMath
import com.sabeel.steps.domain.session.ProjectedLeg
import com.sabeel.steps.domain.session.SessionGoal
import com.sabeel.steps.domain.session.SessionProjection
import com.sabeel.steps.domain.session.SessionProjectionState
import com.sabeel.steps.domain.session.SessionStatus
import com.sabeel.steps.domain.session.WalkLeg
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
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
 * Foreground service for one tracked custom session (v1.3) without any map SDK, network call or
 * Play Services dependency:
 *  - steps are never counted here. The global step count (StepCounterService / StepsRepository) is
 *    the single source; this service only projects the difference since the session baseline with
 *    [SessionProjection] and never writes a daily row (council amendment A7);
 *  - LocationManager updates (GPS, network fallback) every ~3 s / 5 m feed distance and route while
 *    the session runs and GPS was requested;
 *  - goal progress comes from the pure [SessionGoal] engine; a PLACE goal uses the locally saved
 *    place radius through [PlaceMath] with no geocoding and no network (amendment A10);
 *  - [WalkTracker] is the live state the Walk screen collects (this service is the only writer);
 *  - a version 2 checkpoint stores the config, projection, closed legs and paused state every 20 s,
 *    so a process death keeps the session resumable. Version 1 checkpoints are still read tolerantly
 *    (their old own-counted steps field is ignored on purpose: the projection owns steps now).
 *
 * Concurrency contract: every mutable field the checkpoint writer reads is @Volatile and the route
 * is an immutable List replaced (never mutated) on each accepted fix, so the writer can serialize a
 * consistent snapshot from Dispatchers.Default while location callbacks run on the main thread. A
 * bounded flush runs in [onDestroy] so a teardown loses at most the current moment.
 *
 * With no location permission the session runs step-only and reports gpsActive = false; with neither
 * location nor activity perception permission the service cannot legally run at all and [start]
 * returns false instead of starting a foreground service without a permitted type.
 */
class WalkTrackingService : Service(), LocationListener {

    private val app: SabeelApp get() = application as SabeelApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var locationManager: LocationManager? = null
    private var locationRegistered = false
    private var foregroundStarted = false
    private var blocked = false

    // Written on the service main thread, read by the projection/checkpoint coroutines: @Volatile.
    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var pausedAt = 0L
    @Volatile private var totalPausedMs = 0L
    @Volatile private var startedAt = 0L
    @Volatile private var gpsActive = false
    @Volatile private var distanceMeters = 0.0
    @Volatile private var sessionSteps = 0
    @Volatile private var lastLocation: Location? = null

    /**
     * Immutable route snapshot: a callback publishes "route = route + point", it never mutates a
     * shared ArrayList. The checkpoint writer only ever iterates a list that cannot change under it.
     */
    @Volatile private var route: List<RoutePoint> = emptyList()

    @Volatile private var config: SessionConfig = SessionConfig()
    @Volatile private var projection: SessionProjectionState = SessionProjection.start(0)
    @Volatile private var dayTotal = 0
    @Volatile private var dayEstimatedSteps = 0
    @Volatile private var goalCurrent = 0.0
    @Volatile private var goalReached = false
    @Volatile private var countingOff = false
    @Volatile private var endPlace: Place? = null

    private var tickerJob: Job? = null
    private var checkpointJob: Job? = null
    private var projectionJob: Job? = null
    private var lastNotificationAt = 0L

    /** One stop only: the notification action, the auto-stop coroutine and a user Stop can race. */
    private val stopLock = Any()
    @Volatile private var stopping = false

    @Volatile private var strideCm = 70.0
    @Volatile private var units = Units.METRIC

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Android 14+ enforces the foreground-service-type prerequisite at startForeground() time.
        // A session can legitimately run as type location, or as type health when ACTIVITY_RECOGNITION
        // is granted (step-only session). With neither there is no legal type: stop before
        // startForeground instead of letting the system throw SecurityException on the main thread.
        if (!Permissions.hasLocation(this) && !Permissions.hasActivityRecognition(this)) {
            blocked = true
            stopSelf()
            return
        }
        Notifications.ensureChannels(this)
        locationManager = runCatching { getSystemService(LocationManager::class.java) }.getOrNull()
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
            ACTION_PAUSE -> {
                if (running) pauseWalk() else finishService()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                if (running) resumeWalk() else finishService()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                // A pending checkpoint means an interrupted session: the legacy start() entry point
                // resumes it. The setup screen starts a NEW session and tags the Intent fresh, so that
                // path discards the pending checkpoint instead of resurrecting a session the user
                // chose to abandon (the Walk screen only offers Resume when it wants the checkpoint).
                if (!running) {
                    val fresh = intent?.getBooleanExtra(EXTRA_FRESH_SESSION, false) == true
                    beginWalk(if (fresh) null else loadCheckpoint(), SessionConfig.read(intent))
                }
                return START_STICKY
            }
            else -> {
                // START_STICKY restart without an intent: resume an interrupted session if one was saved.
                val checkpoint = loadCheckpoint()
                if (checkpoint != null) {
                    beginWalk(checkpoint, SessionConfig())
                    return START_STICKY
                }
                finishService()
                return START_NOT_STICKY
            }
        }
    }

    // region session lifecycle

    /**
     * Starts a fresh session or restores one from [checkpoint]. The checkpoint always wins over the
     * Intent config: an interrupted session keeps the activity, goal, GPS choice and places it was
     * started with, even when the restart comes from a bare sticky-service Intent.
     *
     * A new session captures the current global day total as the projection baseline before any
     * tick, so the invariant "session steps for the date <= global day total" is structurally true.
     */
    private fun beginWalk(checkpoint: Checkpoint?, intentConfig: SessionConfig) {
        if (running) return
        // A new session may checkpoint again even right after delete-all reset the session.
        WalkTracker.allowCheckpoints()
        config = checkpoint?.config ?: intentConfig
        endPlace = null
        if (checkpoint != null) {
            startedAt = checkpoint.startedAt
            distanceMeters = checkpoint.distanceMeters
            lastLocation = checkpoint.lastLocation
            route = checkpoint.points
            paused = checkpoint.paused
            pausedAt = checkpoint.pausedAt
            totalPausedMs = checkpoint.totalPausedMs
            goalReached = checkpoint.goalReached
            dayTotal = checkpoint.dayTotal
            dayEstimatedSteps = checkpoint.dayEstimatedSteps
            val restored = checkpoint.projection
            if (restored != null) {
                projection = restored
                sessionSteps = SessionProjection.totalSteps(restored, dayTotal)
            } else {
                // A version 1 checkpoint has no projection. Beginning one at the current global total is
                // the only honest restore: the old own-counted number is not comparable to a projection,
                // and inventing it would double count the steps the global count already holds.
                val snapshot = readTodaySnapshot()
                dayTotal = snapshot.first
                dayEstimatedSteps = snapshot.second
                projection = SessionProjection.start(dayTotal)
                sessionSteps = 0
            }
        } else {
            WalkTracker.deleteCheckpoint(this)
            startedAt = System.currentTimeMillis()
            distanceMeters = 0.0
            lastLocation = null
            route = emptyList()
            paused = false
            pausedAt = 0L
            totalPausedMs = 0L
            goalReached = false
            val snapshot = readTodaySnapshot()
            dayTotal = snapshot.first
            dayEstimatedSteps = snapshot.second
            projection = SessionProjection.start(dayTotal)
            sessionSteps = 0
        }
        countingOff = countingDisabled()
        running = true
        gpsActive = false
        // A restored paused session keeps frozen steps/elapsed but must still show real progress; the
        // regular evaluateGoal() call below only re-latches when the session is actually running.
        goalCurrent = SessionGoal.evaluate(
            type = config.goalType,
            target = config.goalValue,
            elapsedSec = activeElapsedMs() / 1000L,
            steps = sessionSteps,
            distanceMeters = distanceMeters,
            atPlace = isAtEndPlace(),
        ).current
        if (!paused) {
            if (config.useGps) gpsActive = startLocationUpdates()
            startProjection()
            evaluateGoal()
        }
        publishLive(resetTransientState = true)
        startTicker()
        startCheckpointing()
        updateNotification(force = true)
        // Read once for notification formatting and for the stride fallback distance.
        loadFormattingSettings()
        loadEndPlace()
    }

    /** Pauses: time, steps and distance freeze, GPS stops and no step can be attributed while paused. */
    private fun pauseWalk() {
        if (!running || paused) return
        paused = true
        pausedAt = System.currentTimeMillis()
        stopLocationUpdates()
        // Location updates are unregistered: the live card must not claim recording while paused.
        gpsActive = false
        projectionJob?.cancel()
        projectionJob = null
        publishLive()
        updateNotification(force = true)
        // Flush the paused state immediately: a process death while paused must not resume as running.
        scope.launch { saveCheckpoint(force = true) }
    }

    /**
     * Resumes a paused session. Steps taken while paused are delimited away by rebasing the overhead
     * projection on the current global day total, and closed midnight legs are kept so earlier days
     * stay attributed. The elapsed clock continues from where it froze (paused time is not elapsed).
     */
    private fun resumeWalk() {
        if (!running || !paused) return
        val now = System.currentTimeMillis()
        totalPausedMs += (now - pausedAt).coerceAtLeast(0L)
        paused = false
        pausedAt = 0L
        val snapshot = readTodaySnapshot()
        dayTotal = snapshot.first
        dayEstimatedSteps = snapshot.second
        // Preserve the steps taken before the pause: the projection baseline must move past steps
        // that happened while paused (they belong to the global day, not this session), but the
        // session total must not lose its earlier segment. Close that segment as its own dated leg
        // and start a fresh baseline on top of the current global total; same-date segments are
        // merged when the session is saved.
        val legsBeforePause = projection.legs.sumOf { it.steps.toLong() }
        val pausedSegment = (sessionSteps - legsBeforePause).coerceAtLeast(0L).toInt()
        val carriedLegs =
            if (pausedSegment > 0 && projection.date.isNotEmpty()) {
                projection.legs + ProjectedLeg(date = projection.date, steps = pausedSegment)
            } else {
                projection.legs
            }
        projection = SessionProjection.start(dayTotal).copy(date = DateKey.today(), legs = carriedLegs)
        sessionSteps = SessionProjection.totalSteps(projection, dayTotal)
        gpsActive = if (config.useGps) startLocationUpdates() else false
        startProjection()
        evaluateGoal()
        publishLive()
        updateNotification(force = true)
        scope.launch { saveCheckpoint(force = true) }
    }

    private fun stopWalk(save: Boolean) {
        val alreadyStopping = synchronized(stopLock) {
            val was = stopping
            stopping = true
            was
        }
        if (alreadyStopping) return
        if (!running) {
            WalkTracker.deleteCheckpoint(this)
            if (!save) WalkTracker.reset()
            finishService()
            return
        }
        val wasPaused = paused
        // The active clock is read while the pause flag is still set, so a paused session stops at the
        // frozen instant and never swallows the pause duration.
        val elapsedMs = activeElapsedMs()
        val now = System.currentTimeMillis()
        val cfg = config
        running = false
        paused = false
        tickerJob?.cancel()
        checkpointJob?.cancel()
        projectionJob?.cancel()
        stopLocationUpdates()

        // One last projection tick so the saved session and its closing leg include every counted step.
        // While paused the projection was already frozen at the pause moment and its steps must stay
        // out of the session, so no fresh global total is applied here.
        var finalDayTotal = dayTotal
        var finalState = projection
        if (!wasPaused) {
            val snapshot = readTodaySnapshot()
            finalDayTotal = snapshot.first
            dayEstimatedSteps = snapshot.second
            finalState = SessionProjection.update(projection, finalDayTotal, DateKey.today(), now).state
        }
        projection = finalState
        if (finalDayTotal < 0) finalDayTotal = 0
        val closingLeg = SessionProjection.closingLeg(finalState, finalDayTotal)
        val finalLegs = finalState.legs + listOfNotNull(closingLeg)
        val finalSteps = SessionProjection.totalSteps(finalState, finalDayTotal)
        val elapsed = (elapsedMs / 1000L).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        var finalDistance = distanceMeters
        if (finalDistance <= 0.0 && finalSteps > 0) {
            // No GPS: fall back to the stride estimate (honest, labelled as an estimate in the UI).
            finalDistance = StepMath.distanceMeters(finalSteps, strideCm)
        }
        val pace = if (finalDistance >= MIN_DISTANCE_FOR_PACE_M) {
            (elapsed * 1000.0 / finalDistance).roundToInt()
        } else {
            0
        }
        val finalRoute = route
        val finalGpsActive = gpsActive
        // Final goal evaluation: a step taken just before Stop must still be able to meet the target.
        if (!goalReached) {
            val progress = SessionGoal.evaluate(
                type = cfg.goalType,
                target = cfg.goalValue,
                elapsedSec = elapsed.toLong(),
                steps = finalSteps,
                distanceMeters = finalDistance,
                atPlace = isAtEndPlace(),
            )
            goalCurrent = progress.current
            goalReached = progress.reached
        }
        val targetMet = goalReached
        // Merge same-date segments produced by pause/resume: the schema promises one leg per local
        // date, while a pause closes the current date's earlier segment on purpose.
        val liveLegs = finalLegs
            .groupBy { it.date }
            .map { (date, segments) -> WalkLeg(date = date, steps = segments.sumOf { it.steps }) }
        val estimated = dayEstimatedSteps > 0
        WalkTracker.update {
            it.copy(
                running = false,
                savedWalkId = null,
                saving = save,
                elapsedSec = elapsed,
                distanceMeters = finalDistance,
                steps = finalSteps,
                secondsPerKm = pace,
                route = finalRoute,
                gpsActive = finalGpsActive,
                estimated = estimated,
                paused = false,
                pausedAt = 0L,
                goalCurrent = goalCurrent,
                goalReached = targetMet,
                sessionDate = finalState.date,
                legs = liveLegs,
                error = null,
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
                endedAt = now,
                durationSec = elapsed,
                steps = finalSteps,
                distanceMeters = finalDistance,
                secondsPerKm = pace,
                route = finalRoute,
                estimated = estimated,
                activityType = cfg.activityType,
                goalType = cfg.goalType,
                goalValue = cfg.goalValue,
                targetMet = targetMet,
                status = SessionStatus.COMPLETED,
                gpsUsed = finalGpsActive || finalRoute.isNotEmpty(),
                startPlaceId = cfg.startPlaceId,
                endPlaceId = cfg.endPlaceId,
                sessionPresetId = cfg.presetId,
            )
            val id = runCatching { app.container.walks.insertSession(walk, liveLegs) }.getOrNull()
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
        projectionJob?.cancel()
        stopLocationUpdates()
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    // endregion

    // region projection and goal

    private fun startProjection() {
        projectionJob?.cancel()
        projectionJob = scope.launch {
            runCatching {
                app.container.steps.todayFlow().collect { row ->
                    if (!running || paused) return@collect
                    onGlobalDayTotal(row?.steps ?: 0, row?.estimatedSteps ?: 0)
                }
            }
        }
    }

    /** One projection tick: recompute the session slice of [total], then re-evaluate the goal. */
    private fun onGlobalDayTotal(total: Int, estimatedSteps: Int) {
        if (!running || paused) return
        dayTotal = total
        dayEstimatedSteps = estimatedSteps
        val update = SessionProjection.update(projection, total, DateKey.today(), System.currentTimeMillis())
        projection = update.state
        sessionSteps = SessionProjection.totalSteps(update.state, total)
        evaluateGoal()
        publishLive()
    }

    /**
     * Evaluates the configured goal through the pure [SessionGoal] engine and latches the first
     * reach. The latch is a caller concern by contract, so the alert and the optional auto-stop
     * happen once per session even though this method runs on every tick.
     */
    private fun evaluateGoal() {
        val cfg = config
        val progress = SessionGoal.evaluate(
            type = cfg.goalType,
            target = cfg.goalValue,
            elapsedSec = activeElapsedMs() / 1000L,
            steps = sessionSteps,
            distanceMeters = distanceMeters,
            atPlace = isAtEndPlace(),
        )
        goalCurrent = progress.current
        if (!progress.reached || goalReached) return
        goalReached = true
        // One-shot system notification (goal channel), plus the foreground notification's reached
        // marker. The latch above is checkpointed, so a process restart mid-session does not re-alert.
        Notifications.notifySessionGoalReached(this)
        updateNotification(force = true)
        if (cfg.autoStop) {
            scope.launch { stopWalk(save = true) }
        }
    }

    /** True when the current fix is inside the goal end place radius (PLACE goals only). */
    private fun isAtEndPlace(): Boolean {
        if (config.goalType != GoalType.PLACE) return false
        val place = endPlace ?: return false
        val fix = lastLocation ?: return false
        return PlaceMath.isInside(place, fix.latitude, fix.longitude)
    }

    /** Loads the goal end place once per session; a dangling id simply means "not reachable". */
    private fun loadEndPlace() {
        val placeId = config.endPlaceId ?: return
        scope.launch {
            runCatching {
                val place = app.container.walks.places().firstOrNull { it.id == placeId }
                endPlace = place
                if (place != null && config.goalType == GoalType.PLACE) {
                    evaluateGoal()
                    publishLive()
                    updateNotification(force = true)
                }
            }
        }
    }

    /**
     * The global day total the session is projected onto. Room answers from an in-memory flow quickly;
     * the timeout exists only so a stuck query can never block the service command path, and 0 is the
     * honest fallback (the session then projects from zero rather than inventing a baseline).
     */
    private fun readTodaySnapshot(): Pair<Int, Int> = runCatching {
        runBlocking {
            withTimeoutOrNull(GLOBAL_TOTAL_TIMEOUT_MS) {
                app.container.steps.todayFlow().first()?.let { row -> row.steps to row.estimatedSteps }
            }
        }
    }.getOrNull() ?: (0 to 0)

    private fun countingDisabled(): Boolean =
        runCatching { !app.container.counting.current().enabled }.getOrDefault(false)

    private fun loadFormattingSettings() {
        scope.launch {
            runCatching {
                val settings = app.container.settings.current()
                strideCm = StepMath.strideCm(settings)
                units = settings.units
            }
        }
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
                    // Approximate-only grants cannot feed the route; the session still counts steps.
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
            evaluateGoal()
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
        evaluateGoal()
        publishLive()
        if (System.currentTimeMillis() - lastNotificationAt >= NOTIFICATION_REFRESH_MS) updateNotification()
    }

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    // LocationListener.onStatusChanged is deprecated in the platform API but is still part of the
    // interface this service implements; the callback body is intentionally empty.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    // endregion

    // region ticker, notification, state

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000L)
                if (!running) break
                countingOff = countingDisabled()
                if (!paused) evaluateGoal()
                publishLive()
                if (System.currentTimeMillis() - lastNotificationAt >= NOTIFICATION_REFRESH_MS) {
                    updateNotification()
                }
            }
        }
    }

    /**
     * Publishes the live session state. The route and the legs are immutable snapshots, so they are
     * handed to the UI as-is; [resetTransientState] clears the save/error fields when a session begins.
     */
    private fun publishLive(resetTransientState: Boolean = false) {
        val liveLegs = projection.legs.map { leg -> WalkLeg(date = leg.date, steps = leg.steps) }
        WalkTracker.update { live ->
            live.copy(
                running = running,
                startedAt = startedAt,
                elapsedSec = elapsedSec(),
                distanceMeters = distanceMeters,
                steps = sessionSteps,
                estimated = dayEstimatedSteps > 0,
                secondsPerKm = paceSecPerKm(),
                route = route,
                gpsActive = gpsActive,
                saving = if (resetTransientState) false else live.saving,
                savedWalkId = if (resetTransientState) null else live.savedWalkId,
                error = if (resetTransientState) null else live.error,
                activityType = config.activityType,
                goalType = config.goalType,
                goalValue = config.goalValue,
                goalCurrent = goalCurrent,
                goalReached = goalReached,
                paused = paused,
                pausedAt = pausedAt,
                countingOff = countingOff,
                sessionDate = projection.date,
                legs = liveLegs,
                startPlaceId = config.startPlaceId,
                endPlaceId = config.endPlaceId,
                presetId = config.presetId,
                autoStop = config.autoStop,
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

    /**
     * Notification text: activity, elapsed time and distance, the session step count and (for a real
     * goal) numeric progress in the goal own unit. Numbers are language-neutral and built from the
     * existing platform strings; localized activity labels and a dedicated goal copy belong to the UI
     * string layer and are not invented here.
     */
    private fun buildNotification(): Notification {
        val localized = AppLocale.wrap(this)
        val title = localized.getString(R.string.sensor_notif_walk_title)
        val cfg = config
        val text = if (running) {
            val parts = ArrayList<String>(4)
            parts.add(activityLabel(localized, cfg.activityType))
            parts.add(
                localized.getString(
                    R.string.sensor_notif_walk_progress,
                    StepMath.formatDuration(activeElapsedMs() / 1000L),
                    StepMath.formatDistance(distanceMeters, units),
                ),
            )
            parts.add(localized.getString(R.string.goal_value, StepMath.formatSteps(sessionSteps)))
            goalLabel(localized, cfg)?.let { label -> parts.add(label) }
            if (paused) parts.add(localized.getString(R.string.session_paused_label))
            parts.joinToString(TEXT_JOINER)
        } else {
            localized.getString(R.string.sensor_notif_walk_preparing)
        }
        return Notifications.walkNotification(
            context = this,
            title = title,
            text = text,
            stopAction = stopPendingIntent(),
            pauseAction = pausePendingIntent(),
            paused = paused,
        )
    }

    /** Localized activity label for the notification (shared with the session setup screen copy). */
    private fun activityLabel(localized: Context, activity: ActivityType): String =
        when (activity) {
            ActivityType.WALK -> localized.getString(R.string.session_activity_walk)
            ActivityType.RUN -> localized.getString(R.string.session_activity_run)
        }

    /**
     * Localized goal progress in the goal own unit, or null when the session has no goal. A reached
     * measured goal keeps its numbers and gains the check mark; a reached PLACE goal says so.
     */
    private fun goalLabel(localized: Context, cfg: SessionConfig): String? {
        val current = goalCurrent
        val target = cfg.goalValue
        return when (cfg.goalType) {
            GoalType.NONE -> null
            GoalType.STEPS -> progressLabel(
                localized,
                StepMath.formatSteps(current.toInt()),
                StepMath.formatSteps(target.toInt()),
            )
            GoalType.TIME -> progressLabel(
                localized,
                StepMath.formatDuration(current.toLong()),
                StepMath.formatDuration(target.toLong()),
            )
            GoalType.DISTANCE -> progressLabel(
                localized,
                StepMath.formatDistance(current, units),
                StepMath.formatDistance(target, units),
            )
            GoalType.PLACE -> if (goalReached) localized.getString(R.string.session_goal_met) else null
        }
    }

    private fun progressLabel(localized: Context, current: String, target: String): String {
        val label = localized.getString(R.string.session_goal_progress, current, target)
        return if (goalReached) label + " " + GOAL_REACHED_MARK else label
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

    /** Pause/Resume action: one request code, flag-updated, so it always targets the current state. */
    private fun pausePendingIntent(): PendingIntent {
        val action = if (paused) ACTION_RESUME else ACTION_PAUSE
        val intent = Intent(this, WalkTrackingService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            this,
            REQUEST_PAUSE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // endregion

    // region checkpoint (version 2, tolerant of version 1)

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
     * Serializes the session from immutable/volatile snapshots. Runs on Dispatchers.Default: a location
     * fix arriving mid-write publishes a new route list, it can never mutate the one being written.
     * Every failure is swallowed on purpose - a missed checkpoint must never kill the session.
     */
    private fun saveCheckpoint(force: Boolean = false) {
        if (!running && !force) return
        runCatching {
            val snapshotRoute = route
            val snapshotProjection = projection
            val cfg = config
            val payload = JSONObject()
                .put("version", CHECKPOINT_VERSION)
                .put("startedAt", startedAt)
                .put("distanceMeters", distanceMeters)
                .put("paused", paused)
                .put("pausedAt", pausedAt)
                .put("totalPausedMs", totalPausedMs)
                .put("goalReached", goalReached)
                .put("gpsActive", gpsActive)
                .put("dayTotal", dayTotal)
                .put("dayEstimatedSteps", dayEstimatedSteps)
                .put("lastLat", lastLocation?.let { location -> location.latitude } ?: JSONObject.NULL)
                .put("lastLng", lastLocation?.let { location -> location.longitude } ?: JSONObject.NULL)
                .put("lastTime", lastLocation?.time ?: 0L)
                .put("config", configToJson(cfg))
                .put("projection", projectionToJson(snapshotProjection))
            val points = JSONArray()
            snapshotRoute.forEach { point ->
                points.put(JSONObject().put("lat", point.lat).put("lng", point.lng).put("t", point.t))
            }
            payload.put("points", points)
            val text = payload.toString()
            val target = WalkTracker.checkpointFile(this)
            val tmp = File(target.parentFile, target.name + CHECKPOINT_TMP_SUFFIX)
            tmp.writeText(text)
            if (!tmp.renameTo(target)) {
                target.writeText(text)
                tmp.delete()
            }
        }
    }

    /** Reads the checkpoint of either schema; version 1 fields that no longer exist are ignored. */
    private fun loadCheckpoint(): Checkpoint? {
        val file = WalkTracker.checkpointFile(this)
        if (!file.exists()) return null
        return runCatching {
            val payload = JSONObject(file.readText())
            val version = payload.optInt("version", 1)
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
            val modern = version >= CHECKPOINT_VERSION
            val restoredPaused = modern && payload.optBoolean("paused", false)
            Checkpoint(
                config = if (modern) configFromJson(payload.optJSONObject("config")) else SessionConfig(),
                projection = if (modern) projectionFromJson(payload.optJSONObject("projection")) else null,
                dayTotal = payload.optInt("dayTotal", 0),
                dayEstimatedSteps = payload.optInt("dayEstimatedSteps", 0),
                startedAt = payload.getLong("startedAt"),
                distanceMeters = payload.optDouble("distanceMeters", 0.0),
                gpsActive = payload.optBoolean("gpsActive", false),
                paused = restoredPaused,
                pausedAt = if (restoredPaused) payload.optLong("pausedAt", 0L) else 0L,
                totalPausedMs = payload.optLong("totalPausedMs", 0L),
                goalReached = payload.optBoolean("goalReached", false),
                lastLocation = savedLocation,
                points = points,
            )
        }.getOrNull()
    }

    private data class Checkpoint(
        val config: SessionConfig,
        val projection: SessionProjectionState?,
        val dayTotal: Int,
        val dayEstimatedSteps: Int,
        val startedAt: Long,
        val distanceMeters: Double,
        val gpsActive: Boolean,
        val paused: Boolean,
        val pausedAt: Long,
        val totalPausedMs: Long,
        val goalReached: Boolean,
        val lastLocation: Location?,
        val points: List<RoutePoint>,
    )

    private fun configToJson(cfg: SessionConfig): JSONObject = JSONObject()
        .put("activity", cfg.activityType.name)
        .put("goal", cfg.goalType.name)
        .put("goalValue", if (cfg.goalValue.isFinite()) cfg.goalValue else 0.0)
        .put("useGps", cfg.useGps)
        .put("startPlace", cfg.startPlaceId ?: SessionConfig.NO_ID)
        .put("endPlace", cfg.endPlaceId ?: SessionConfig.NO_ID)
        .put("preset", cfg.presetId ?: SessionConfig.NO_ID)
        .put("autoStop", cfg.autoStop)

    private fun configFromJson(json: JSONObject?): SessionConfig {
        if (json == null) return SessionConfig()
        return SessionConfig(
            activityType = ActivityType.fromStorage(json.optString("activity")),
            goalType = GoalType.fromStorage(json.optString("goal")),
            goalValue = json.optDouble("goalValue", 0.0),
            useGps = json.optBoolean("useGps", SessionConfig().useGps),
            startPlaceId = readJsonId(json, "startPlace"),
            endPlaceId = readJsonId(json, "endPlace"),
            presetId = readJsonId(json, "preset"),
            autoStop = json.optBoolean("autoStop", false),
        )
    }

    private fun readJsonId(json: JSONObject, key: String): Long? {
        val id = json.optLong(key, SessionConfig.NO_ID)
        return if (id > 0L) id else null
    }

    private fun projectionToJson(state: SessionProjectionState): JSONObject {
        val json = JSONObject()
            .put("baseline", state.baselineDayTotal)
            .put("date", state.date)
            .put("lastDayTotal", state.lastDayTotal)
            .put("lastUpdateAt", state.lastUpdateAt)
        val legs = JSONArray()
        state.legs.forEach { leg ->
            legs.put(JSONObject().put("date", leg.date).put("steps", leg.steps))
        }
        json.put("legs", legs)
        return json
    }

    private fun projectionFromJson(json: JSONObject?): SessionProjectionState? {
        if (json == null) return null
        val legs = ArrayList<ProjectedLeg>()
        val legsArray = json.optJSONArray("legs") ?: JSONArray()
        for (index in 0 until legsArray.length()) {
            val leg = legsArray.optJSONObject(index) ?: continue
            val date = leg.optString("date", "")
            if (date.isEmpty()) continue
            legs.add(ProjectedLeg(date = date, steps = leg.optInt("steps", 0)))
        }
        return SessionProjectionState(
            baselineDayTotal = json.optInt("baseline", 0),
            date = json.optString("date", ""),
            lastDayTotal = json.optInt("lastDayTotal", 0),
            lastUpdateAt = json.optLong("lastUpdateAt", 0L),
            legs = legs,
        )
    }

    // endregion

    // region helpers

    /** Active wall clock: frozen at the pause moment, excluding every paused span. */
    private fun activeElapsedMs(now: Long = System.currentTimeMillis()): Long {
        if (startedAt <= 0L) return 0L
        val stoppedAt = if (paused) pausedAt else now
        return (stoppedAt - startedAt - totalPausedMs).coerceAtLeast(0L)
    }

    private fun elapsedSec(): Int =
        (activeElapsedMs() / 1000L).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

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
        projectionJob?.cancel()
        stopLocationUpdates()
        if (wasRunning) {
            if (WalkTracker.isCheckpointSuppressed()) {
                // Delete-all stopped the service after dropping the session: never resurrect the
                // checkpoint it just wiped.
                WalkTracker.deleteCheckpoint(this)
            } else {
                // Bounded synchronous flush on teardown: a system kill of the service loses at most
                // the current moment, not the last 20 s checkpoint window. If this returns late the
                // last good checkpoint is still on disk, so the session stays resumable either way.
                runBlocking {
                    withTimeoutOrNull(CHECKPOINT_FLUSH_TIMEOUT_MS) { saveCheckpoint(force = true) }
                }
            }
        }
        scope.cancel()
        WalkTracker.update { it.copy(running = false, paused = false, pausedAt = 0L) }
        super.onDestroy()
    }

    // endregion

    companion object {
        const val ACTION_START = "com.sabeel.steps.action.WALK_START"
        const val ACTION_STOP = "com.sabeel.steps.action.WALK_STOP"
        const val ACTION_PAUSE = "com.sabeel.steps.action.WALK_PAUSE"
        const val ACTION_RESUME = "com.sabeel.steps.action.WALK_RESUME"

        /** Boolean extra on [ACTION_STOP]: true = discard the session instead of saving it. */
        private const val EXTRA_DISCARD = "com.sabeel.steps.extra.WALK_DISCARD"

        /** Boolean extra on [ACTION_START]: true = begin a fresh session and drop any checkpoint. */
        private const val EXTRA_FRESH_SESSION = "com.sabeel.steps.extra.WALK_FRESH"
        private const val REQUEST_STOP = 7101
        private const val REQUEST_PAUSE = 7102
        private const val CHECKPOINT_INTERVAL_MS = 20_000L
        private const val CHECKPOINT_FLUSH_TIMEOUT_MS = 1_500L
        private const val CHECKPOINT_TMP_SUFFIX = ".tmp"
        private const val CHECKPOINT_VERSION = 2
        private const val NOTIFICATION_REFRESH_MS = 5_000L
        private const val LOCATION_INTERVAL_MS = 3_000L
        private const val LOCATION_MIN_DISTANCE_M = 5f
        private const val LOCATION_MAX_ACCURACY_M = 65f
        private const val LOCATION_MIN_STEP_M = 1.0
        private const val LOCATION_MAX_SPEED_MPS = 12.0
        private const val MIN_DISTANCE_FOR_PACE_M = 50.0
        private const val GLOBAL_TOTAL_TIMEOUT_MS = 1_000L
        private const val GOAL_REACHED_MARK = "✓"
        private const val TEXT_JOINER = " · "

        /**
         * Starts a session with the legacy default setup only when it can legally run.
         *
         * @return false when neither location nor ACTIVITY_RECOGNITION is granted: with neither
         * permission there is no foreground-service type the app may use (the health fallback needs
         * ACTIVITY_RECOGNITION on Android 14+ and the location type needs a location permission),
         * so the caller shows the blocked message instead of the process crashing.
         */
        fun start(context: Context): Boolean = requestStart(context, SessionConfig(), fresh = false)

        /**
         * Starts a NEW session described by [config] and hands it to the service as Intent extras.
         * A pending checkpoint of an earlier interrupted session is discarded: the caller explicitly
         * asked for a new session, and resuming the old one would ignore this config.
         */
        fun start(context: Context, config: SessionConfig): Boolean =
            requestStart(context, config, fresh = true)

        private fun requestStart(context: Context, config: SessionConfig, fresh: Boolean): Boolean {
            val appContext = context.applicationContext
            if (!Permissions.hasLocation(appContext) && !Permissions.hasActivityRecognition(appContext)) {
                return false
            }
            return runCatching {
                val intent = Intent(appContext, WalkTrackingService::class.java).setAction(ACTION_START)
                SessionConfig.write(intent, config)
                intent.putExtra(EXTRA_FRESH_SESSION, fresh)
                ContextCompat.startForegroundService(appContext, intent)
                true
            }.getOrDefault(false)
        }

        /** Asks the running session to pause (freeze time/steps and stop GPS); harmless when idle. */
        fun pause(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.startService(
                    Intent(appContext, WalkTrackingService::class.java).setAction(ACTION_PAUSE),
                )
            }
        }

        /**
         * Asks the session to resume. A killed service is restarted as a foreground service first, which
         * is legal here because the caller is the visible app (a user action), never a boot receiver.
         */
        fun resume(context: Context) {
            val appContext = context.applicationContext
            if (!Permissions.hasLocation(appContext) && !Permissions.hasActivityRecognition(appContext)) {
                return
            }
            runCatching {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, WalkTrackingService::class.java).setAction(ACTION_RESUME),
                )
            }
        }

        /** Asks the service to persist the session and show the saved summary (save path). */
        fun stop(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.startService(
                    Intent(appContext, WalkTrackingService::class.java).setAction(ACTION_STOP),
                )
            }
        }

        /**
         * Ends the current session WITHOUT saving it: the ACTION_STOP is tagged with [EXTRA_DISCARD],
         * the checkpoint is deleted and [WalkTracker] is reset. A later session starts clean.
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
         * Deletes the interrupted-session checkpoint and resets the live process state (delete-all and
         * the Walk screen "discard pending" action). Does not touch the database.
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
