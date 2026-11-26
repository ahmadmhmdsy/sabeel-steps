package com.sabeel.steps.diagnostics

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import com.sabeel.steps.platform.CountingStore
import com.sabeel.steps.service.StepCounterService
import java.time.Instant
import java.time.ZoneId

/**
 * One inexact wake-up a few minutes after local midnight.
 *
 * It is the missing "day baseline" restart path: when the process is asleep at midnight, no sample
 * is processed, so the first sample tomorrow cannot be split into "yesterday" and "today" and is
 * dropped by design. Waking briefly right after midnight lets the service record the day's real
 * baseline, so the morning delta is attributable again.
 *
 * setAndAllowWhileIdle is exact-free and needs no SCHEDULE_EXACT_ALARM. Delivery is not guaranteed
 * on OEM-killed apps; when it did not run the old drop rule stays in force, so no step is invented.
 */
object MidnightBaseline {

    const val ACTION = "com.sabeel.steps.action.MIDNIGHT_BASELINE"
    private const val REQUEST_CODE = 7201
    private const val HOUR = 0
    private const val MINUTE = 5
    private const val LOG_LABEL = "midnight"

    /** Next 00:05 in the device zone, strictly after [now]. Pure so it can be unit-tested. */
    fun nextTriggerAt(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val local = Instant.ofEpochMilli(now).atZone(zone)
        var target = local.toLocalDate().atTime(HOUR, MINUTE).atZone(zone)
        if (target.toInstant().toEpochMilli() <= now) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    fun ensureScheduled(context: Context) {
        runCatching {
            val app = context.applicationContext
            val manager = app.getSystemService(AlarmManager::class.java) ?: return
            val at = nextTriggerAt(System.currentTimeMillis())
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(app))
        }
    }

    fun cancel(context: Context) {
        runCatching {
            val app = context.applicationContext
            val manager = app.getSystemService(AlarmManager::class.java) ?: return
            manager.cancel(pendingIntent(app))
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MidnightBaselineReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun isExempt(context: Context): Boolean = runCatching {
        val power = context.applicationContext.getSystemService(PowerManager::class.java)
        power?.isIgnoringBatteryOptimizations(context.applicationContext.packageName) == true
    }.getOrDefault(false)
}

/**
 * Runs at ~00:05: records a canary, and if counting is wanted and Android has exempted Sabeel from
 * battery optimisation, starts the counting service so today gets a baseline. The exemption is the
 * documented foreground-service-start exemption; without it the receiver does not attempt a start.
 */
class MidnightBaselineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        runCatching {
            val app = context.applicationContext
            val counting = runCatching { CountingStore.get(app).current() }.getOrNull()
            val exempt = MidnightBaseline.isExempt(app)
            val wanted = counting?.enabled == true && counting.onboarded
            FlightRecorder.logSync(
                "midnight_alarm",
                mapOf(
                    "wanted" to wanted,
                    "exempt" to exempt,
                    "running" to StepCounterService.runningInstance,
                    "wall" to System.currentTimeMillis(),
                    "elapsed" to SystemClock.elapsedRealtime(),
                ),
            )
            if (!wanted) {
                FlightRecorder.recordAlarm("skipped_off")
            } else if (!exempt) {
                FlightRecorder.recordAlarm("skipped_not_exempt")
            } else if (StepCounterService.runningInstance) {
                FlightRecorder.recordAlarm("already_running")
            } else {
                val result = StepCounterService.ensureStarted(app)
                FlightRecorder.recordAlarm(result.name, mapOf("exempt" to true))
            }
        }
        runCatching { MidnightBaseline.ensureScheduled(context.applicationContext) }
    }
}
