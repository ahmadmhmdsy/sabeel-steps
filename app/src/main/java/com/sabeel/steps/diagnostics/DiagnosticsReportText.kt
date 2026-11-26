package com.sabeel.steps.diagnostics

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure formatter for the support report. Kept free of Android so it can be unit-tested and so the
 * preview shown to the user is byte-for-byte the report.txt that gets zipped.
 *
 * It contains no free text: every value is a number, a fixed enum name or a value the user already
 * sees in the app. The footer tells the tester exactly what is inside.
 */
data class ReportData(
    val generatedAt: Long,
    val appVersion: String,
    val appCode: Int,
    val fieldBuild: Boolean,
    val deviceModel: String,
    val deviceBrand: String,
    val androidRelease: String,
    val sdkInt: Int,
    val processPid: Int,
    val processStartAt: Long,
    val uptimeMs: Long,
    val wallElapsedBase: Long,
    val rebootDetected: Boolean,
    val todaySteps: Int,
    val stepStateTotal: Int,
    val statusName: String,
    val serviceRunning: Boolean,
    val storageFaultAt: Long,
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
    val lastProcessEndAt: Long,
    val lastProcessEndReason: String,
    val bootAttempts: Int,
    val batteryUnrestricted: Boolean,
    val batteryPercent: Int,
    val charging: Boolean,
    val screenOn: Boolean,
    val dozeMode: Boolean,
    val powerSave: Boolean,
    val standbyBucket: Int?,
    val backgroundRestricted: Boolean?,
    val notificationsAllowed: Boolean,
    val activityRecognition: Boolean,
    val exitRecords: List<ExitRecord>,
    val eventLines: Int,
    val eventBytes: Long,
    val eventTailLines: List<String>,
)

object DiagnosticsReportText {

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun build(data: ReportData): String {
        val now = data.generatedAt
        val zone = ZoneId.systemDefault()
        fun at(millis: Long): String =
            if (millis <= 0L) "never" else stamp.format(Instant.ofEpochMilli(millis).atZone(zone))
        fun age(millis: Long): String = when {
            millis <= 0L -> "never"
            millis < 90_000L -> (millis / 1000L).toString() + "s"
            millis < 90L * 60_000L -> (millis / 60_000L).toString() + "m"
            millis < 48L * 3_600_000L -> (millis / 3_600_000L).toString() + "h " +
                ((millis % 3_600_000L) / 60_000L).toString() + "m"
            else -> (millis / 86_400_000L).toString() + "d"
        }
        fun gap(then: Long): String = if (then <= 0L) "never" else age((now - then).coerceAtLeast(0L))
        fun flag(value: Boolean): String = if (value) "yes" else "no"
        fun orUnknown(value: Any?): String = value?.toString() ?: "unknown"

        return buildString {
            appendLine("Sabeel field diagnostics")
            appendLine("========================")
            appendLine("Generated: " + at(now) + " (" + zone.id + ")")
            appendLine("App: " + data.appVersion + " (" + data.appCode + ")" +
                (if (data.fieldBuild) " field build with diagnostics" else " public build"))
            appendLine("Device: " + data.deviceBrand + " " + data.deviceModel)
            appendLine("Android: " + data.androidRelease + " (SDK " + data.sdkInt + ")")
            appendLine()
            appendLine("[PROCESS]")
            appendLine("pid: " + data.processPid)
            appendLine("started: " + at(data.processStartAt) + " (age " + gap(data.processStartAt) + ")")
            appendLine("wall - elapsedRealtime: " + data.wallElapsedBase + " ms; device uptime: " + age(data.uptimeMs))
            appendLine("boot changed since last report: " + flag(data.rebootDetected))
            appendLine()
            appendLine("[COUNTING]")
            appendLine("today rows/steps: " + data.todaySteps)
            appendLine("pipeline state total: " + data.stepStateTotal)
            appendLine("status: " + data.statusName)
            appendLine("service instance alive in this process: " + flag(data.serviceRunning))
            appendLine("storage fault: " + (if (data.storageFaultAt > 0L) at(data.storageFaultAt) else "none"))
            appendLine()
            appendLine("[HEARTBEAT]")
            appendLine("last: " + at(data.lastHeartbeatAt) + " (age " + gap(data.lastHeartbeatAt) + ")")
            appendLine()
            appendLine("[SENSOR]")
            appendLine("last delivered sample: " + at(data.lastSensorAt) + " (age " + gap(data.lastSensorAt) + ")")
            appendLine("last write: " + at(data.lastWriteAt) + " (age " + gap(data.lastWriteAt) + ")")
            appendLine()
            appendLine("[WORKER]")
            appendLine("last run: " + at(data.lastWorkerAt) + " (age " + gap(data.lastWorkerAt) + ")")
            appendLine("result: " + data.lastWorkerResult.ifEmpty { "never" })
            appendLine()
            appendLine("[START]")
            appendLine("last: " + at(data.lastStartAt) + " (age " + gap(data.lastStartAt) + ")")
            appendLine("result: " + data.lastStartResult.ifEmpty { "never" })
            appendLine("boot attempts recorded: " + data.bootAttempts)
            appendLine()
            appendLine("[MIDNIGHT ALARM]")
            appendLine("last: " + at(data.lastAlarmAt) + " (age " + gap(data.lastAlarmAt) + ")")
            appendLine("result: " + data.lastAlarmResult.ifEmpty { "never" })
            appendLine()
            appendLine("[CRASH]")
            appendLine("last crash: " + at(data.lastCrashAt) + " (age " + gap(data.lastCrashAt) + ")")
            appendLine("type: " + data.lastCrashType.ifEmpty { "none" })
            appendLine()
            appendLine("[PROCESS END]")
            appendLine(
                "last clean stop: " + at(data.lastProcessEndAt) + " (age " + gap(data.lastProcessEndAt) + ")" +
                    " reason=" + data.lastProcessEndReason.ifEmpty { "none" },
            )
            if (data.exitRecords.isEmpty()) {
                appendLine("exit records: none (Android 10 or older, or no record kept)")
            } else {
                appendLine("exit records (newest first, ApplicationExitInfo):")
                data.exitRecords.take(6).forEach { record ->
                    appendLine(
                        "  " + at(record.at) + " age=" + gap(record.at) +
                            " reason=" + record.reasonName + "(" + record.reason + ")" +
                            " pss=" + record.pssKb + "kB rss=" + record.rssKb + "kB" +
                            " imp=" + record.importance +
                            (if (record.description.isNotEmpty()) " desc=" + record.description else "") +
                            (if (record.stateSummary.isNotEmpty()) " summary=" + record.stateSummary else ""),
                    )
                }
            }
            appendLine()
            appendLine("[POWER]")
            appendLine("battery: " + data.batteryPercent + "% charging=" + flag(data.charging))
            appendLine("screen on: " + flag(data.screenOn) + " doze=" + flag(data.dozeMode) +
                " power save=" + flag(data.powerSave))
            appendLine("battery optimisation exemption: " + flag(data.batteryUnrestricted))
            appendLine("app standby bucket: " + orUnknown(data.standbyBucket) +
                " background restricted: " + orUnknown(data.backgroundRestricted))
            appendLine()
            appendLine("[PERMISSIONS]")
            appendLine("activity recognition: " + flag(data.activityRecognition))
            appendLine("notifications: " + flag(data.notificationsAllowed))
            appendLine()
            appendLine("[RECORDER]")
            appendLine("events: " + data.eventLines + " lines, " + data.eventBytes + " bytes")
            appendLine("tail (" + data.eventTailLines.size + " newest lines):")
            data.eventTailLines.forEach { appendLine(it) }
            appendLine()
            appendLine("HINTS FOR THE DEVELOPER (not a diagnosis)")
            appendLine("- PROCESS END USER_REQUESTED with \"force stop\" in the description is a Force stop (Android 12+).")
            appendLine("- PROCESS END USER_STOPPED/FREEZER/SIGNALED while the screen was off points to an OS or OEM stop.")
            appendLine("- A fresh heartbeat but an old SENSOR line means the service is alive without sensor callbacks.")
            appendLine("- NO heartbeat, NO worker run and a large gap mean nothing in the app was scheduled.")
            appendLine("- Exemption=no on Android 12+ means a background foreground-service start is refused.")
            appendLine()
            appendLine("Sabeel keeps this report on the phone. It is read only when you share it.")
            appendLine("No location, contacts, notifications, app list or identifiers are included.")
            appendLine("هذا التقرير محفوظ على هاتفك ولا يُقرأ إلا عندما تشاركه. لا يحتوي على الموقع أو")
            appendLine("جهات الاتصال أو الإشعارات أو قائمة التطبيقات أو أي معرّفات شخصية.")
        }
    }
}
