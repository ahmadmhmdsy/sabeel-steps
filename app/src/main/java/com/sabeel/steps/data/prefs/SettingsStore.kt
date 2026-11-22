package com.sabeel.steps.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sabeel.steps.domain.AppLanguage
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.StepSource
import com.sabeel.steps.domain.StepPipeline
import com.sabeel.steps.domain.StepState
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.SabeelDataStore: DataStore<Preferences> by preferencesDataStore(name = "Sabeel_settings")

/** Rest days older than this can no longer affect a streak, so they are pruned on write. */
private const val REST_DAY_HISTORY_DAYS = 365

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

    // explicit rest days, stored as ISO dates; a rest day pauses the streak instead of breaking it
    val restDays = stringSetPreferencesKey("rest_days")

    // legacy single-baseline pipeline keys (v1.1); read once for migration, never written again
    val legacyBaselineRaw = floatPreferencesKey("step_baseline_raw")
    val legacyTodaySteps = intPreferencesKey("step_today_steps")
    val legacyBaselineDate = stringPreferencesKey("step_baseline_date")
    val legacyInitialized = booleanPreferencesKey("step_initialized")
    val legacySource = stringPreferencesKey("step_source")
    val legacyLastRawAt = longPreferencesKey("step_last_raw_at")

    // manifold state (v1.2): one fused day backed by four independent ledgers
    val schema = intPreferencesKey("step_state_schema")
    val day = stringPreferencesKey("step_state_day")
    val total = intPreferencesKey("step_state_total")
    val confirmed = intPreferencesKey("step_state_confirmed")
    val counterToday = intPreferencesKey("step_state_counter_today")
    val counterLastRaw = floatPreferencesKey("step_state_counter_last_raw")
    val counterLastEventNanos = longPreferencesKey("step_state_counter_last_event_ns")
    val detectorToday = intPreferencesKey("step_state_detector_today")
    val detectorLastEventNanos = longPreferencesKey("step_state_detector_last_event_ns")
    val hcToday = intPreferencesKey("step_state_hc_today")
    val hcOffsetToday = intPreferencesKey("step_state_hc_offset_today")
    val hcSuppressedForDay = booleanPreferencesKey("step_state_hc_suppressed_for_day")
    val estimateToday = intPreferencesKey("step_state_estimate_today")
    val initialized = booleanPreferencesKey("step_state_initialized")
    val updatedAt = longPreferencesKey("step_state_updated_at")
}

/** Bump when the persisted manifold shape changes; readers migrate older shapes below. */
private const val STEP_STATE_SCHEMA = 3

/**
 * First schema that stored the manifold shape (v1.2). A row at or above this schema is read as a
 * manifold state; schema 2 predates the pause/resume Health Connect keys, which default to
 * "no offset, not suppressed" when absent, so reading it is the same code path as schema 3.
 */
private const val MANIFOLD_SCHEMA = 2

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /** Cached answer to "is this install already on the manifold schema?" (legacy upgrade path). */
    @Volatile private var manifoldSchemaKnown: Boolean? = null

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { it.toUserSettings() }

    /** ISO dates the user marked as rest days. Kept out of [UserSettings] because it is history. */
    val restDaysFlow: Flow<Set<String>> = dataStore.data
        .map { it[Keys.restDays] ?: emptySet() }
        .distinctUntilChanged()

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

    /**
     * True once this install has written the v1.2 manifold shape. False means the next read is a
     * legacy migration whose estimate marker lives only in the durable daily row.
     */
    suspend fun isManifoldSchema(): Boolean {
        manifoldSchemaKnown?.let { return it }
        val known = runCatching { (dataStore.data.first()[Keys.schema] ?: 0) >= MANIFOLD_SCHEMA }
            .getOrDefault(true)
        manifoldSchemaKnown = known
        return known
    }

    /**
     * Reads the manifold state, migrating the v1.1 single-baseline shape on first read. The
     * migration treats the old day total as confirmed (the old estimate marker was never persisted —
     * that bug is part of why this schema exists) and keeps the old counter baseline so the next
     * counter sample adds only its real delta. Schema >= [MANIFOLD_SCHEMA] rows read the pause/resume
     * keys with "no offset, not suppressed" defaults, so a v1.2 row upgrades in place.
     */
    suspend fun readStepState(today: String): StepState = runCatching {
        val prefs = dataStore.data.first()
        val schema = prefs[Keys.schema] ?: 0
        if (schema >= MANIFOLD_SCHEMA) readManifoldState(prefs, today) else migrateLegacyState(prefs, today)
    }.getOrElse { StepState.initial(today) }

    suspend fun writeStepState(state: StepState) {
        manifoldSchemaKnown = true
        dataStore.edit { prefs ->
            prefs[Keys.schema] = STEP_STATE_SCHEMA
            prefs[Keys.day] = state.day
            prefs[Keys.total] = state.total
            prefs[Keys.confirmed] = state.confirmed
            prefs[Keys.counterToday] = state.counterToday
            prefs[Keys.counterLastRaw] = state.counterLastRaw
            prefs[Keys.counterLastEventNanos] = state.counterLastEventNanos
            prefs[Keys.detectorToday] = state.detectorToday
            prefs[Keys.detectorLastEventNanos] = state.detectorLastEventNanos
            prefs[Keys.hcToday] = state.hcToday
            prefs[Keys.hcOffsetToday] = state.hcOffsetToday
            prefs[Keys.hcSuppressedForDay] = state.hcSuppressedForDay
            prefs[Keys.estimateToday] = state.estimateToday
            prefs[Keys.initialized] = state.initialized
            prefs[Keys.updatedAt] = state.updatedAt
            // The legacy keys are dead after the first manifold write.
            prefs.remove(Keys.legacyBaselineRaw)
            prefs.remove(Keys.legacyTodaySteps)
            prefs.remove(Keys.legacyBaselineDate)
            prefs.remove(Keys.legacyInitialized)
            prefs.remove(Keys.legacySource)
            prefs.remove(Keys.legacyLastRawAt)
        }
    }

    private fun readManifoldState(prefs: Preferences, today: String): StepState {
        val day = prefs[Keys.day] ?: today
        return StepState(
            day = if (day.isEmpty()) today else day,
            total = prefs[Keys.total] ?: 0,
            confirmed = prefs[Keys.confirmed] ?: 0,
            counterToday = prefs[Keys.counterToday] ?: 0,
            counterLastRaw = prefs[Keys.counterLastRaw] ?: Float.NaN,
            counterLastEventNanos = prefs[Keys.counterLastEventNanos] ?: 0L,
            detectorToday = prefs[Keys.detectorToday] ?: 0,
            detectorLastEventNanos = prefs[Keys.detectorLastEventNanos] ?: 0L,
            hcToday = prefs[Keys.hcToday] ?: 0,
            hcOffsetToday = prefs[Keys.hcOffsetToday] ?: 0,
            hcSuppressedForDay = prefs[Keys.hcSuppressedForDay] ?: false,
            estimateToday = prefs[Keys.estimateToday] ?: 0,
            initialized = prefs[Keys.initialized] ?: false,
            updatedAt = prefs[Keys.updatedAt] ?: 0L,
        )
    }

    /** v1.1 shape: baselineRaw/todaySteps/baselineDate/initialized/source. */
    private fun migrateLegacyState(prefs: Preferences, today: String): StepState =
        StepPipeline.migrateLegacyState(
            today = today,
            legacyTotal = prefs[Keys.legacyTodaySteps] ?: 0,
            legacyBaselineRaw = prefs[Keys.legacyBaselineRaw] ?: Float.NaN,
            legacyBaselineDate = prefs[Keys.legacyBaselineDate] ?: today,
            legacyInitialized = prefs[Keys.legacyInitialized] ?: false,
        )

    /** Marks [date] (ISO) as a rest day. The set is bounded so it cannot grow forever. */
    suspend fun markRestDay(date: String) {
        if (runCatching { java.time.LocalDate.parse(date) }.isFailure) return
        dataStore.edit { prefs ->
            prefs[Keys.restDays] = pruneRestDays((prefs[Keys.restDays] ?: emptySet()) + date)
        }
    }

    suspend fun unmarkRestDay(date: String) {
        dataStore.edit { prefs ->
            prefs[Keys.restDays] = pruneRestDays((prefs[Keys.restDays] ?: emptySet()) - date)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

/** Drops rest days that are too old to influence a streak, keeping the stored set small. */
private fun pruneRestDays(days: Set<String>): Set<String> {
    val cutoff = DateKey.minusDays(DateKey.today(), REST_DAY_HISTORY_DAYS)
    return days.filter { it >= cutoff }.toSet()
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