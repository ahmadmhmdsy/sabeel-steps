package com.sabeel.steps.domain

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