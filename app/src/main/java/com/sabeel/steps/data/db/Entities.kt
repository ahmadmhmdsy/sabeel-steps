package com.sabeel.steps.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sabeel.steps.domain.DailyStep
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.domain.session.Place
import com.sabeel.steps.domain.session.SessionPreset
import com.sabeel.steps.domain.session.SessionStatus
import com.sabeel.steps.domain.session.WalkLeg
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "daily_steps")
data class DailyStepEntity(
    @PrimaryKey val date: String,
    val steps: Int,
    val distanceMeters: Double,
    val calories: Double,
    val activeMinutes: Int,
    val updatedAt: Long,
    val estimatedSteps: Int,
)

@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,
    val endedAt: Long,
    val durationSec: Int,
    val steps: Int,
    val distanceMeters: Double,
    val secondsPerKm: Int,
    val routeJson: String,
    val estimated: Boolean,
    // ---- Room schema v3: custom sessions. Every column has a SQL default so MIGRATION_2_3 can add
    // them to pre-v3 rows, and every Kotlin property has a default so old call sites keep compiling.
    @ColumnInfo(defaultValue = "'WALK'") val activityType: String = "WALK",
    @ColumnInfo(defaultValue = "'NONE'") val goalType: String = "NONE",
    @ColumnInfo(defaultValue = "0") val goalValue: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val targetMet: Boolean = false,
    @ColumnInfo(defaultValue = "'COMPLETED'") val status: String = "COMPLETED",
    @ColumnInfo(defaultValue = "0") val gpsUsed: Boolean = false,
    val startPlaceId: Long? = null,
    val endPlaceId: Long? = null,
    val sessionPresetId: Long? = null,
)

/**
 * One calendar-day slice of a custom session. Rows are deleted with their walk
 * (ON DELETE CASCADE) and are written only through [WalkDao.insertWithLegs].
 */
@Entity(
    tableName = "walk_legs",
    foreignKeys = [
        ForeignKey(
            entity = WalkEntity::class,
            parentColumns = ["id"],
            childColumns = ["walkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["walkId"])],
)
data class WalkLegEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val walkId: Long,
    /** ISO yyyy-MM-dd in the device time zone. */
    val date: String,
    val steps: Int,
    val distanceMeters: Double,
    val durationSec: Int,
)

/** A locally defined place (label + coordinates + radius). Never geocoded (amendment A10). */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val lat: Double,
    val lon: Double,
    val radiusM: Double,
    val createdAt: Long,
)

/** A saved custom-session template. */
@Entity(tableName = "session_presets")
data class SessionPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val activityType: String,
    val goalType: String,
    val goalValue: Double,
    val useGps: Boolean,
    val startPlaceId: Long? = null,
    val endPlaceId: Long? = null,
    val lastUsedAt: Long,
)

fun DailyStepEntity.toDomain() = DailyStep(
    date = date,
    steps = steps,
    distanceMeters = distanceMeters,
    calories = calories,
    activeMinutes = activeMinutes,
    updatedAt = updatedAt,
    estimatedSteps = estimatedSteps,
)

fun DailyStep.toEntity() = DailyStepEntity(
    date = date,
    steps = steps,
    distanceMeters = distanceMeters,
    calories = calories,
    activeMinutes = activeMinutes,
    updatedAt = updatedAt,
    estimatedSteps = estimatedSteps,
)

fun WalkEntity.toDomain() = Walk(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    durationSec = durationSec,
    steps = steps,
    distanceMeters = distanceMeters,
    secondsPerKm = secondsPerKm,
    route = parseRoute(routeJson),
    estimated = estimated,
    activityType = ActivityType.fromStorage(activityType),
    goalType = GoalType.fromStorage(goalType),
    goalValue = goalValue,
    targetMet = targetMet,
    status = SessionStatus.fromStorage(status),
    gpsUsed = gpsUsed,
    startPlaceId = startPlaceId,
    endPlaceId = endPlaceId,
    sessionPresetId = sessionPresetId,
)

fun Walk.toEntity() = WalkEntity(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    durationSec = durationSec,
    steps = steps,
    distanceMeters = distanceMeters,
    secondsPerKm = secondsPerKm,
    routeJson = encodeRoute(route),
    estimated = estimated,
    activityType = activityType.name,
    goalType = goalType.name,
    goalValue = goalValue,
    targetMet = targetMet,
    status = status.name,
    gpsUsed = gpsUsed,
    startPlaceId = startPlaceId,
    endPlaceId = endPlaceId,
    sessionPresetId = sessionPresetId,
)

fun WalkLegEntity.toDomain() = WalkLeg(
    id = id,
    walkId = walkId,
    date = date,
    steps = steps,
    distanceMeters = distanceMeters,
    durationSec = durationSec,
)

fun WalkLeg.toEntity() = WalkLegEntity(
    id = id,
    walkId = walkId,
    date = date,
    steps = steps,
    distanceMeters = distanceMeters,
    durationSec = durationSec,
)

fun PlaceEntity.toDomain() = Place(
    id = id,
    label = label,
    lat = lat,
    lon = lon,
    radiusM = radiusM,
    createdAt = createdAt,
)

fun Place.toEntity() = PlaceEntity(
    id = id,
    label = label,
    lat = lat,
    lon = lon,
    radiusM = radiusM,
    createdAt = createdAt,
)

fun SessionPresetEntity.toDomain() = SessionPreset(
    id = id,
    name = name,
    activityType = ActivityType.fromStorage(activityType),
    goalType = GoalType.fromStorage(goalType),
    goalValue = goalValue,
    useGps = useGps,
    startPlaceId = startPlaceId,
    endPlaceId = endPlaceId,
    lastUsedAt = lastUsedAt,
)

fun SessionPreset.toEntity() = SessionPresetEntity(
    id = id,
    name = name,
    activityType = activityType.name,
    goalType = goalType.name,
    goalValue = goalValue,
    useGps = useGps,
    startPlaceId = startPlaceId,
    endPlaceId = endPlaceId,
    lastUsedAt = lastUsedAt,
)

fun encodeRoute(points: List<RoutePoint>): String {
    if (points.isEmpty()) return "[]"
    val arr = JSONArray()
    points.forEach { p ->
        arr.put(JSONObject().put("lat", p.lat).put("lng", p.lng).put("t", p.t))
    }
    return arr.toString()
}

fun parseRoute(json: String): List<RoutePoint> = runCatching {
    val arr = JSONArray(json)
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(RoutePoint(o.getDouble("lat"), o.getDouble("lng"), o.optLong("t")))
        }
    }
}.getOrDefault(emptyList())
