package com.sabeel.steps.data.repo

import com.sabeel.steps.data.db.DailyStepsDao
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.data.db.toEntity
import com.sabeel.steps.data.prefs.SettingsRepository
import com.sabeel.steps.domain.DailyStep
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.StepPipeline
import com.sabeel.steps.domain.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the step pipeline and the daily aggregates. The foreground service is the only writer of
 * raw sensor events; UI only observes flows.
 */
class StepsRepository(
    private val dao: DailyStepsDao,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()

    private fun todayTicker(): Flow<String> = flow {
        while (true) {
            emit(DateKey.today())
            delay(60_000)
        }
    }.distinctUntilChanged()

    fun todayFlow(): Flow<DailyStep?> = todayTicker()
        .flatMapLatest { date -> dao.observe(date).map { it?.toDomain() } }

    fun lastDaysFlow(days: Int): Flow<List<DailyStep>> = todayTicker()
        .flatMapLatest { today ->
            dao.observeSince(DateKey.minusDays(today, days - 1)).map { rows -> rows.map { it.toDomain() } }
        }

    fun streakFlow(): Flow<Int> = combine(settings.settingsFlow, lastDaysFlow(60)) { s, days ->
        StepMath.streak(days, s.goalSteps)
    }

    /**
     * Called by StepCounterService for every sensor event.
     * @param activeMinutesToday current active-minute count maintained by the service, or -1 to keep.
     * @return the updated day row, or null when nothing changed.
     */
    suspend fun recordRawStep(
        raw: Float,
        now: Long = System.currentTimeMillis(),
        activeMinutesToday: Int = -1,
    ): DailyStep? = mutex.withLock {
        val today = DateKey.today(now)
        val state = settings.readStepState(today)
        val update = StepPipeline.onSensor(state, raw, today)
        settings.writeStepState(update.state)
        val user = settings.current()
        upsertDay(today, update.todaySteps, user, now, activeMinutesToday)
        stepsOf(today, update.todaySteps, user, now, activeMinutesToday)
    }

    /** Writes the current step total (used on service start / reconcile after boot). */
    suspend fun reconcile(
        now: Long = System.currentTimeMillis(),
        raw: Float? = null,
        activeMinutesToday: Int = -1,
    ): DailyStep? = mutex.withLock {
        val today = DateKey.today(now)
        var state = settings.readStepState(today)
        if (raw != null) {
            val update = StepPipeline.onSensor(state, raw, today)
            state = update.state
            settings.writeStepState(state)
        } else if (state.baselineDate != today) {
            state = com.sabeel.steps.domain.StepState(baselineRaw = state.baselineRaw, todaySteps = 0, baselineDate = today)
            settings.writeStepState(state)
        }
        val user = settings.current()
        upsertDay(today, state.todaySteps, user, now, activeMinutesToday)
        stepsOf(today, state.todaySteps, user, now, activeMinutesToday)
    }

    private suspend fun upsertDay(
        date: String,
        steps: Int,
        user: UserSettings,
        now: Long,
        activeMinutes: Int,
    ) {
        val row = DailyStep(
            date = date,
            steps = steps,
            distanceMeters = StepMath.distanceMeters(steps, StepMath.strideCm(user)),
            calories = StepMath.calories(steps, user.weightKg),
            activeMinutes = if (activeMinutes >= 0) activeMinutes else readActiveMinutes(date, steps),
            updatedAt = now,
        )
        dao.upsert(row.toEntity())
    }

    private suspend fun readActiveMinutes(date: String, fallbackSteps: Int): Int {
        val rows = dao.getSince(date)
        return rows.firstOrNull { it.date == date }?.activeMinutes ?: (fallbackSteps / 120)
    }

    private fun stepsOf(
        date: String,
        steps: Int,
        user: UserSettings,
        now: Long,
        activeMinutes: Int,
    ): DailyStep = DailyStep(
        date = date,
        steps = steps,
        distanceMeters = StepMath.distanceMeters(steps, StepMath.strideCm(user)),
        calories = StepMath.calories(steps, user.weightKg),
        activeMinutes = activeMinutes.coerceAtLeast(0),
        updatedAt = now,
    )

    suspend fun clearAll() = dao.clear()
}