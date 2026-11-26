package com.sabeel.steps.diagnostics

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.sabeel.steps.BuildConfig
import com.sabeel.steps.platform.StatusStore
import com.sabeel.steps.service.StepCounterService
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds the two artifacts the tester can send: one readable report.txt and a ZIP that also carries
 * the raw JSONL event stream. Everything is assembled in the app's cache directory and shared
 * through the app's own FileProvider; nothing is uploaded anywhere.
 */
object DiagnosticsReport {

    private const val CACHE_DIR = "diagnostics"
    private const val TAIL_MAX_CHARS = 96 * 1024
    private const val TAIL_MAX_LINES = 40

    fun collect(context: Context, todaySteps: Int, stateTotal: Int): ReportData {
        val app = context.applicationContext
        val facts = FlightRecorder.facts()
        val device = DeviceStateReader.read(app)
        val status = runCatching { StatusStore.read(app) }.getOrNull()
        val source = runCatching { StatusStore.readSource(app) }.getOrNull()
        val storageFault = runCatching { StatusStore.readStorageFault(app) }.getOrNull() ?: 0L
        val bootAttempts = runCatching { StatusStore.readBootAttempts(app).size }.getOrDefault(0)
        val exits = runCatching { PostMortem.readExits(app) }.getOrDefault(emptyList())
        val stats = FlightRecorder.eventsInfo(app)
        val events = FlightRecorder.readEventsText(app)
        val now = System.currentTimeMillis()
        val sensorAt = maxOf(facts.lastSensorAt, source?.lastEventAt ?: 0L)
        val tailLines = events.lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(TAIL_MAX_LINES)
        val bootWallBase = now - SystemClock.elapsedRealtime()
        val reboot = facts.bootWallBase > 0L &&
            kotlin.math.abs(bootWallBase - facts.bootWallBase) > 120_000L
        return ReportData(
            generatedAt = now,
            appVersion = BuildConfig.VERSION_NAME,
            appCode = BuildConfig.VERSION_CODE,
            fieldBuild = BuildConfig.FIELD_DIAGNOSTICS,
            deviceModel = Build.MODEL.orEmpty(),
            deviceBrand = Build.BRAND.orEmpty(),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            processPid = android.os.Process.myPid(),
            processStartAt = facts.lastProcessStartAt,
            uptimeMs = SystemClock.elapsedRealtime(),
            wallElapsedBase = bootWallBase,
            rebootDetected = reboot,
            todaySteps = todaySteps,
            stepStateTotal = stateTotal,
            statusName = status?.name ?: "unknown",
            serviceRunning = StepCounterService.runningInstance,
            storageFaultAt = storageFault,
            lastHeartbeatAt = facts.lastHeartbeatAt,
            lastSensorAt = sensorAt,
            lastWriteAt = facts.lastWriteAt,
            lastWorkerAt = facts.lastWorkerAt,
            lastWorkerResult = facts.lastWorkerResult,
            lastStartAt = facts.lastStartAt,
            lastStartResult = facts.lastStartResult,
            lastAlarmAt = facts.lastAlarmAt,
            lastAlarmResult = facts.lastAlarmResult,
            lastCrashAt = facts.lastCrashAt,
            lastCrashType = facts.lastCrashType,
            lastProcessEndAt = facts.lastProcessEndAt,
            lastProcessEndReason = facts.lastProcessEndReason,
            bootAttempts = bootAttempts,
            batteryUnrestricted = device.batteryUnrestricted,
            batteryPercent = device.batteryPercent,
            charging = device.charging,
            screenOn = device.screenOn,
            dozeMode = device.dozeMode,
            powerSave = device.powerSave,
            standbyBucket = device.appStandbyBucket,
            backgroundRestricted = device.backgroundRestricted,
            notificationsAllowed = device.notificationsAllowed,
            activityRecognition = device.activityRecognition,
            exitRecords = exits,
            eventLines = stats.lines,
            eventBytes = stats.bytes,
            eventTailLines = tailLines,
        )
    }

    fun buildText(data: ReportData): String = DiagnosticsReportText.build(data)

    /** Raw events for the ZIP: everything the recorder keeps, newest last. */
    fun eventsJsonl(context: Context): String {
        val text = FlightRecorder.readEventsText(context)
        return if (text.length <= TAIL_MAX_CHARS) text else text.takeLast(TAIL_MAX_CHARS)
    }

    fun metaJson(data: ReportData): String = JSONObject()
        .put("app", data.appVersion)
        .put("code", data.appCode)
        .put("field", data.fieldBuild)
        .put("generated", data.generatedAt)
        .put("device", data.deviceBrand + " " + data.deviceModel)
        .put("android", data.androidRelease)
        .put("sdk", data.sdkInt)
        .put("events_lines", data.eventLines)
        .put("events_bytes", data.eventBytes)
        .toString()

    /** Writes the shareable ZIP and returns it. Call from Dispatchers.IO. */
    fun buildZip(context: Context, data: ReportData): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { if (!isDirectory) mkdirs() }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(Instant.ofEpochMilli(data.generatedAt).atZone(ZoneId.systemDefault()))
        val file = File(dir, "sabeel-report-" + stamp + ".zip")
        val events = eventsJsonl(context)
        ZipOutputStream(FileOutputStream(file).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("report.txt"))
            zip.write(buildText(data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("meta.json"))
            zip.write(metaJson(data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("events.jsonl"))
            zip.write(events.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return file
    }

    /** Removes old report ZIPs; called before building a new one. */
    fun prune(context: Context) {
        runCatching {
            val dir = File(context.cacheDir, CACHE_DIR)
            val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) file.delete()
            }
        }
    }
}
