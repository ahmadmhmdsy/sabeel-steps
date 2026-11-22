package com.sabeel.steps.domain.session

import com.sabeel.steps.data.db.MIGRATION_2_3_STATEMENTS
import com.sabeel.steps.data.db.PlaceEntity
import com.sabeel.steps.data.db.ROOM_SCHEMA_VERSION
import com.sabeel.steps.data.db.SessionPresetEntity
import com.sabeel.steps.data.db.WalkEntity
import com.sabeel.steps.data.db.WalkLegEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM guard for the v2 -> v3 migration SQL. A Room migration cannot be executed without an
 * emulator (androidTest infra is not present yet), so this test pins the parts that reviews and the
 * schema JSON later depend on: every new entity property has a column, the NOT NULL additions carry
 * the documented SQL defaults (otherwise existing v2 rows cannot be migrated), the leg cascade and
 * index exist, and nothing destructive or route-rewriting is in the statement list.
 */
class SessionSchemaSqlTest {

    private val statements = MIGRATION_2_3_STATEMENTS

    @Test
    fun schemaVersionWasBumpedForSessions() {
        assertEquals(3, ROOM_SCHEMA_VERSION)
    }

    @Test
    fun everyNewWalkSessionPropertyGetsAMigrationColumn() {
        val v2Columns = setOf(
            "id", "startedAt", "endedAt", "durationSec", "steps", "distanceMeters", "secondsPerKm",
            "routeJson", "estimated",
        )
        val newProperties = fieldsOf(WalkEntity::class.java) - v2Columns
        assertEquals(
            "MIGRATION_2_3 must add exactly the new WalkEntity properties",
            newProperties,
            alteredColumns(),
        )
        assertTrue(
            alteredColumns().containsAll(
                listOf(
                    "activityType", "goalType", "goalValue", "targetMet", "status", "gpsUsed",
                    "startPlaceId", "endPlaceId", "sessionPresetId",
                ),
            ),
        )
    }

    @Test
    fun notNullAddedColumnsCarryADefaultSoExistingRowsMigrate() {
        statements.filter { it.contains("ADD COLUMN") }.forEach { sql ->
            if (sql.uppercase().contains("NOT NULL")) {
                assertTrue("NOT NULL column without DEFAULT: " + sql, sql.uppercase().contains("DEFAULT"))
            }
        }
        assertTrue(statements.any { it.contains("`activityType` TEXT NOT NULL DEFAULT 'WALK'") })
        assertTrue(statements.any { it.contains("`goalType` TEXT NOT NULL DEFAULT 'NONE'") })
        assertTrue(statements.any { it.contains("`status` TEXT NOT NULL DEFAULT 'COMPLETED'") })
        assertTrue(statements.any { it.contains("`estimated` INTEGER") || !it.contains("`estimated`") })
    }

    @Test
    fun newSessionTablesAreCreated() {
        assertTrue(statements.any { it.startsWith("CREATE TABLE IF NOT EXISTS `places`") })
        assertTrue(statements.any { it.startsWith("CREATE TABLE IF NOT EXISTS `session_presets`") })
        assertTrue(statements.any { it.startsWith("CREATE TABLE IF NOT EXISTS `walk_legs`") })
    }

    @Test
    fun everyNewEntityPropertyHasAColumnInItsCreateTableStatement() {
        mapOf(
            "places" to fieldsOf(PlaceEntity::class.java),
            "session_presets" to fieldsOf(SessionPresetEntity::class.java),
            "walk_legs" to fieldsOf(WalkLegEntity::class.java),
        ).forEach { (table, fields) ->
            val create = statements.first { it.startsWith("CREATE TABLE IF NOT EXISTS `" + table + "`") }
            fields.forEach { field ->
                assertTrue(table + " is missing a column for property " + field + ": " + create, create.contains("`" + field + "`"))
            }
        }
    }

    @Test
    fun walkLegsCascadeWithTheirWalk() {
        val create = statements.first { it.startsWith("CREATE TABLE IF NOT EXISTS `walk_legs`") }
        assertTrue(create.contains("FOREIGN KEY(`walkId`) REFERENCES `walks`(`id`)"))
        assertTrue(create.contains("ON DELETE CASCADE"))
        assertTrue(statements.any { it.startsWith("CREATE INDEX IF NOT EXISTS `index_walk_legs_walkId`") })
    }

    @Test
    fun migrationIsAdditiveOnlyAndKeepsSavedRoutes() {
        val forbidden = listOf("DROP TABLE", "DROP COLUMN", "DELETE FROM", "TRUNCATE", "INSERT INTO")
        statements.forEach { sql ->
            val upper = sql.uppercase()
            forbidden.forEach { word -> assertFalse("destructive statement (" + word + "): " + sql, upper.contains(word)) }
            assertFalse("migration must not rewrite saved routes: " + sql, upper.contains("ROUTEJSON"))
        }
    }

    @Test
    fun statementListHasNoDuplicates() {
        assertEquals(statements.size, statements.toSet().size)
    }

    private fun alteredColumns(): Set<String> {
        val regex = Regex("ADD COLUMN `([^`]+)`")
        return statements.mapNotNull { regex.find(it)?.groupValues?.get(1) }.toSet()
    }

    private fun fieldsOf(cls: Class<*>): Set<String> =
        cls.declaredFields
            // Compose stability fields (`$stable`) and the companion singleton are compiler
            // artifacts, not entity columns.
            .filterNot { it.isSynthetic || it.name.startsWith('$') || it.name == "Companion" }
            .map { it.name }
            .toSet()
}
