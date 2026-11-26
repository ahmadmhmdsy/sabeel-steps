package com.sabeel.steps.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTextTest {

    private fun data(
        exitRecords: List<ExitRecord> = emptyList(),
        tail: List<String> = emptyList(),
    ): ReportData = ReportData(
        generatedAt = 1_800_000_000_000L,
        appVersion = "1.3.1-field",
        appCode = 4,
        fieldBuild = true,
        deviceModel = "Pixel 6",
        deviceBrand = "google",
        androidRelease = "14",
        sdkInt = 34,
        processPid = 4242,
        processStartAt = 1_799_999_000_000L,
        uptimeMs = 1_000_000L,
        wallElapsedBase = 1_799_998_000_000L,
        rebootDetected = false,
        todaySteps = 1234,
        stepStateTotal = 1234,
        statusName = "STOPPED_BY_SYSTEM",
        serviceRunning = false,
        storageFaultAt = 0L,
        lastHeartbeatAt = 1_799_999_500_000L,
        lastSensorAt = 1_799_900_000_000L,
        lastWriteAt = 1_799_999_400_000L,
        lastWorkerAt = 1_799_990_000_000L,
        lastWorkerResult = "no_action",
        lastStartAt = 1_799_999_600_000L,
        lastStartResult = "STARTED",
        lastAlarmAt = 0L,
        lastAlarmResult = "",
        lastCrashAt = 0L,
        lastCrashType = "",
        lastProcessEndAt = 0L,
        lastProcessEndReason = "",
        bootAttempts = 2,
        batteryUnrestricted = false,
        batteryPercent = 61,
        charging = false,
        screenOn = false,
        dozeMode = true,
        powerSave = true,
        standbyBucket = 40,
        backgroundRestricted = true,
        notificationsAllowed = true,
        activityRecognition = true,
        exitRecords = exitRecords,
        eventLines = 87,
        eventBytes = 9_000L,
        eventTailLines = tail,
    )

    @Test
    fun reportHasEverySupportSection() {
        val text = DiagnosticsReportText.build(data())
        for (heading in listOf(
            "[PROCESS]",
            "[COUNTING]",
            "[HEARTBEAT]",
            "[SENSOR]",
            "[WORKER]",
            "[START]",
            "[MIDNIGHT ALARM]",
            "[CRASH]",
            "[PROCESS END]",
            "[POWER]",
            "[PERMISSIONS]",
            "[RECORDER]",
        )) {
            assertTrue("missing section " + heading, text.contains(heading))
        }
        assertTrue(text.contains("1.3.1-field"))
        assertTrue(text.contains("today rows/steps: 1234"))
        // The age must be measured from the process start (1000 s before generatedAt), not from
        // device uptime.
        assertTrue(text.contains("started: ") && text.contains("(age 16m)"))
    }

    @Test
    fun reportShowsExitReasonWhenAndroidProvidesOne() {
        val exit = ExitRecord(
            at = 1_799_999_000_000L,
            reason = 10,
            reasonName = "USER_STOPPED",
            pssKb = 10_000,
            rssKb = 20_000,
            importance = 100,
            description = "",
            stateSummary = "hb=123",
        )
        val text = DiagnosticsReportText.build(data(exitRecords = listOf(exit)))
        assertTrue(text.contains("USER_STOPPED"))
        assertTrue(text.contains("summary=hb=123"))
    }

    @Test
    fun reportCarriesNoInterpolatedNulls() {
        val text = DiagnosticsReportText.build(data())
        assertFalse(text.contains("null"))
    }
}
