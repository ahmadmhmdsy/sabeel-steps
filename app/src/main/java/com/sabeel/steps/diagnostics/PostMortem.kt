package com.sabeel.steps.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the previous process was doing when it died, captured in Application.onCreate before any
 * AppContainer work can overwrite the evidence (app-open restart rewrites the heartbeat).
 *
 * ApplicationExitInfo exists from Android 11 (API 30). On older phones the same summary is written
 * from the persisted facts, so the report still shows "last clean stop / heartbeat age / gap".
 */
data class ExitRecord(
    val at: Long,
    val reason: Int,
    val reasonName: String,
    val pssKb: Long,
    val rssKb: Long,
    val importance: Int,
    val description: String,
    val stateSummary: String,
) {
    fun toFields(): Map<String, Any?> = mapOf(
        "at" to at,
        "reason" to reasonName,
        "pss" to pssKb,
        "rss" to rssKb,
        "imp" to importance,
        "desc" to description,
    )
}

object PostMortem {

    private const val MAX_RECORDS = 8

    /** Called first in SabeelApp.onCreate. Never throws. */
    fun capture(context: Context) {
        runCatching {
            val app = context.applicationContext
            val now = System.currentTimeMillis()
            val bootWallBase = now - SystemClock.elapsedRealtime()
            val facts = FlightRecorder.facts()
            val reboot = facts.bootWallBase > 0L &&
                kotlin.math.abs(bootWallBase - facts.bootWallBase) > 120_000L
            val exits = readExits(app)
            FlightRecorder.writeProcessStart(now, bootWallBase)
            FlightRecorder.writeExitRecords(encode(exits))
            FlightRecorder.logSync(
                "process_start_facts",
                mapOf(
                    "gap_ms" to age(facts.lastHeartbeatAt, now),
                    "last_end_age_ms" to age(facts.lastProcessEndAt, now),
                    "last_end_reason" to facts.lastProcessEndReason,
                    "reboot" to reboot,
                    "wall_elapsed" to bootWallBase,
                    "last_boot_wall" to facts.bootWallBase,
                    "starts" to facts.starts,
                    "prev_crash" to facts.lastCrashType,
                    "prev_crash_age_ms" to age(facts.lastCrashAt, now),
                    "exit_count" to exits.size,
                ),
            )
            for (record in exits.take(4)) {
                FlightRecorder.logSync("exit_record", record.toFields())
            }
            val previousEnd = exits.firstOrNull()?.reasonName
                ?: facts.lastProcessEndReason.ifEmpty { "UNKNOWN" }
            FlightRecorder.logSync(
                "previous_process",
                mapOf(
                    "end" to previousEnd,
                    "heartbeat_age_ms" to age(facts.lastHeartbeatAt, now),
                    "sensor_age_ms" to age(facts.lastSensorAt, now),
                    "worker_age_ms" to age(facts.lastWorkerAt, now),
                    "alarm_age_ms" to age(facts.lastAlarmAt, now),
                    "last_start" to facts.lastStartResult,
                ),
            )
        }
    }

    fun readExits(context: Context): List<ExitRecord> {
        val stored = decode(FlightRecorder.readExitRecords().orEmpty()).orEmpty()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return stored
        // Always ask Android again: the stored snapshot is only a fallback. Preferring it would
        // freeze the report on the first exit it ever saw (the install update) and hide every later
        // force-stop, which is exactly the event this field build exists to observe.
        val live = readLive(context)
        return if (live.isNotEmpty()) live else stored
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readLive(context: Context): List<ExitRecord> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return runCatching {
            val manager = context.getSystemService(ActivityManager::class.java) ?: return emptyList()
            manager.getHistoricalProcessExitReasons(null, 0, MAX_RECORDS)
                .mapNotNull { info -> info?.toRecord() }
        }.getOrDefault(emptyList())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun ApplicationExitInfo.toRecord(): ExitRecord? = runCatching {
        ExitRecord(
            at = timestamp,
            reason = reason,
            reasonName = reasonName(reason),
            pssKb = pss,
            rssKb = rss,
            importance = importance,
            description = description.orEmpty().take(120),
            stateSummary = processStateSummary?.toString(Charsets.UTF_8).orEmpty().take(120),
        )
    }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.R)
    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "PACKAGE_STATE"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "PACKAGE_UPDATED"
        ApplicationExitInfo.REASON_FREEZER -> "FREEZER"
        else -> "REASON_" + reason
    }

    private fun encode(records: List<ExitRecord>): String {
        val array = JSONArray()
        records.take(MAX_RECORDS).forEach { record ->
            array.put(
                JSONObject()
                    .put("at", record.at)
                    .put("reason", record.reason)
                    .put("reason_name", record.reasonName)
                    .put("pss", record.pssKb)
                    .put("rss", record.rssKb)
                    .put("imp", record.importance)
                    .put("desc", record.description)
                    .put("summary", record.stateSummary),
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<ExitRecord>? = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                add(
                    ExitRecord(
                        at = obj.optLong("at", 0L),
                        reason = obj.optInt("reason", 0),
                        reasonName = obj.optString("reason_name", "UNKNOWN"),
                        pssKb = obj.optLong("pss", 0L),
                        rssKb = obj.optLong("rss", 0L),
                        importance = obj.optInt("imp", 0),
                        description = obj.optString("desc", ""),
                        stateSummary = obj.optString("summary", ""),
                    ),
                )
            }
        }
    }.getOrNull()

    private fun age(then: Long, now: Long): Long =
        if (then <= 0L) -1L else (now - then).coerceAtLeast(0L)
}
