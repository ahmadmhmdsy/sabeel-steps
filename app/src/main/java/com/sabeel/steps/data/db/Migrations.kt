package com.sabeel.steps.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: persist the truth markers next to the numbers they describe.
 *
 * - `daily_steps.estimatedSteps`: how many of the day's steps came from the labelled motion
 *   estimate. Existing rows keep 0 because the marker did not exist when they were written; the
 *   app must not back-fill a claim it never measured.
 * - `walks.estimated`: whether any of a saved walk's steps came from the labelled estimate.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_steps ADD COLUMN estimatedSteps INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE walks ADD COLUMN estimated INTEGER NOT NULL DEFAULT 0")
    }
}
