package com.sabeel.steps.diagnostics

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-package flight recorder for the field build.
 *
 * It answers one question after the fact: why did counting stop, and what did the app know when it
 * stopped? Everything is written as one JSON object per line to a bounded pair of files under the
 * app's private storage. Nothing leaves the phone unless the user taps "Share full report".
 *
 * Rules this class keeps:
 * - never throws into a caller and never blocks a sensor thread (all writes go through one daemon
 *   thread, sensor callers only touch a volatile timestamp),
 * - one writer at a time, so a crash cannot interleave two lines,
 * - rotation at [MAX_BYTES] with one previous file, retention of [RETENTION_MS],
 * - no free text, no identifiers, no location; event names and fixed keys only,
 * - a small facts store mirrored into SharedPreferences so the next process can read it before the
 *   recorder itself has started (post-mortem runs first in Application.onCreate),
 * - process exit records (ApplicationExitInfo) are persisted so any later report can show them.
 */
object FlightRecorder {

    const val EVENT_FILE = "events.jsonl"
    const val ROTATED_FILE = "events.1.jsonl"
    const val MAX_BYTES = 512L * 1024L
    const val RETENTION_MS = 14L * 24L * 60L * 60L * 1000L

    private const val PREFS = "sabeel_flight_recorder"
    private const val KEY_SEQ = "seq"
    private const val KEY_HEARTBEAT = "last_heartbeat_at"
    private const val KEY_SENSOR = "last_sensor_at"
    private const val KEY_WRITE = "last_write_at"
    private const val KEY_WORKER_AT = "last_worker_at"
    private const val KEY_WORKER_RESULT = "last_worker_result"
    private const val KEY_START_AT = "last_start_at"
    private const val KEY_START_RESULT = "last_start_result"
    private const val KEY_ALARM_AT = "last_alarm_at"
    private const val KEY_ALARM_RESULT = "last_alarm_result"
    private const val KEY_CRASH_AT = "last_crash_at"
    private const val KEY_CRASH_TYPE = "last_crash_type"
    private const val KEY_PROCESS_START = "last_process_start_at"
    private const val KEY_PROCESS_END = "last_process_end_at"
    private const val KEY_PROCESS_END_REASON = "last_process_end_reason"
    private const val KEY_BOOT_WALL = "boot_wall_base"
    private const val KEY_BOOT_COUNT = "start_count"
    private const val KEY_HEARTBEAT_COUNT = "heartbeat_count"
    private const val KEY_EXITS = "exit_records"

    private const val HEARTBEAT_LOG_GAP_MS = 5L * 60L * 1000L
    private const val WRITE_LOG_GAP_MS = 5L * 60L * 1000L
    private const val SENSOR_LOG_GAP_MS = 60L * 1000L

    private val started = AtomicBoolean(false)
    private val lock = Any()
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sabeel-recorder").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var seq: Long = 0L

    @Volatile
    private var lastHeartbeatLogAt: Long = 0L

    @Volatile
    private var lastWriteLogAt: Long = 0L

    @Volatile
    private var lastSensorLogAt: Long = 0L

    private val isoFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

    /** Called once from Application.onCreate, before any AppContainer work. Safe to call twice. */
    fun install(context: Context) {
        if (!started.compareAndSet(false, true)) return
        appContext = context.applicationContext
        seq = prefs().getLong(KEY_SEQ, 0L)
        installCrashHandler()
        writer.execute { maintain() }
        log(
            "process_start",
            mapOf(
                "app" to com.sabeel.steps.BuildConfig.VERSION_NAME,
                "diag" to com.sabeel.steps.BuildConfig.FIELD_DIAGNOSTICS,
                "gap_ms" to ageMs(prefs().getLong(KEY_HEARTBEAT, 0L)),
                "prev_start" to prefs().getLong(KEY_PROCESS_START, 0L),
            ),
        )
    }

    /** Fire-and-forget event. Cheap enough for a sensor callback: only an executor submit. */
    fun log(event: String, fields: Map<String, Any?> = emptyMap()) {
        if (!started.get()) return
        writer.execute { runCatching { writeLine(event, fields, sync = false) } }
    }

    /**
     * Event written on the calling thread and fsynced. Only for transitions a crash might swallow:
     * process start, crash, clean stop, export and wipe.
     */
    fun logSync(event: String, fields: Map<String, Any?> = emptyMap()) {
        if (!started.get()) return
        synchronized(lock) { runCatching { writeLine(event, fields, sync = true) } }
    }

    /** Heartbeat from the counting service: facts every beat, one log line every five minutes. */
    fun heartbeat(context: Context, fields: Map<String, Any?> = emptyMap()) {
        if (!started.get()) return
        val now = System.currentTimeMillis()
        runCatching {
            prefs().edit()
                .putLong(KEY_HEARTBEAT, now)
                .putInt(KEY_HEARTBEAT_COUNT, prefs().getInt(KEY_HEARTBEAT_COUNT, 0) + 1)
                .apply()
        }
        setBreadcrumb(context, "hb=" + (now / 1000L))
        if (now - lastHeartbeatLogAt >= HEARTBEAT_LOG_GAP_MS) {
            lastHeartbeatLogAt = now
            log("heartbeat", fields)
        }
    }

    /** Records that a sensor sample arrived without logging every sample or its raw value. */
    fun noteSensor(source: String) {
        if (!started.get()) return
        val now = System.currentTimeMillis()
        runCatching { prefs().edit().putLong(KEY_SENSOR, now).apply() }
        if (now - lastSensorLogAt >= SENSOR_LOG_GAP_MS) {
            lastSensorLogAt = now
            log("sensor_live", mapOf("src" to source))
        }
    }

    /** Writer success is throttled; a failure is always worth a line. */
    fun noteWrite(todaySteps: Int, failed: Boolean = false) {
        if (!started.get()) return
        val now = System.currentTimeMillis()
        runCatching { prefs().edit().putLong(KEY_WRITE, now).apply() }
        if (failed) {
            log("write_failed", mapOf("today" to todaySteps))
        } else if (now - lastWriteLogAt >= WRITE_LOG_GAP_MS) {
            lastWriteLogAt = now
            log("write_ok", mapOf("today" to todaySteps))
        }
    }

    fun recordWorker(result: String, detail: Map<String, Any?> = emptyMap()) {
        val now = System.currentTimeMillis()
        runCatching {
            prefs().edit()
                .putLong(KEY_WORKER_AT, now)
                .putString(KEY_WORKER_RESULT, result)
                .apply()
        }
        log("worker", detail + mapOf("result" to result))
    }

    fun recordStart(result: String, map: Map<String, Any?> = emptyMap()) {
        val now = System.currentTimeMillis()
        runCatching {
            prefs().edit()
                .putLong(KEY_START_AT, now)
                .putString(KEY_START_RESULT, result)
                .apply()
        }
        log("start_result", map + ("result" to result))
    }

    fun recordAlarm(result: String, map: Map<String, Any?> = emptyMap()) {
        val now = System.currentTimeMillis()
        runCatching {
            prefs().edit()
                .putLong(KEY_ALARM_AT, now)
                .putString(KEY_ALARM_RESULT, result)
                .apply()
        }
        log("alarm_result", map + ("result" to result))
    }

    fun recordCrash(type: String, frames: String) {
        val now = System.currentTimeMillis()
        runCatching {
            prefs().edit()
                .putLong(KEY_CRASH_AT, now)
                .putString(KEY_CRASH_TYPE, type)
                .apply()
        }
        logSync("crash_facts", mapOf("type" to type, "frames" to frames))
    }

    fun recordCleanStop(reason: String) {
        val now = System.currentTimeMillis()
        runCatching {
            prefs().edit()
                .putLong(KEY_PROCESS_END, now)
                .putString(KEY_PROCESS_END_REASON, reason)
                .apply()
        }
        logSync("clean_stop", mapOf("reason" to reason))
    }

    /** Persisted ApplicationExitInfo records, written/read by [PostMortem]. */
    fun writeExitRecords(json: String) {
        runCatching { prefs().edit().putString(KEY_EXITS, json).apply() }
    }

    fun readExitRecords(): String? = runCatching { prefs().getString(KEY_EXITS, null) }.getOrNull()

    fun writeProcessStart(now: Long, bootWallBase: Long) {
        runCatching {
            prefs().edit()
                .putLong(KEY_PROCESS_START, now)
                .putLong(KEY_BOOT_WALL, bootWallBase)
                .putInt(KEY_BOOT_COUNT, prefs().getInt(KEY_BOOT_COUNT, 0) + 1)
                .apply()
        }
    }

    fun facts(): Facts {
        val p = prefs()
        return Facts(
            lastHeartbeatAt = p.getLong(KEY_HEARTBEAT, 0L),
            lastSensorAt = p.getLong(KEY_SENSOR, 0L),
            lastWriteAt = p.getLong(KEY_WRITE, 0L),
            lastWorkerAt = p.getLong(KEY_WORKER_AT, 0L),
            lastWorkerResult = p.getString(KEY_WORKER_RESULT, "") ?: "",
            lastStartAt = p.getLong(KEY_START_AT, 0L),
            lastStartResult = p.getString(KEY_START_RESULT, "") ?: "",
            lastAlarmAt = p.getLong(KEY_ALARM_AT, 0L),
            lastAlarmResult = p.getString(KEY_ALARM_RESULT, "") ?: "",
            lastCrashAt = p.getLong(KEY_CRASH_AT, 0L),
            lastCrashType = p.getString(KEY_CRASH_TYPE, "") ?: "",
            lastProcessStartAt = p.getLong(KEY_PROCESS_START, 0L),
            lastProcessEndAt = p.getLong(KEY_PROCESS_END, 0L),
            lastProcessEndReason = p.getString(KEY_PROCESS_END_REASON, "") ?: "",
            bootWallBase = p.getLong(KEY_BOOT_WALL, 0L),
            starts = p.getInt(KEY_BOOT_COUNT, 0),
            heartbeats = p.getInt(KEY_HEARTBEAT_COUNT, 0),
        )
    }

    fun setBreadcrumb(context: Context, value: String) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return
        runCatching {
            context.getSystemService(android.app.ActivityManager::class.java)
                ?.setProcessStateSummary(value.toByteArray(Charsets.UTF_8))
        }
    }

    fun eventsInfo(context: Context): EventStats {
        val dir = diagnosticsDir(context)
        val files = listOf(File(dir, ROTATED_FILE), File(dir, EVENT_FILE)).filter { it.isFile }
        var bytes = 0L
        var lines = 0
        for (file in files) {
            bytes += file.length()
            runCatching {
                file.bufferedReader().useLines { sequence -> lines += sequence.count() }
            }
        }
        return EventStats(lines, bytes)
    }

    /** Concatenates both files; the returned text starts at a line boundary. */
    fun readEventsText(context: Context): String {
        val dir = diagnosticsDir(context)
        val builder = StringBuilder()
        for (name in listOf(ROTATED_FILE, EVENT_FILE)) {
            val file = File(dir, name)
            if (!file.isFile) continue
            runCatching { builder.append(file.readText()) }
        }
        return builder.toString()
    }

    /** Deletes every recorded event and every fact, keeping nothing that could identify the tester. */
    fun wipe(context: Context) {
        val dir = diagnosticsDir(context)
        synchronized(lock) {
            runCatching { File(dir, ROTATED_FILE).delete() }
            runCatching { File(dir, EVENT_FILE).delete() }
            runCatching { prefs().edit().clear().apply() }
        }
        logSync("diagnostics_cleared", mapOf("reason" to "user"))
    }

    private fun maintain() {
        val context = appContext ?: return
        val dir = diagnosticsDir(context)
        val current = File(dir, EVENT_FILE)
        val rotated = File(dir, ROTATED_FILE)
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        if (rotated.isFile && rotated.lastModified() < cutoff) rotated.delete()
        if (current.isFile && current.length() > MAX_BYTES) rotate(current, rotated)
    }

    private fun writeLine(event: String, fields: Map<String, Any?>, sync: Boolean) {
        val context = appContext ?: return
        val now = System.currentTimeMillis()
        val line = buildString {
            val record = JSONObject()
            record.put("ts", now)
            record.put("t", isoFormat.format(Instant.ofEpochMilli(now)))
            record.put("seq", seq)
            record.put("pid", android.os.Process.myPid())
            record.put("up", SystemClock.elapsedRealtime())
            record.put("e", event)
            for ((key, value) in fields) {
                when (value) {
                    null -> record.put(key, JSONObject.NULL)
                    is Boolean, is Int, is Long, is Float, is Double -> record.put(key, value)
                    is String -> record.put(key, if (value.length > 200) value.take(200) else value)
                    is List<*> -> {
                        val array = JSONArray()
                        value.take(12).forEach { item ->
                            if (item == null) array.put(JSONObject.NULL) else array.put(item.toString().take(120))
                        }
                        record.put(key, array)
                    }
                    else -> record.put(key, value.toString().take(200))
                }
            }
            append(record.toString())
            append('\n')
        }
        seq += 1
        val dir = diagnosticsDir(context)
        val current = File(dir, EVENT_FILE)
        if (current.isFile && current.length() + line.length > MAX_BYTES) {
            rotate(current, File(dir, ROTATED_FILE))
        }
        FileOutputStream(current, true).use { out ->
            out.write(line.toByteArray(Charsets.UTF_8))
            if (sync) out.fd.sync()
        }
        runCatching { prefs().edit().putLong(KEY_SEQ, seq).apply() }
    }

    private fun rotate(current: File, rotated: File) {
        runCatching {
            if (rotated.isFile) rotated.delete()
            current.renameTo(rotated)
        }
    }

    private fun diagnosticsDir(context: Context): File =
        File(context.filesDir, "diagnostics").apply { if (!isDirectory) mkdirs() }

    private fun prefs(): SharedPreferences =
        appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun ageMs(then: Long): Long = if (then <= 0L) -1L else (System.currentTimeMillis() - then).coerceAtLeast(0L)

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val frames = error.stackTrace.take(12).joinToString(";") { frame ->
                    frame.className.take(80) + "." + frame.methodName + ":" + frame.lineNumber
                }
                logSync(
                    "crash",
                    mapOf(
                        "type" to (error.javaClass.name ?: "unknown"),
                        "frames" to frames,
                        "thread" to thread.name.take(40),
                    ),
                )
                recordCrash(error.javaClass.name ?: "unknown", frames)
                synchronized(lock) {
                    runCatching {
                        val current = File(diagnosticsDir(appContext!!), EVENT_FILE)
                        if (current.isFile) FileOutputStream(current, true).use { it.fd.sync() }
                    }
                }
            }
            if (previous != null) previous.uncaughtException(thread, error)
        }
    }

    data class Facts(
        val lastHeartbeatAt: Long,
        val lastSensorAt: Long,
        val lastWriteAt: Long,
        val lastWorkerAt: Long,
        val lastWorkerResult: String,
        val lastStartAt: Long,
        val lastStartResult: String,
        val lastAlarmAt: Long,
        val lastAlarmResult: String,
        val lastCrashAt: Long,
        val lastCrashType: String,
        val lastProcessStartAt: Long,
        val lastProcessEndAt: Long,
        val lastProcessEndReason: String,
        val bootWallBase: Long,
        val starts: Int,
        val heartbeats: Int,
    )

    data class EventStats(val lines: Int, val bytes: Long)
}
