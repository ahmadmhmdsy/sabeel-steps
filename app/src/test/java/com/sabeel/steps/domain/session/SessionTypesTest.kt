package com.sabeel.steps.domain.session

import com.sabeel.steps.domain.Walk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Pre-v3 rows and legacy presets must keep working: tolerant storage parses are part of the contract. */
class SessionTypesTest {

    @Test
    fun activityTypeParsingFallsBackToWalk() {
        assertEquals(ActivityType.WALK, ActivityType.fromStorage(null))
        assertEquals(ActivityType.WALK, ActivityType.fromStorage(""))
        assertEquals(ActivityType.WALK, ActivityType.fromStorage("walk"))
        assertEquals(ActivityType.WALK, ActivityType.fromStorage("BOGUS"))
        assertEquals(ActivityType.RUN, ActivityType.fromStorage("RUN"))
        assertEquals(ActivityType.RUN, ActivityType.fromStorage(" RUN "))
    }

    @Test
    fun sessionStatusParsingFallsBackToCompleted() {
        assertEquals(SessionStatus.COMPLETED, SessionStatus.fromStorage(null))
        assertEquals(SessionStatus.COMPLETED, SessionStatus.fromStorage("BOGUS"))
        assertEquals(SessionStatus.PAUSED, SessionStatus.fromStorage("PAUSED"))
    }

    @Test
    fun goalTypeParsingFallsBackToNone() {
        assertEquals(GoalType.NONE, GoalType.fromStorage(null))
        assertEquals(GoalType.NONE, GoalType.fromStorage("BOGUS"))
        assertEquals(GoalType.DISTANCE, GoalType.fromStorage("DISTANCE"))
    }

    @Test
    fun aPlainWalkStillDefaultsToNoSession() {
        val walk = Walk(startedAt = 1L, endedAt = 2L, durationSec = 1)
        assertEquals(ActivityType.WALK, walk.activityType)
        assertEquals(GoalType.NONE, walk.goalType)
        assertEquals(SessionStatus.COMPLETED, walk.status)
        assertEquals(0.0, walk.goalValue, 0.0)
        assertFalse(walk.targetMet)
        assertFalse(walk.gpsUsed)
        assertNull(walk.startPlaceId)
        assertNull(walk.endPlaceId)
        assertNull(walk.sessionPresetId)
    }

    @Test
    fun placeAndPresetHaveUsableDefaults() {
        val place = Place(label = "Home", lat = 33.3, lon = 44.4)
        assertEquals(DEFAULT_PLACE_RADIUS_M, place.radiusM, 0.0)

        val preset = SessionPreset(name = "Morning walk")
        assertEquals(ActivityType.WALK, preset.activityType)
        assertEquals(GoalType.NONE, preset.goalType)
        assertFalse(preset.useGps)
    }
}
