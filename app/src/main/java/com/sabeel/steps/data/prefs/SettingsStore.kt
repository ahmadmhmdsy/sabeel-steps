package com.sabeel.steps.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sabeel.steps.domain.AppLanguage
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.StepState
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.SabeelDataStore: DataStore<Preferences> by preferencesDataStore(name = "Sabeel_settings")

private object Keys {
    val goalSteps = intPreferencesKey("goal_steps")
    val units = stringPreferencesKey("units")
    val heightCm = intPreferencesKey("height_cm")
    val weightKg = intPreferencesKey("weight_kg")
    val gender = stringPreferencesKey("gender")
    val strideCm = intPreferencesKey("stride_cm")
    val language = stringPreferencesKey("language")
    val reminderEnabled = booleanPreferencesKey("reminder_enabled")
    val reminderMinutes = intPreferencesKey("reminder_minutes")
    val onboardingDone = booleanPreferencesKey("onboarding_done")

    // sensor pipeline state
    val baselineRaw = floatPreferencesKey("step_baseline_raw")
    val todaySteps = intPreferencesKey("step_today_steps")
    val baselineDate = stringPreferencesKey("step_baseline_date")
    val initialized = booleanPreferencesKey("step_initialized")
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { it.toUserSettings() }

    suspend fun current(): UserSettings = settingsFlow.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toUserSettings())
            prefs[Keys.goalSteps] = next.goalSteps
            prefs[Keys.units] = next.units.name
            prefs[Keys.heightCm] = next.heightCm
            prefs[Keys.weightKg] = next.weightKg
            prefs[Keys.gender] = next.gender.name
            prefs[Keys.strideCm] = next.strideCm
            prefs[Keys.language] = next.language.code
            prefs[Keys.reminderEnabled] = next.reminderEnabled
            prefs[Keys.reminderMinutes] = next.reminderMinutes
            prefs[Keys.onboardingDone] = next.onboardingDone
        }
    }

    suspend fun readStepState(today: String): StepState = runCatching {
        val prefs = dataStore.data.first()
        StepState(
            baselineRaw = prefs[Keys.baselineRaw] ?: 0f,
            todaySteps = prefs[Keys.todaySteps] ?: 0,
            baselineDate = prefs[Keys.baselineDate] ?: today,
            initialized = prefs[Keys.initialized] ?: false,
        )
    }.getOrElse { StepState.initial(today) }

    suspend fun writeStepState(state: StepState) {
        dataStore.edit { prefs ->
            prefs[Keys.baselineRaw] = state.baselineRaw
            prefs[Keys.todaySteps] = state.todaySteps
            prefs[Keys.baselineDate] = state.baselineDate
            prefs[Keys.initialized] = state.initialized
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

private fun Preferences.toUserSettings(): UserSettings = UserSettings(
    goalSteps = this[Keys.goalSteps] ?: 8000,
    units = runCatching { Units.valueOf(this[Keys.units] ?: Units.METRIC.name) }.getOrDefault(Units.METRIC),
    heightCm = this[Keys.heightCm] ?: 170,
    weightKg = this[Keys.weightKg] ?: 70,
    gender = runCatching { Gender.valueOf(this[Keys.gender] ?: Gender.UNSPECIFIED.name) }
        .getOrDefault(Gender.UNSPECIFIED),
    strideCm = this[Keys.strideCm] ?: 0,
    language = AppLanguage.fromCode(this[Keys.language]),
    reminderEnabled = this[Keys.reminderEnabled] ?: false,
    reminderMinutes = this[Keys.reminderMinutes] ?: (19 * 60),
    onboardingDone = this[Keys.onboardingDone] ?: false,
)