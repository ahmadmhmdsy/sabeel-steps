package com.sabeel.steps.domain

import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.domain.session.SessionStatus

enum class Units { METRIC, IMPERIAL }
enum class Gender { UNSPECIFIED, MALE, FEMALE }
enum class AppLanguage(val code: String) {
    EN("en"),
    AR("ar");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}

/** One local calendar day of aggregates. date = ISO yyyy-MM-dd in the device time zone. */
data class DailyStep(
    val date: String,
    val steps: Int,
    val distanceMeters: Double = 0.0,
    val calories: Double = 0.0,
    val activeMinutes: Int = 0,
    val updatedAt: Long = 0L,
    /**
     * How many of [steps] were produced by the labelled motion estimate rather than a measured
     * sensor. 0 on days with no estimated steps, and also on rows written before this marker was
     * recorded (the app never invents a marker it did not measure).
     */
    val estimatedSteps: Int = 0,
)

data class RoutePoint(val lat: Double, val lng: Double, val t: Long)

data class Walk(
    val id: Long = 0L,
    val startedAt: Long,
    val endedAt: Long,
    val durationSec: Int,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val secondsPerKm: Int = 0,
    val route: List<RoutePoint> = emptyList(),
    /** True when at least part of this walk's step count came from the labelled estimate. */
    val estimated: Boolean = false,
    // Custom-session fields (Room schema v3). Defaults keep every pre-session call site valid: a
    // plain walk is still WALK / NONE / COMPLETED with no GPS and no places.
    val activityType: ActivityType = ActivityType.WALK,
    val goalType: GoalType = GoalType.NONE,
    /** Goal value in the goal's unit: seconds (TIME), steps (STEPS), metres (DISTANCE), ignored otherwise. */
    val goalValue: Double = 0.0,
    /** True when the session's goal had been reached at least once before the session was saved. */
    val targetMet: Boolean = false,
    val status: SessionStatus = SessionStatus.COMPLETED,
    /** True when GPS was recorded for this session (foreground only, never boot-started). */
    val gpsUsed: Boolean = false,
    val startPlaceId: Long? = null,
    val endPlaceId: Long? = null,
    val sessionPresetId: Long? = null,
)

data class UserSettings(
    val goalSteps: Int = 8000,
    val units: Units = Units.METRIC,
    val heightCm: Int = 170,
    val weightKg: Int = 70,
    val gender: Gender = Gender.UNSPECIFIED,
    /** 0 = derive from height/gender. */
    val strideCm: Int = 0,
    val language: AppLanguage = AppLanguage.EN,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 19 * 60,
    val onboardingDone: Boolean = false,
)