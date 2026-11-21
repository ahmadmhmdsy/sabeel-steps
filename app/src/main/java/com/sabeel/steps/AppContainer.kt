package com.sabeel.steps

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.sabeel.steps.data.db.AppDatabase
import com.sabeel.steps.data.db.MIGRATION_1_2
import com.sabeel.steps.data.prefs.SettingsRepository
import com.sabeel.steps.data.prefs.SabeelDataStore
import com.sabeel.steps.data.repo.StepsRepository
import com.sabeel.steps.data.repo.WalkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual DI. One instance per process, created by SabeelApp. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val store: DataStore<Preferences> = appContext.SabeelDataStore

    // Schema is exported under app/schemas by the Room compiler (see build.gradle.kts). No
    // fallbackToDestructiveMigration: a missing migration must fail loudly in tests, not wipe data.
    val database: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "Sabeel.db")
        .addMigrations(MIGRATION_1_2)
        .build()

    val settings = SettingsRepository(store)
    val steps = StepsRepository(database, database.dailyStepsDao(), settings)
    val walks = WalkRepository(database.walkDao())
}