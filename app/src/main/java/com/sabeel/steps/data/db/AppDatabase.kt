package com.sabeel.steps.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyStepEntity::class,
        WalkEntity::class,
        WalkLegEntity::class,
        PlaceEntity::class,
        SessionPresetEntity::class,
    ],
    version = ROOM_SCHEMA_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun walkDao(): WalkDao
    abstract fun placeDao(): PlaceDao
    abstract fun sessionPresetDao(): SessionPresetDao
}
