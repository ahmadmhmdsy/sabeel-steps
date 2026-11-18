package com.sabeel.steps.data.repo

import androidx.room.withTransaction
import com.sabeel.steps.data.db.AppDatabase
import com.sabeel.steps.data.db.DailyStepsDao
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.data.db.toEntity
import com.sabeel.steps.data.prefs.SettingsRepository
import com.sabeel.steps.domain.DailyStep
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.StepPipeline
import com.sabeel.steps.domain.StepState
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

/** Outcome of merging one imported day with the day already stored on the device. */
enum class MergeOutcome { INSERTED, UPDATED, KEPT }

/** Pure merge policy shared by the importer and its unit tests. */
object StepMerge {
    fun decide(existingSteps: Int?, importedSteps: Int): MergeOutcome = when {
        existingSteps == null -> MergeOutcome.INSERTED
        importedSteps > existingSteps -> MergeOutcome.UPDATED
        else -> MergeOutcome.KEPT
    }
}

/** Counts returned by a CSV restore so the UI can report what actually happened. */
data class RestoreSummary(
    val inserted: Int,
    val updated: Int,
    val kept: Int,
) {
    val imported: Int get() = inserted + updated
}

/**
 * Owns the step pipeline and the daily aggregates. The foreground service is the only writer of
 * raw sensor events; UI only observes flows.
 *
 * Invariants:
 *  - a day's stored total never decreases (the database is the durable record);
 *  - the pipeline state is persisted before the row, so a process death between the two writes is
 *    healed by max(state, row) on the next event;
 *  - the state machine is day-aware: moving to any different date drops the unattributable delta.
 */
class StepsRepository(
    private val database: AppDatabase,
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
            val from = DateKey.minusDays(today, (days - 1).coerceAtLeast(0))
            dao.observeSince(from).map { rows -> rows.map { it.toDomain() } }
        }

    /** Every stored day, for lifetime totals (not windowed aggregates). */
    fun allDaysFlow(): Flow<List<DailyStep>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun streakFlow(): Flow<Int> = combine(
        settings.settingsFlow,
        lastDaysFlow(365),
        settings.restDaysFlow,
    ) { s, days, restDays ->
        StepMath.streak(days, s.goalSteps, restDays = restDays)
    }

    /**
     * Called by StepCounterService for every sensor event.
     * @param activeMinutesToday current active-minute count maintained by the service, or -1 to keep.
     * @return the updated day row, or null when the first baseline sample produced nothing worth
     * persisting (a fresh install must not create a zero-step day row).
     */
    suspend fun recordRawStep(
        raw: Float,
        now: Long = System.currentTimeMillis(),
        activeMinutesToday: Int = -1,
    ): DailyStep? = mutex.withLock {
        val today = DateKey.today(now)
        val state = settings.readStepState(today)
        val update = StepPipeline.onSensor(state, raw, today)
        val existing = dao.today(today)?.toDomain()
        val steps = StepPipeline.resolveTodaySteps(update.state, existing?.steps)
        settings.writeStepState(update.state.copy(todaySteps = steps))
        if (!state.initialized && steps == 0 && existing == null) return@withLock null
        val user = settings.current()
        upsertDay(today, steps, user, now, activeMinutesToday, existing)
        dayRow(today, steps, user, now, activeMinutesToday, existing)
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
            state = StepPipeline.onSensor(state, raw, today).state
        } else if (state.baselineDate != today) {
            // No sensor reading yet on the new day: forget the old baseline and re-seed on the next
            // event, but keep the existing day row (max rule below).
            state = StepState(baselineRaw = 0f, todaySteps = 0, baselineDate = today, initialized = false)
        }
        val existing = dao.today(today)?.toDomain()
        val steps = StepPipeline.resolveTodaySteps(state, existing?.steps)
        settings.writeStepState(state.copy(todaySteps = steps))
        if (steps == 0 && existing == null) return@withLock null
        val user = settings.current()
        upsertDay(today, steps, user, now, activeMinutesToday, existing)
        dayRow(today, steps, user, now, activeMinutesToday, existing)
    }

    /**
     * Restores daily aggregates from a CSV backup in one transaction. Existing days are never
     * lowered: the larger total wins. Today's pipeline state is re-seeded from the durable row so
     * the first sensor event after the import continues from the restored total instead of
     * overwriting it.
     */
    suspend fun restore(rows: List<DailyStep>): RestoreSummary = mutex.withLock {
        var inserted = 0
        var updated = 0
        var kept = 0
        database.withTransaction {
            for (imported in rows.sortedBy { it.date }) {
                val existing = dao.today(imported.date)?.toDomain()
                when (StepMerge.decide(existing?.steps, imported.steps)) {
                    MergeOutcome.INSERTED -> {
                        dao.upsert(imported.toEntity())
                        inserted++
                    }
                    MergeOutcome.UPDATED -> {
                        dao.upsert(imported.toEntity())
                        updated++
                    }
                    MergeOutcome.KEPT -> kept++
                }
            }
        }
        val today = DateKey.today()
        val todayRow = dao.today(today)?.toDomain()
        if (todayRow != null) {
            val state = settings.readStepState(today)
            if (todayRow.steps >= state.todaySteps) {
                settings.writeStepState(
                    state.copy(todaySteps = todayRow.steps, baselineDate = today, initialized = false),
                )
            }
        }
        RestoreSummary(inserted = inserted, updated = updated, kept = kept)
    }

    private suspend fun upsertDay(
        date: String,
        steps: Int,
        user: UserSettings,
        now: Long,
        activeMinutes: Int,
        existing: DailyStep?,
    ) {
        dao.upsert(dayRow(date, steps, user, now, activeMinutes, existing).toEntity())
    }

    private fun dayRow(
        date: String,
        steps: Int,
        user: UserSettings,
        now: Long,
        activeMinutes: Int,
        existing: DailyStep?,
    ): DailyStep = DailyStep(
        date = date,
        steps = steps,
        distanceMeters = StepMath.distanceMeters(steps, StepMath.strideCm(user)),
        calories = StepMath.calories(steps, user.weightKg),
        activeMinutes = resolveActiveMinutes(activeMinutes, existing),
        updatedAt = now,
    )

    /**
     * Active minutes are measured by the service's cadence. There is deliberately no synthetic
     * fallback: a missing value means "not measured", not "invent a number".
     */
    private fun resolveActiveMinutes(activeMinutes: Int, existing: DailyStep?): Int =
        if (activeMinutes >= 0) activeMinutes.coerceAtLeast(0) else existing?.activeMinutes?.coerceAtLeast(0) ?: 0

    suspend fun clearAll() = dao.clear()
}
