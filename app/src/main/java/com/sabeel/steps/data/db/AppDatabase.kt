package com.sabeel.steps.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStepEntity::class, WalkEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun walkDao(): WalkDao
}