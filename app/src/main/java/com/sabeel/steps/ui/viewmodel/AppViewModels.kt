package com.sabeel.steps.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sabeel.steps.AppContainer
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.export.CsvIo
import com.sabeel.steps.data.export.SessionExportIo
import com.sabeel.steps.domain.AppLanguage
import com.sabeel.steps.domain.DailyStep
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.GoalAdvisor
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.UserSettings
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.domain.session.Place
import com.sabeel.steps.domain.session.SessionPreset
import com.sabeel.steps.platform.BootAttempt
import com.sabeel.steps.platform.CountingStore
import com.sabeel.steps.platform.HealthConnect
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.Reminders
import com.sabeel.steps.platform.StatusStore
import com.sabeel.steps.platform.StepSensors
import com.sabeel.steps.platform.StepServiceStatus
import com.sabeel.steps.platform.StepSourceStatus
import com.sabeel.steps.service.SessionConfig
import com.sabeel.steps.service.StepCounterService
import com.sabeel.steps.ui.i18n.AppLocale
import com.sabeel.steps.ui.screens.SavedWalkUi
import com.sabeel.steps.service.WalkTracker
import com.sabeel.steps.service.WalkTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * ViewModels are thin translators between the repositories' Flows and the screens. No step math,
 * aggregation or persistence logic is duplicated here: it all stays in data/ and domain/.
 */

data class AppUiState(
    val loading: Boolean = true,
    val onboardingDone: Boolean = false,
)

class AppViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<AppUiState> = container.settings.settingsFlow
        .map { AppUiState(loading = false, onboardingDone = it.onboardingDone) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { AppViewModel(app.container) }
        }
    }
}

data class HomeUiState(
    val settings: UserSettings = UserSettings(),
    val today: DailyStep? = null,
    val last7: List<DailyStep> = emptyList(),
    val streak: Int = 0,
    val serviceStatus: StepServiceStatus = StepServiceStatus.STOPPED,
    val sourceStatus: StepSourceStatus = StepSourceStatus(),
    val restDayToday: Boolean = false,
    /** The persisted counting intent (the switch the user set), not whether the service is alive. */
    val countingEnabled: Boolean = false,
    /** True when the user deliberately turned counting off; never presented as a failure. */
    val countingOffByUser: Boolean = false,
    /** True when counting is wanted but the service heartbeat is gone (Android/OEM stopped it). */
    val countingStoppedBySystem: Boolean = false,
    /** Wall-clock of the last service heartbeat; 0 when no heartbeat was ever recorded. */
    val lastHeartbeatAt: Long = 0L,
)

/** One poll of everything Home needs to describe the live counting state honestly. */
private data class HomeProbe(
    val serviceStatus: StepServiceStatus,
    val sourceStatus: StepSourceStatus,
    val countingEnabled: Boolean,
    val lastHeartbeatAt: Long,
)

class HomeViewModel(private val app: SabeelApp) : ViewModel() {
    private val container = app.container

    /** Settings plus whether today is an explicit rest day (streak paused, counting untouched). */
    private val preferencesFlow: Flow<Pair<UserSettings, Boolean>> = combine(
        container.settings.settingsFlow,
        container.settings.restDaysFlow,
    ) { settings, restDays -> settings to (DateKey.today() in restDays) }

    /**
     * Cheap local poll; no network, no wakeups beyond the screen being visible. The counting
     * intent, the service status and the heartbeat are read in the same tick so the ring, the pill
     * and the status card can never disagree about on/off/stopped.
     */
    private fun probeFlow(): Flow<HomeProbe> = flow {
        val counting = CountingStore.get(app)
        while (true) {
            val service = when {
                !Permissions.hasActivityRecognition(app) -> StepServiceStatus.NO_PERMISSION
                !StepSensors.hasAny(app) && !HealthConnect.isReady(app) -> StepServiceStatus.NO_SENSOR
                // A live in-process service instance outranks a stale heartbeat: after a device
                // sleep the prefs mark can look old even though counting never stopped.
                StepCounterService.runningInstance -> StepServiceStatus.COUNTING
                else -> StatusStore.read(app)
            }
            emit(
                HomeProbe(
                    serviceStatus = service,
                    sourceStatus = StatusStore.readSource(app),
                    countingEnabled = counting.current().enabled,
                    lastHeartbeatAt = StatusStore.readLastAliveAt(app),
                ),
            )
            delay(5_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<HomeUiState> = combine(
        preferencesFlow,
        container.steps.todayFlow(),
        container.steps.lastDaysFlow(7),
        container.steps.streakFlow(),
        probeFlow(),
    ) { preferences, today, last7, streak, probe ->
        val (settings, restDayToday) = preferences
        val window = StepMath.windowDates(DateKey.today(), 7)
        HomeUiState(
            settings = settings,
            today = today,
            last7 = StepMath.fillWindow(last7, window),
            streak = streak,
            serviceStatus = probe.serviceStatus,
            sourceStatus = probe.sourceStatus,
            restDayToday = restDayToday,
            countingEnabled = probe.countingEnabled,
            countingOffByUser = !probe.countingEnabled,
            countingStoppedBySystem = probe.countingEnabled &&
                probe.serviceStatus == StepServiceStatus.STOPPED_BY_SYSTEM,
            lastHeartbeatAt = probe.lastHeartbeatAt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * The one toggle behind the ring, the pill and the Settings mirror. The controller persists the
     * intent before it starts or stops anything and serialises double taps, so the UI stays dumb.
     */
    fun toggleCounting() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { container.countingController.toggle() }
        }
    }

    /** Called by the status cards. Union with the flag: the controller refuses without permission. */
    fun startCounting() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { container.countingController.setEnabled(true, CountingStore.REASON_USER) }
        }
    }

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { HomeViewModel(app) }
        }
    }
}

data class DiagnosticsUiState(
    val settings: UserSettings = UserSettings(),
    val today: DailyStep? = null,
    val serviceStatus: StepServiceStatus = StepServiceStatus.STOPPED,
    val sourceStatus: StepSourceStatus = StepSourceStatus(),
    /** Persisted counting intent (on/off), independent of the live service. */
    val countingEnabled: Boolean = false,
    /** Wall-clock of the last heartbeat; 0 when never recorded. */
    val lastHeartbeatAt: Long = 0L,
    /** Last failed persistence write, 0 when writes are healthy. */
    val storageFaultAt: Long = 0L,
    val notificationsGranted: Boolean = false,
    /** True when Android may restrict Sabeel in the background (battery optimisation applies). */
    val batteryOptimized: Boolean = false,
    /** Boot-time start attempts recorded by the boot receiver, as recorded (bounded by platform). */
    val bootAttempts: List<BootAttempt> = emptyList(),
)

/** One poll of the read-only evidence Diagnostics shows. */
private data class DiagnosticsProbe(
    val serviceStatus: StepServiceStatus,
    val sourceStatus: StepSourceStatus,
    val countingEnabled: Boolean,
    val lastHeartbeatAt: Long,
    val storageFaultAt: Long,
    val notificationsGranted: Boolean,
    val batteryOptimized: Boolean,
    val bootAttempts: List<BootAttempt>,
)

/**
 * Powers the "Why is my count low?" screen. It only reads local state (permission, sensor feature,
 * counting intent, service heartbeat, boot attempts, storage health, notification permission,
 * battery-optimisation state, today's row); it never writes anything by itself and never asks for a
 * new permission. The one action it can request is the explicit "Restart counting" button.
 */
class DiagnosticsViewModel(private val app: SabeelApp) : ViewModel() {
    private fun probeFlow(): Flow<DiagnosticsProbe> = flow {
        val counting = CountingStore.get(app)
        while (true) {
            val service = when {
                !Permissions.hasActivityRecognition(app) -> StepServiceStatus.NO_PERMISSION
                !StepSensors.hasAny(app) && !HealthConnect.isReady(app) -> StepServiceStatus.NO_SENSOR
                // A live in-process service instance outranks a stale heartbeat: after a device
                // sleep the prefs mark can look old even though counting never stopped.
                StepCounterService.runningInstance -> StepServiceStatus.COUNTING
                else -> StatusStore.read(app)
            }
            emit(
                DiagnosticsProbe(
                    serviceStatus = service,
                    sourceStatus = StatusStore.readSource(app),
                    countingEnabled = counting.current().enabled,
                    lastHeartbeatAt = StatusStore.readLastAliveAt(app),
                    storageFaultAt = StatusStore.readStorageFault(app) ?: 0L,
                    notificationsGranted = Permissions.hasNotifications(app),
                    batteryOptimized = isBatteryOptimized(),
                    bootAttempts = StatusStore.listBootAttempts(app),
                ),
            )
            delay(3_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<DiagnosticsUiState> = combine(
        app.container.settings.settingsFlow,
        app.container.steps.todayFlow(),
        probeFlow(),
    ) { settings, today, probe ->
        DiagnosticsUiState(
            settings = settings,
            today = today,
            serviceStatus = probe.serviceStatus,
            sourceStatus = probe.sourceStatus,
            countingEnabled = probe.countingEnabled,
            lastHeartbeatAt = probe.lastHeartbeatAt,
            storageFaultAt = probe.storageFaultAt,
            notificationsGranted = probe.notificationsGranted,
            batteryOptimized = probe.batteryOptimized,
            bootAttempts = probe.bootAttempts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiagnosticsUiState())

    /** User action: ask the controller to re-arm counting. Idempotent, safe without a service. */
    fun restartCounting() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { app.container.countingController.setEnabled(true, CountingStore.REASON_USER) }
        }
    }

    /** Battery optimisation is "on" for Sabeel when the app is NOT on the ignore list. */
    private fun isBatteryOptimized(): Boolean = runCatching {
        val power = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        power != null && !power.isIgnoringBatteryOptimizations(app.packageName)
    }.getOrDefault(false)

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { DiagnosticsViewModel(app) }
        }
    }
}

data class HistoryUiState(
    val range: Int = 7,
    val settings: UserSettings = UserSettings(),
    val days: List<DailyStep> = emptyList(),
    val walks: List<SavedWalkUi> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    private val range = MutableStateFlow(7)

    val state: StateFlow<HistoryUiState> = combine(
        range.flatMapLatest { container.steps.lastDaysFlow(it) },
        container.settings.settingsFlow,
        range,
        container.walks.walksFlow(),
    ) { days, settings, selected, walks ->
        HistoryUiState(
            range = selected,
            settings = settings,
            days = StepMath.fillWindow(days, StepMath.windowDates(DateKey.today(), selected)),
            walks = walks.map { it.toSavedWalkUi() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(days: Int) {
        range.value = days
    }

    /** True while a session is recording or being saved; exports are disabled meanwhile. */
    val sessionActive: StateFlow<Boolean> = WalkTracker.state
        .map { it.running || it.saving }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Writes every saved session as the CSV file the user picked; reports the session count. */
    fun exportAllSessionsCsv(context: Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { SessionExportIo.exportAllSessionsCsv(context, uri) }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    /**
     * Writes one saved session as a GPX file. A result of false means the session no longer exists
     * (nothing was written); an exception means the file could not be written.
     */
    fun exportSessionGpx(context: Context, uri: Uri, walkId: Long, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { SessionExportIo.exportSessionGpx(context, uri, walkId) }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { HistoryViewModel(app.container) }
        }
    }
}

data class InsightsUiState(
    val settings: UserSettings = UserSettings(),
    val days: List<DailyStep> = emptyList(),
    val streak: Int = 0,
    val averageSteps: Int = 0,
    val bestDay: DailyStep? = null,
    val goalHitRate: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val goalHitDays: Int = 0,
)

class InsightsViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<InsightsUiState> = combine(
        container.settings.settingsFlow,
        container.steps.lastDaysFlow(30),
        container.steps.streakFlow(),
        container.steps.allDaysFlow(),
    ) { settings, rows, streak, allRows ->
        val window = StepMath.windowDates(DateKey.today(), 30)
        InsightsUiState(
            settings = settings,
            days = StepMath.fillWindow(rows, window),
            streak = streak,
            averageSteps = StepMath.averageOverWindow(rows, window),
            bestDay = StepMath.bestDayOverWindow(rows, window),
            goalHitRate = StepMath.goalHitRateOverWindow(rows, window, settings.goalSteps),
            // Lifetime distance, deliberately different from the 30-day window above.
            totalDistanceMeters = StepMath.totalDistanceMeters(allRows),
            goalHitDays = StepMath.goalHitDaysOverWindow(rows, window, settings.goalSteps),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { InsightsViewModel(app.container) }
        }
    }
}

data class WalkUiState(
    val live: WalkTracker.Live = WalkTracker.Live(),
    val settings: UserSettings = UserSettings(),
    val pendingCheckpoint: Boolean = false,
    val startBlocked: Boolean = false,
)

/** Place radius range offered by the place dialog; the domain default is 100 m. */
internal const val PLACE_RADIUS_MIN_M = 25.0
internal const val PLACE_RADIUS_MAX_M = 1000.0

class WalkViewModel(private val app: SabeelApp) : ViewModel() {

    val live: StateFlow<WalkTracker.Live> = WalkTracker.state

    val settings: StateFlow<UserSettings> = app.container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    /** Saved setup templates; the most recently used one pre-fills the session setup screen. */
    val presets: StateFlow<List<SessionPreset>> = app.container.walks.presetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Locally saved places; a PLACE goal can only pick one of these (no search, no network). */
    val places: StateFlow<List<Place>> = app.container.walks.placesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The persisted counting intent, read synchronously for a correct first frame. */
    val countingEnabled: StateFlow<Boolean> = CountingStore.get(app).state
        .map { it.enabled }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CountingStore.get(app).current().enabled,
        )

    private val startBlocked = MutableStateFlow(false)

    /** The single state the Walk screen renders (kept in sync with the legacy flows above). */
    val state: StateFlow<WalkUiState> = combine(live, settings, startBlocked) { live, settings, blocked ->
        WalkUiState(
            live = live,
            settings = settings,
            pendingCheckpoint = WalkTracker.hasCheckpoint(app),
            startBlocked = blocked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkUiState())

    /**
     * Starts a fresh tracked session with [config] (activity, goal, GPS, places).
     *
     * @return false when the phone offers no foreground-service type the session may use; the setup
     * screen shows the blocked message instead of pretending a session started.
     */
    fun startSession(config: SessionConfig): Boolean {
        if (WalkTracker.state.value.running) return true
        WalkTracker.reset()
        val started = runCatching { WalkTrackingService.start(app, config) }.getOrDefault(false)
        startBlocked.value = started == false
        return started
    }

    /** Starts a fresh tracked walk with the legacy default setup. No-op if already tracking. */
    fun start() {
        if (WalkTracker.state.value.running) return
        WalkTracker.reset()
        val started = runCatching { WalkTrackingService.start(app) }.getOrDefault(true)
        startBlocked.value = started == false
    }

    /** Resumes the walk that was interrupted (checkpoint found on screen entry). */
    fun resumePending() {
        if (WalkTracker.state.value.running) return
        WalkTracker.reset()
        val started = runCatching { WalkTrackingService.start(app) }.getOrDefault(true)
        startBlocked.value = started == false
    }

    /** Deletes an interrupted session's checkpoint without saving it. */
    fun discardPending() {
        WalkTracker.deleteCheckpoint(app)
        WalkTracker.reset()
    }

    /** Pauses the running session: time, steps, distance and GPS freeze until resumed. */
    fun pauseSession() {
        runCatching { WalkTrackingService.pause(app) }
    }

    /** Resumes a paused session. Steps taken while paused are never counted (amendments A7/A8). */
    fun resumeSession() {
        runCatching { WalkTrackingService.resume(app) }
    }

    /** Asks the service to wrap up and persist the session. */
    fun stopSession() {
        runCatching { WalkTrackingService.stop(app) }
    }

    /** Ends the session without persisting it. */
    fun discardSession() {
        runCatching { WalkTrackingService.discard(app) }
    }

    /** Legacy save entry point kept for old walk call sites. */
    fun finish() = stopSession()

    /** Legacy discard entry point kept for old walk call sites. */
    fun discard() = discardSession()

    /** Clears the post-session summary card. */
    fun clearSummary() {
        WalkTracker.reset()
    }

    /**
     * Turns step counting on before a session starts: a session measures the global count and never
     * counts on its own (amendment A8). The controller persists the intent before starting anything.
     */
    fun turnOnCounting() {
        runCatching { app.container.countingController.setEnabled(true, CountingStore.REASON_USER) }
    }

    /** Saves [config] as a named preset ("Morning walk", "Friday run"); local only. */
    fun savePreset(name: String, config: SessionConfig) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                app.container.walks.upsertPreset(
                    SessionPreset(
                        name = trimmed,
                        activityType = config.activityType,
                        goalType = config.goalType,
                        goalValue = config.goalValue,
                        useGps = config.useGps,
                        startPlaceId = config.startPlaceId,
                        endPlaceId = config.endPlaceId,
                        lastUsedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    /** Deletes a saved preset; recorded sessions keep their own settings (presets are templates). */
    fun deletePreset(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { app.container.walks.deletePreset(id) }
        }
    }

    /**
     * Adds a locally defined place for PLACE goals. The dialog validates coordinates and radius; the
     * radius is clamped here as a second guard. Nothing is geocoded or uploaded.
     */
    fun savePlace(label: String, lat: Double, lon: Double, radiusM: Double) {
        val trimmed = label.trim()
        if (trimmed.isEmpty() || !lat.isFinite() || !lon.isFinite() || !radiusM.isFinite()) return
        val radius = radiusM.coerceIn(PLACE_RADIUS_MIN_M, PLACE_RADIUS_MAX_M)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                app.container.walks.upsertPlace(
                    Place(
                        label = trimmed,
                        lat = lat,
                        lon = lon,
                        radiusM = radius,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    /** Deletes a saved place; sessions that used it keep their recorded data. */
    fun deletePlace(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { app.container.walks.deletePlace(id) }
        }
    }

    /**
     * The OS' last known fix, only when a location permission is already granted; null otherwise.
     * Used solely to pre-fill the place dialog: nothing is requested and no fix leaves the phone.
     */
    suspend fun lastKnownLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!Permissions.hasLocation(app)) return@withContext null
        runCatching {
            val manager = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            listOf(
                LocationManager.PASSIVE_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
            ).firstNotNullOfOrNull { provider ->
                // Checked again directly at the call site: lint cannot follow the custom
                // Permissions.hasLocation() helper, and a stale grant can be revoked between
                // the early return and this call.
                val granted =
                    ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                if (!granted) return@firstNotNullOfOrNull null
                runCatching { manager?.getLastKnownLocation(provider) }.getOrNull()
            }?.let { fix -> fix.latitude to fix.longitude }
        }.getOrNull()
    }

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { WalkViewModel(app) }
        }
    }
}

private fun Walk.toSavedWalkUi() = SavedWalkUi(
    id = id,
    startedAtMillis = startedAt,
    durationSeconds = durationSec.toLong(),
    steps = steps,
    distanceMeters = distanceMeters,
    secondsPerKm = secondsPerKm,
    route = route,
    estimated = estimated,
    activityType = activityType,
    goalType = goalType,
    goalValue = goalValue,
    targetMet = targetMet,
)

class SettingsViewModel(private val app: SabeelApp) : ViewModel() {
    private val container = app.container

    val settings: StateFlow<UserSettings> = container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val restDays: StateFlow<Set<String>> = container.settings.restDaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Advice recomputed from the last four weeks of recorded days; null suggestion means "no change". */
    val goalAdvice: StateFlow<GoalAdvisor.Advice> = combine(
        container.settings.settingsFlow,
        container.steps.lastDaysFlow(GoalAdvisor.WINDOW_DAYS),
    ) { settings, days -> GoalAdvisor.advise(days, settings.goalSteps) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalAdvisor.Advice(recordedDays = 0, medianSteps = null, suggestion = null),
        )

    /** Mirror of the persisted counting intent for the Settings switch. */
    val countingEnabled: StateFlow<Boolean> = CountingStore.get(app).state
        .map { state -> state.enabled }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CountingStore.get(app).current().enabled,
        )

    private fun persist(transform: (UserSettings) -> UserSettings) {
        container.appScope.launch { container.settings.update(transform) }
    }

    private fun persistReminder(transform: (UserSettings) -> UserSettings) {
        container.appScope.launch {
            container.settings.update(transform)
            val updated = container.settings.current()
            if (updated.reminderEnabled) {
                Reminders.reschedule(app, updated)
            } else {
                Reminders.cancel(app)
            }
        }
    }

    /**
     * Persists the counting intent; the controller starts or stops the service to match. The switch
     * row passes the target value, so a double tap cannot invert twice by accident.
     */
    fun setCountingEnabled(enabled: Boolean) {
        container.appScope.launch {
            runCatching { container.countingController.setEnabled(enabled, CountingStore.REASON_USER) }
        }
    }

    /** Same intent flip as Home's ring/pill; kept for callers that do not know the target value. */
    fun toggleCounting() {
        container.appScope.launch {
            runCatching { container.countingController.toggle() }
        }
    }

    fun setGoal(goal: Int) = persist { it.copy(goalSteps = goal.coerceIn(1_000, 60_000)) }

    /** Applies a suggestion the user explicitly confirmed; never called automatically. */
    fun applySuggestedGoal(suggestion: GoalAdvisor.Suggestion) = setGoal(suggestion.proposedGoal)

    /** Marks or unmarks today as a rest day. Counting is not affected; only the streak pauses. */
    fun setRestDayToday(rest: Boolean) {
        container.appScope.launch {
            val today = DateKey.today()
            if (rest) container.settings.markRestDay(today) else container.settings.unmarkRestDay(today)
        }
    }

    fun setUnits(units: Units) = persist { it.copy(units = units) }

    fun setHeight(heightCm: Int) = persist { it.copy(heightCm = heightCm.coerceIn(100, 250)) }

    fun setWeight(weightKg: Int) = persist { it.copy(weightKg = weightKg.coerceIn(30, 250)) }

    fun setGender(gender: Gender) = persist { it.copy(gender = gender) }

    fun setReminderEnabled(enabled: Boolean) = persistReminder { it.copy(reminderEnabled = enabled) }

    fun setReminderMinutes(minutes: Int) =
        persistReminder { it.copy(reminderMinutes = minutes.coerceIn(0, 23 * 60 + 59)) }

    /**
     * Language is written twice on purpose: synchronously to SharedPreferences (read by
     * MainActivity.attachBaseContext before DataStore is available) and to DataStore so the domain
     * layer sees the same choice.
     */
    fun syncLanguage(language: AppLanguage) {
        container.appScope.launch { container.settings.update { it.copy(language = language) } }
    }

    fun exportCsv(context: Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        container.appScope.launch {
            val result = runCatching { CsvIo.export(context, uri) }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun importCsv(context: Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        container.appScope.launch {
            // The UI reports restored days; days already present with a larger total are kept and
            // not counted as imported.
            val result = runCatching { CsvIo.import(context, uri).imported }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun deleteAll(onDone: () -> Unit) {
        // Runs on the application scope, not viewModelScope: clearing settings flips the root
        // composition to onboarding, which destroys this ViewModel - a viewModelScope job would be
        // cancelled at the next suspension point and leave the erase half-done (locale, status and
        // the service's erase flag). NonCancellable keeps the whole erase atomic even if the
        // process is torn down immediately after.
        container.appScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                // Order matters: suppress service writes, kill the writers, erase every store, wait
                // one writer tick and erase again so a late flush cannot resurrect a day row, then
                // clear the platform-level prefs that are not part of DataStore and lift the
                // erase flag last so counting resumes cleanly.
                runCatching { StepCounterService.beginErase() }
                runCatching { WalkTracker.deleteCheckpoint(app) }
                runCatching { WalkTracker.resetSession(app) }
                runCatching { WalkTracker.reset() }
                runCatching { app.stopService(Intent(app, WalkTrackingService::class.java)) }
                runCatching { StepCounterService.stopForErase(app) }
                eraseAllData()
                delay(400)
                eraseAllData()
                runCatching { Notifications.resetPrefs(app) }
                runCatching { StatusStore.clear(app) }
                runCatching { AppLocale.clear(app) }
                runCatching { StepCounterService.endErase() }
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    private suspend fun eraseAllData() {
        runCatching { container.steps.clearAll() }
        runCatching { container.walks.clearAll() }
        runCatching { container.settings.clearAll() }
        runCatching { Reminders.cancel(app) }
    }

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { SettingsViewModel(app) }
        }
    }
}

data class OnboardingUiState(
    val settings: UserSettings = UserSettings(),
)

class OnboardingViewModel(private val app: SabeelApp) : ViewModel() {
    private val container = app.container

    val state: StateFlow<OnboardingUiState> = container.settings.settingsFlow
        .map { OnboardingUiState(settings = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun setGoal(goal: Int) {
        container.appScope.launch { container.settings.update { it.copy(goalSteps = goal.coerceIn(1_000, 60_000)) } }
    }

    fun setUnits(units: Units) {
        container.appScope.launch { container.settings.update { it.copy(units = units) } }
    }

    fun setBody(heightCm: Int, weightKg: Int, gender: Gender) {
        container.appScope.launch {
            container.settings.update {
                it.copy(
                    heightCm = heightCm.coerceIn(100, 250),
                    weightKg = weightKg.coerceIn(30, 250),
                    gender = gender,
                )
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        container.appScope.launch { container.settings.update { it.copy(language = language) } }
    }

    /** Marks onboarding complete and starts step counting, then hands control back to the caller. */
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                container.settings.update { it.copy(onboardingDone = true) }
            }
            // Starting the health foreground service without ACTIVITY_RECOGNITION would throw on
            // Android 14+; finish onboarding and let the Home status card ask for the permission.
            runCatching { StepCounterService.ensureStarted(app) }
            runCatching { Reminders.ensureReconcile(app) }
            onDone()
        }
    }

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { OnboardingViewModel(app) }
        }
    }
}