package com.sabeel.steps.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE date = :date LIMIT 1")
    fun observe(date: String): Flow<DailyStepEntity?>

    @Query("SELECT * FROM daily_steps WHERE date >= :fromDate ORDER BY date ASC")
    fun observeSince(fromDate: String): Flow<List<DailyStepEntity>>

    @Query("SELECT * FROM daily_steps ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyStepEntity>>

    @Query("SELECT * FROM daily_steps WHERE date >= :fromDate ORDER BY date ASC")
    suspend fun getSince(fromDate: String): List<DailyStepEntity>

    @Query("SELECT * FROM daily_steps WHERE date = :date LIMIT 1")
    suspend fun today(date: String): DailyStepEntity?

    @Upsert
    suspend fun upsert(row: DailyStepEntity)

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    suspend fun getAll(): List<DailyStepEntity>

    @Query("DELETE FROM daily_steps")
    suspend fun clear()
}

@Dao
abstract class WalkDao {
    @Query("SELECT * FROM walks ORDER BY startedAt DESC")
    abstract fun observeAll(): Flow<List<WalkEntity>>

    @Query("SELECT * FROM walks WHERE id = :id LIMIT 1")
    abstract suspend fun get(id: Long): WalkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: WalkEntity): Long

    @Query("SELECT * FROM walks ORDER BY startedAt DESC")
    abstract suspend fun getAll(): List<WalkEntity>

    @Query("DELETE FROM walks")
    abstract suspend fun clear()

    // ---- Custom-session legs (Room schema v3) ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLeg(entity: WalkLegEntity): Long

    @Query("SELECT * FROM walk_legs WHERE walkId = :walkId ORDER BY date ASC, id ASC")
    abstract fun observeLegs(walkId: Long): Flow<List<WalkLegEntity>>

    @Query("SELECT * FROM walk_legs WHERE walkId = :walkId ORDER BY date ASC, id ASC")
    abstract suspend fun legsFor(walkId: Long): List<WalkLegEntity>

    @Query("DELETE FROM walk_legs")
    abstract suspend fun clearLegs()

    /**
     * Saves one session walk and its midnight legs atomically: either the walk and every leg land,
     * or nothing does. The legs' [WalkLegEntity.walkId] and id are overwritten with the new walk id,
     * so callers pass legs built from [com.sabeel.steps.domain.session.ProjectedLeg] without ids.
     */
    @Transaction
    open suspend fun insertWithLegs(entity: WalkEntity, legs: List<WalkLegEntity>): Long {
        val walkId = insert(entity)
        legs.forEach { leg -> insertLeg(leg.copy(id = 0L, walkId = walkId)) }
        return walkId
    }
}

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY label ASC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY label ASC")
    suspend fun getAll(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): PlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaceEntity): Long

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM places")
    suspend fun clear()
}

@Dao
interface SessionPresetDao {
    @Query("SELECT * FROM session_presets ORDER BY lastUsedAt DESC, name ASC")
    fun observeAll(): Flow<List<SessionPresetEntity>>

    @Query("SELECT * FROM session_presets ORDER BY lastUsedAt DESC, name ASC")
    suspend fun getAll(): List<SessionPresetEntity>

    @Query("SELECT * FROM session_presets WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): SessionPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SessionPresetEntity): Long

    @Query("DELETE FROM session_presets WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM session_presets")
    suspend fun clear()
}
