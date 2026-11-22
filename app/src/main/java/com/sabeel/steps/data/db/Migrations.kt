package com.sabeel.steps.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Current Room schema version. [AppDatabase] references this constant; every schema bump must add a
 * matching migration and (when Gradle/KSP runs) a committed schema JSON under `app/schemas/...`.
 */
const val ROOM_SCHEMA_VERSION = 3

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

/**
 * v2 -> v3 adds the custom-session model (council amendment A7, plan section 6.1):
 *
 * - `walks` gains the session columns. Existing walks become WALK / NONE / COMPLETED with no GPS and
 *   no places; their route JSON is untouched, so no pre-v3 walk loses its route.
 * - `walk_legs`: one row per local date of a session that crossed midnight.
 * - `places`: user-defined local places (label + coordinates + radius, no geocoding).
 * - `session_presets`: saved custom-session templates.
 *
 * The statements are a public list (not inlined) so a pure-JVM test can guard column names,
 * defaults and "no destructive SQL" without an emulator.
 */
val MIGRATION_2_3_STATEMENTS: List<String> = listOf(
    "ALTER TABLE `walks` ADD COLUMN `activityType` TEXT NOT NULL DEFAULT 'WALK'",
    "ALTER TABLE `walks` ADD COLUMN `goalType` TEXT NOT NULL DEFAULT 'NONE'",
    "ALTER TABLE `walks` ADD COLUMN `goalValue` REAL NOT NULL DEFAULT 0",
    "ALTER TABLE `walks` ADD COLUMN `targetMet` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `walks` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'COMPLETED'",
    "ALTER TABLE `walks` ADD COLUMN `gpsUsed` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `walks` ADD COLUMN `startPlaceId` INTEGER",
    "ALTER TABLE `walks` ADD COLUMN `endPlaceId` INTEGER",
    "ALTER TABLE `walks` ADD COLUMN `sessionPresetId` INTEGER",
    "CREATE TABLE IF NOT EXISTS `places` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`label` TEXT NOT NULL, " +
        "`lat` REAL NOT NULL, " +
        "`lon` REAL NOT NULL, " +
        "`radiusM` REAL NOT NULL, " +
        "`createdAt` INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `session_presets` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`name` TEXT NOT NULL, " +
        "`activityType` TEXT NOT NULL, " +
        "`goalType` TEXT NOT NULL, " +
        "`goalValue` REAL NOT NULL, " +
        "`useGps` INTEGER NOT NULL, " +
        "`startPlaceId` INTEGER, " +
        "`endPlaceId` INTEGER, " +
        "`lastUsedAt` INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `walk_legs` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`walkId` INTEGER NOT NULL, " +
        "`date` TEXT NOT NULL, " +
        "`steps` INTEGER NOT NULL, " +
        "`distanceMeters` REAL NOT NULL, " +
        "`durationSec` INTEGER NOT NULL, " +
        "FOREIGN KEY(`walkId`) REFERENCES `walks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
    "CREATE INDEX IF NOT EXISTS `index_walk_legs_walkId` ON `walk_legs` (`walkId`)",
)

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_2_3_STATEMENTS.forEach { db.execSQL(it) }
    }
}
