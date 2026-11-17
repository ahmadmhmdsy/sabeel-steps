package com.sabeel.steps.ui.viewmodel

import android.content.Context
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
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.UserSettings
import com.sabeel.steps.platform.Reminders
import com.sabeel.steps.service.StepCounterService
import com.sabeel.steps.service.WalkTracker
import com.sabeel.steps.service.WalkTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        container.settings.settingsFlow,
        container.steps.todayFlow(),
        container.steps.lastDaysFlow(7),
        container.steps.streakFlow(),
    ) { settings, today, last7, streak ->
        HomeUiState(settings = settings, today = today, last7 = last7, streak = streak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { HomeViewModel(app.container) }
        }
    }
}

data class HistoryUiState(
    val range: Int = 7,
    val settings: UserSettings = UserSettings(),
    val days: List<DailyStep> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(container: AppContainer) : ViewModel() {
    private val range = MutableStateFlow(7)

    val state: StateFlow<HistoryUiState> = combine(
        range.flatMapLatest { container.steps.lastDaysFlow(it) },
        container.settings.settingsFlow,
        range,
    ) { days, settings, selected ->
        HistoryUiState(range = selected, settings = settings, days = days)
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
        container.steps.lastDaysFlow(7),
        container.steps.streakFlow(),
    ) { settings, days, streak ->
        InsightsUiState(
            settings = settings,
            days = days,
            streak = streak,
            averageSteps = StepMath.averageSteps(days),
            bestDay = days.maxByOrNull { it.steps },
            goalHitRate = StepMath.goalHitRate(days, settings.goalSteps),
            totalDistanceMeters = StepMath.totalDistanceMeters(days),
            goalHitDays = days.count { it.steps >= settings.goalSteps },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    companion object {
        fun factory(app: SabeelApp) = viewModelFactory {
            initializer { InsightsViewModel(app.container) }
        }
    }
}

class WalkViewModel(private val app: SabeelApp) : ViewModel() {

    val live: StateFlow<WalkTracker.Live> = WalkTracker.state

    val settings: StateFlow<UserSettings> = app.container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    /** Starts a fresh tracked walk. No-op if the service is already tracking. */
    fun start() {
        if (WalkTracker.state.value.running) return
        WalkTracker.reset()
        runCatching { WalkTrackingService.start(app) }
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

class SettingsViewModel(private val app: SabeelApp) : ViewModel() {
    private val container = app.container

    val settings: StateFlow<UserSettings> = container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

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
            val result = runCatching { CsvIo.import(context, uri) }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun deleteAll(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { container.steps.clearAll() }
                runCatching { container.walks.clearAll() }
                runCatching { container.settings.clearAll() }
                runCatching { Reminders.cancel(app) }
            }
            onDone()
        }
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
            runCatching { StepCounterService.start(app) }
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
