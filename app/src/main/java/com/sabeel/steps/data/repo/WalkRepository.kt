package com.sabeel.steps.data.repo

import com.sabeel.steps.data.db.AppDatabase
import com.sabeel.steps.data.db.WalkDao
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.data.db.toEntity
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.domain.session.Place
import com.sabeel.steps.domain.session.SessionPreset
import com.sabeel.steps.domain.session.WalkLeg
import com.sabeel.steps.domain.session.WalkWithLegs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads/writes saved walks, their custom-session legs, places and session presets.
 *
 * [database] is optional only for source compatibility with the pre-v3 wiring
 * (`WalkRepository(database.walkDao())`): walks and legs work without it because they go through
 * [WalkDao]. Places and presets need the database, so the integrating owner must construct it as
 * `WalkRepository(database.walkDao(), database)`; until then those calls fail fast with a clear
 * message instead of silently returning stale data.
 */
class WalkRepository(
    private val dao: WalkDao,
    private val database: AppDatabase? = null,
) {
    fun walksFlow(): Flow<List<Walk>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun get(id: Long): Walk? = dao.get(id)?.toDomain()

    /** Plain walk save; kept for WalkTrackingService, which does not attach session legs. */
    suspend fun save(walk: Walk): Long = dao.insert(walk.toEntity())

    /**
     * Saves a finished custom session and its midnight legs in one transaction. Legs are ordered
     * oldest first and their ids/walkId are assigned by the DAO; pass [WalkLeg.walkId] = 0.
     */
    suspend fun insertSession(walk: Walk, legs: List<WalkLeg>): Long =
        dao.insertWithLegs(walk.toEntity(), legs.map { it.toEntity() })

    /** One saved walk with its per-day legs; null when the walk does not exist. */
    suspend fun walkWithLegs(id: Long): WalkWithLegs? {
        val walk = dao.get(id)?.toDomain() ?: return null
        return WalkWithLegs(walk = walk, legs = dao.legsFor(id).map { it.toDomain() })
    }

    fun legsFlow(walkId: Long): Flow<List<WalkLeg>> =
        dao.observeLegs(walkId).map { rows -> rows.map { it.toDomain() } }

    // ---- Places (local label + coordinates + radius; no geocoding) ----

    fun placesFlow(): Flow<List<Place>> =
        requireDatabase("placesFlow").placeDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun places(): List<Place> =
        requireDatabase("places").placeDao().getAll().map { it.toDomain() }

    /** Inserts when [Place.id] is 0, replaces the row with that id otherwise; returns the stored id. */
    suspend fun upsertPlace(place: Place): Long =
        requireDatabase("upsertPlace").placeDao().insert(place.toEntity())

    /**
     * Deletes a place. Walks/presets that referenced it keep the dangling id on purpose: history is
     * immutable, and readers treat an unknown id as "place no longer saved" instead of rewriting it.
     */
    suspend fun deletePlace(id: Long) = requireDatabase("deletePlace").placeDao().delete(id)

    // ---- Session presets ----

    fun presetsFlow(): Flow<List<SessionPreset>> =
        requireDatabase("presetsFlow").sessionPresetDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun presets(): List<SessionPreset> =
        requireDatabase("presets").sessionPresetDao().getAll().map { it.toDomain() }

    suspend fun upsertPreset(preset: SessionPreset): Long =
        requireDatabase("upsertPreset").sessionPresetDao().insert(preset.toEntity())

    suspend fun deletePreset(id: Long) = requireDatabase("deletePreset").sessionPresetDao().delete(id)

    /** Clears every saved walk, leg, place and preset (Settings "delete all data"). */
    suspend fun clearAll() {
        dao.clearLegs()
        dao.clear()
        database?.let {
            it.placeDao().clear()
            it.sessionPresetDao().clear()
        }
    }

    private fun requireDatabase(operation: String): AppDatabase = requireNotNull(database) {
        "WalkRepository.$operation needs the AppDatabase wiring: construct it as " +
            "WalkRepository(database.walkDao(), database) in AppContainer. Walks and legs work without it."
    }
}
