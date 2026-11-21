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
 * One coalesced observation from the counting layer: the newest cumulative counter value, the
 * detector steps seen since the last write, the estimated steps seen since the last write and/or
 * the absolute Health Connect floor. All fields optional; the service sends one snapshot per second.
 */
data class StepSnapshot(
    val counterRaw: Float? = null,
    val counterEventNanos: Long = 0L,
    val detectorSteps: Int = 0,
    val detectorEventNanos: Long = 0L,
    val estimateSteps: Int = 0,
    val hcTotal: Int? = null,
) {
    val isEmpty: Boolean
        get() = counterRaw == null && detectorSteps <= 0 && estimateSteps <= 0 && hcTotal == null
}

/**
 * Owns the step manifold and the daily aggregates. The foreground service is the only writer; UI
 * only observes flows.
 *
 * Invariants:
 *  - the state is persisted before the row, so a process death between the two writes is healed by
 *    max(state, row) on the next observation;
 *  - a day's stored total never decreases (the database is the durable record);
 *  - the estimated marker is derived from the fused state, never accumulated as a second count;
 *  - every source is applied under one mutex, so the writer and the Health Connect poll cannot race.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
     * Applies one coalesced observation. Returns the updated day row, or null when there is nothing
     * worth writing (a zero-step day never creates a row).
     */
    suspend fun record(
        snapshot: StepSnapshot,
        now: Long = System.currentTimeMillis(),
        activeMinutesToday: Int = -1,
    ): DailyStep? = mutex.withLock {
        if (snapshot.isEmpty) return@withLock null
        val today = DateKey.today(now)
        val legacyUpgrade = !settings.isManifoldSchema()
        var state = settings.readStepState(today)
        var rolled = false

        snapshot.counterRaw?.let { raw ->
            val update = StepPipeline.onCounter(
                state,
                raw = raw,
                today = today,
                eventNanos = snapshot.counterEventNanos,
                nowMillis = now,
            )
            state = update.state
            rolled = rolled || update.rolledOver
        }
        if (snapshot.detectorSteps > 0) {
            val update = StepPipeline.onDetector(
                state,
                steps = snapshot.detectorSteps,
                today = today,
                eventNanos = snapshot.detectorEventNanos,
                nowMillis = now,
            )
            state = update.state
            rolled = rolled || update.rolledOver
        }
        if (snapshot.estimateSteps > 0) {
            val update = StepPipeline.onEstimate(state, steps = snapshot.estimateSteps, today = today, nowMillis = now)
            state = update.state
            rolled = rolled || update.rolledOver
        }
        snapshot.hcTotal?.let { total ->
            val update = StepPipeline.onExternalTotal(state, totalSteps = total, today = today, nowMillis = now)
            state = update.state
            rolled = rolled || update.rolledOver
        }
        if (rolled) {
            // The day changed mid-snapshot: re-read the new day's row so both ledgers agree.
            val existingRolled = dao.today(today)?.toDomain()
            val resolved = StepPipeline.resolveTodaySteps(state, existingRolled?.steps)
            state = state.copy(total = resolved)
        }

        val existing = dao.today(today)?.toDomain()
        val steps = StepPipeline.resolveTodaySteps(state, existing?.steps)
        val rowIsAhead = existing != null && existing.steps > state.total
        var estimated = StepPipeline.resolveEstimatedSteps(state.estimated, existing?.estimatedSteps, rowIsAhead)
        if (legacyUpgrade) {
            estimated = StepPipeline.preserveLegacyMarker(
                estimated,
                steps,
                existing?.steps,
                existing?.estimatedSteps,
            )
        }
        val adjusted = state.copy(
            total = steps,
            confirmed = maxOf(state.confirmed, steps - estimated),
            updatedAt = now,
        )
        settings.writeStepState(adjusted)
        if (steps <= 0 && existing == null) return@withLock null
        val user = settings.current()
        upsertDay(today, steps, estimated, user, now, activeMinutesToday, existing)
        dayRow(today, steps, estimated, user, now, activeMinutesToday, existing)
    }

    /**
     * Writes the current stepped day without new sensor evidence (service start, boot, midnight and
     * the 15-minute reconcile). The row and the in-memory state are reconciled by max, so this never
     * lowers a day.
     */
    suspend fun reconcile(
        now: Long = System.currentTimeMillis(),
        activeMinutesToday: Int = -1,
    ): DailyStep? = mutex.withLock {
        val today = DateKey.today(now)
        val legacyUpgrade = !settings.isManifoldSchema()
        val prior = settings.readStepState(today)
        val update = StepPipeline.reconcile(prior, today, now)
        var state = update.state
        val existing = dao.today(today)?.toDomain()
        val steps = StepPipeline.resolveTodaySteps(state, existing?.steps)
        val rowIsAhead = existing != null && existing.steps > state.total
        var estimated = StepPipeline.resolveEstimatedSteps(state.estimated, existing?.estimatedSteps, rowIsAhead)
        if (legacyUpgrade) {
            estimated = StepPipeline.preserveLegacyMarker(
                estimated,
                steps,
                existing?.steps,
                existing?.estimatedSteps,
            )
        }
        state = state.copy(
            total = steps,
            confirmed = maxOf(state.confirmed, steps - estimated),
            updatedAt = now,
        )
        settings.writeStepState(state)
        if (steps <= 0 && existing == null) return@withLock null
        val user = settings.current()
        upsertDay(today, steps, estimated, user, now, activeMinutesToday, existing)
        dayRow(today, steps, estimated, user, now, activeMinutesToday, existing)
    }

    /**
     * Restores daily aggregates from a CSV backup in one transaction. Existing days are never
     * lowered: the larger total wins. Today's manifold state is re-seeded from the durable row so a
     * restored marker survives and the next counter sample adds only its real delta.
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
            var state = settings.readStepState(today)
            if (todayRow.steps >= state.total) {
                val marker = todayRow.estimatedSteps.coerceIn(0, todayRow.steps)
                val priorConfirmed = maxOf(state.confirmed, todayRow.steps - marker)
                state = state.copy(
                    day = today,
                    total = todayRow.steps,
                    confirmed = priorConfirmed,
                    estimateToday = maxOf(state.estimateToday, todayRow.steps),
                    updatedAt = System.currentTimeMillis(),
                )
                settings.writeStepState(state)
            }
        }
        RestoreSummary(inserted = inserted, updated = updated, kept = kept)
    }

    /**
     * Persists only the active-minute counter after the cadence closes a minute. Written separately
     * so the writer does not have to re-send sensor evidence just to record a finished minute.
     */
    suspend fun updateActiveMinutes(
        activeMinutes: Int,
        now: Long = System.currentTimeMillis(),
    ): DailyStep? = mutex.withLock {
        val today = DateKey.today(now)
        val existing = dao.today(today)?.toDomain() ?: return@withLock null
        val user = settings.current()
        val row = dayRow(today, existing.steps, existing.estimatedSteps, user, now, activeMinutes, existing)
        dao.upsert(row.toEntity())
        row
    }

    private suspend fun upsertDay(
        date: String,
        steps: Int,
        estimatedSteps: Int,
        user: UserSettings,
        now: Long,
        activeMinutes: Int,
        existing: DailyStep?,
    ) {
        dao.upsert(dayRow(date, steps, estimatedSteps, user, now, activeMinutes, existing).toEntity())
    }

    private fun dayRow(
        date: String,
        steps: Int,
        estimatedSteps: Int,
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
        estimatedSteps = estimatedSteps.coerceIn(0, steps.coerceAtLeast(0)),
    )

    /**
     * Active minutes are measured by the service's cadence. There is deliberately no synthetic
     * fallback: a missing value means "not measured", not "invent a number".
     */
    private fun resolveActiveMinutes(activeMinutes: Int, existing: DailyStep?): Int =
        if (activeMinutes >= 0) activeMinutes.coerceAtLeast(0) else existing?.activeMinutes?.coerceAtLeast(0) ?: 0

    suspend fun clearAll() = dao.clear()
}