package com.sabeel.steps.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sabeel.steps.AppContainer
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.export.CsvIo
import com.sabeel.steps.domain.AppLanguage
import com.sabeel.steps.domain.DailyStep
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.GoalAdvisor
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.UserSettings
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.platform.HealthConnect
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.Reminders
import com.sabeel.steps.platform.StatusStore
import com.sabeel.steps.platform.StepSensors
import com.sabeel.steps.platform.StepServiceStatus
import com.sabeel.steps.platform.StepSourceStatus
import com.sabeel.steps.ui.i18n.AppLocale
import com.sabeel.steps.ui.screens.SavedWalkUi
import com.sabeel.steps.service.StepCounterService
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
)

class HomeViewModel(private val app: SabeelApp) : ViewModel() {
    private val container = app.container

    /** Cheap local poll; no network, no wakeups beyond the screen being visible. */
    private fun serviceStatusFlow(): Flow<StepServiceStatus> = flow {
        while (true) {
            emit(
                when {
                    !Permissions.hasActivityRecognition(app) -> StepServiceStatus.NO_PERMISSION
                    !StepSensors.hasAny(app) && !HealthConnect.isReady(app) -> StepServiceStatus.NO_SENSOR
                    // A live in-process service instance outranks a stale heartbeat: after a device
                    // sleep the prefs mark can look old even though counting never stopped.
                    StepCounterService.runningInstance -> StepServiceStatus.COUNTING
                    else -> StatusStore.read(app)
                },
            )
            delay(5_000)
        }
    }.distinctUntilChanged()

    /** Settings plus whether today is an explicit rest day (streak paused, counting untouched). */
    private val preferencesFlow: Flow<Pair<UserSettings, Boolean>> = combine(
        container.settings.settingsFlow,
        container.settings.restDaysFlow,
    ) { settings, restDays -> settings to (DateKey.today() in restDays) }

    /** Service status plus the live source, polled together so Home never shows mismatched state. */
    private fun statusFlow(): Flow<Pair<StepServiceStatus, StepSourceStatus>> = combine(
        serviceStatusFlow(),
        sourceStatusFlow(),
    ) { status, source -> status to source }

    private fun sourceStatusFlow(): Flow<StepSourceStatus> = flow {
        while (true) {
            emit(StatusStore.readSource(app))
            delay(5_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<HomeUiState> = combine(
        preferencesFlow,
        container.steps.todayFlow(),
        container.steps.lastDaysFlow(7),
        container.steps.streakFlow(),
        statusFlow(),
    ) { preferences, today, last7, streak, status ->
        val (settings, restDayToday) = preferences
        val (serviceStatus, sourceStatus) = status
        val window = StepMath.windowDates(DateKey.today(), 7)
        HomeUiState(
            settings = settings,
            today = today,
            last7 = StepMath.fillWindow(last7, window),
            streak = streak,
            serviceStatus = serviceStatus,
            sourceStatus = sourceStatus,
            restDayToday = restDayToday,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Called by the status card. Safe when the permission is missing (returns false). */
    fun startCounting() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { StepCounterService.ensureStarted(app) }
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
)

/**
 * Powers the "Why is my count low?" screen. It only reads local state (permission, sensor feature,
 * service heartbeat, today's row); it never writes anything and never asks for a new permission.
 */
class DiagnosticsViewModel(private val app: SabeelApp) : ViewModel() {
    private fun serviceStatusFlow(): Flow<StepServiceStatus> = flow {
        while (true) {
            emit(
                when {
                    !Permissions.hasActivityRecognition(app) -> StepServiceStatus.NO_PERMISSION
                    !StepSensors.hasAny(app) && !HealthConnect.isReady(app) -> StepServiceStatus.NO_SENSOR
                    // A live in-process service instance outranks a stale heartbeat: after a device
                    // sleep the prefs mark can look old even though counting never stopped.
                    StepCounterService.runningInstance -> StepServiceStatus.COUNTING
                    else -> StatusStore.read(app)
                },
            )
            delay(3_000)
        }
    }.distinctUntilChanged()

    private fun sourceStatusFlow(): Flow<StepSourceStatus> = flow {
        while (true) {
            emit(StatusStore.readSource(app))
            delay(3_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<DiagnosticsUiState> = combine(
        app.container.settings.settingsFlow,
        app.container.steps.todayFlow(),
        serviceStatusFlow(),
        sourceStatusFlow(),
    ) { settings, today, serviceStatus, sourceStatus ->
        DiagnosticsUiState(
            settings = settings,
            today = today,
            serviceStatus = serviceStatus,
            sourceStatus = sourceStatus,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiagnosticsUiState())

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
class HistoryViewModel(container: AppContainer) : ViewModel() {
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

class WalkViewModel(private val app: SabeelApp) : ViewModel() {

    val live: StateFlow<WalkTracker.Live> = WalkTracker.state

    val settings: StateFlow<UserSettings> = app.container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

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

    /** Starts a fresh tracked walk. No-op if the service is already tracking. */
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

    /** Deletes an interrupted walk's checkpoint without saving it. */
    fun discardPending() {
        WalkTracker.deleteCheckpoint(app)
        WalkTracker.reset()
    }

    /** Asks the service to wrap up and persist the walk. */
    fun finish() {
        runCatching { WalkTrackingService.stop(app) }
    }

    /** Ends the walk without persisting it; the service clears its checkpoint and live state. */
    fun discard() {
        runCatching { WalkTrackingService.discard(app) }
    }

    /** Clears the post-walk summary card. */
    fun clearSummary() {
        WalkTracker.reset()
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