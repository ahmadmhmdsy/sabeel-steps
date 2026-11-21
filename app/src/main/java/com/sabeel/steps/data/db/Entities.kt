package com.sabeel.steps.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sabeel.steps.domain.DailyStep
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.Walk
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