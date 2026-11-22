package com.sabeel.steps.domain.session

import com.sabeel.steps.domain.Walk

/** What kind of movement a custom session records. Stored as its enum name in the database. */
enum class ActivityType {
    WALK,
    RUN;

    companion object {
        /** Tolerant storage parse: null, blank and unknown legacy values become [WALK]. */
        fun fromStorage(value: String?): ActivityType =
            entries.firstOrNull { it.name == value?.trim() } ?: WALK
    }
}

/**
 * Lifecycle of a custom session. Only one session can be active at a time (council amendment A7);
 * a session that was never finished is persisted as [CANCELLED].
 */
enum class SessionStatus {
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED;

    companion object {
        /** Tolerant storage parse: unknown legacy values become [COMPLETED] (the pre-v3 default). */
        fun fromStorage(value: String?): SessionStatus =
            entries.firstOrNull { it.name == value?.trim() } ?: COMPLETED
    }
}

/** Default geofence radius for a locally defined place, in metres. */
const val DEFAULT_PLACE_RADIUS_M = 100.0

/**
 * A locally defined place (label + coordinates + radius). Coordinates are supplied by the user on
 * a map or by "use current location"; nothing is geocoded or sent anywhere (amendment A10).
 */
data class Place(
    val id: Long = 0L,
    val label: String,
    val lat: Double,
    val lon: Double,
    val radiusM: Double = DEFAULT_PLACE_RADIUS_M,
    val createdAt: Long = 0L,
)

/**
 * A saved custom-session template ("Morning walk", "Friday run"). Starting a session from a preset
 * copies its settings into the new session; editing the preset later never rewrites history.
 */
data class SessionPreset(
    val id: Long = 0L,
    val name: String,
    val activityType: ActivityType = ActivityType.WALK,
    val goalType: GoalType = GoalType.NONE,
    val goalValue: Double = 0.0,
    val useGps: Boolean = false,
    val startPlaceId: Long? = null,
    val endPlaceId: Long? = null,
    val lastUsedAt: Long = 0L,
)

/**
 * One calendar-day slice of a custom session. A session that crosses midnight is persisted as one
 * walk plus one leg per local date, so per-day attribution is exact and never re-counted.
 */
data class WalkLeg(
    val id: Long = 0L,
    val walkId: Long = 0L,
    /** ISO yyyy-MM-dd in the device time zone. */
    val date: String,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val durationSec: Int = 0,
)

/** A saved session together with its per-day legs. */
data class WalkWithLegs(
    val walk: Walk,
    val legs: List<WalkLeg> = emptyList(),
)
