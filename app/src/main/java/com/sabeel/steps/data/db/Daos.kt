package com.sabeel.steps.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
interface WalkDao {
    @Query("SELECT * FROM walks ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WalkEntity>>

    @Query("SELECT * FROM walks WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): WalkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WalkEntity): Long

    @Query("SELECT * FROM walks ORDER BY startedAt DESC")
    suspend fun getAll(): List<WalkEntity>

    @Query("DELETE FROM walks")
    suspend fun clear()
}