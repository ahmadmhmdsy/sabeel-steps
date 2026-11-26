package com.sabeel.steps.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.repo.StepSnapshot
import com.sabeel.steps.diagnostics.FlightRecorder
import com.sabeel.steps.diagnostics.MidnightBaseline
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.UserSettings
import com.sabeel.steps.service.StepCounterService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * WorkManager scheduling for the daily reminder and for the 15-minute reconcile pass.
 * WorkManager is the only scheduler used: no exact alarms, no wakelocks.
 */
object Reminders {

    const val WORK_REMINDER = "sabeel_reminder"
    const val WORK_RECONCILE = "sabeel_reconcile"
    const val WORK_BOOT_RECONCILE = "sabeel_boot_reconcile"
    const val WORK_BOOT_HEALTH = "sabeel_boot_health"

    /** (Re)schedules the one-shot daily reminder; cancels the previous chain first. */
    fun reschedule(context: Context, settings: UserSettings) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_REMINDER)
        if (!settings.reminderEnabled) return
        val delayMs = delayUntilNext(settings.reminderMinutes, System.currentTimeMillis(), ZoneId.systemDefault())
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(WORK_REMINDER)
            .build()
        workManager.enqueueUniqueWork(WORK_REMINDER, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_REMINDER)
    }

    /** Periodic 15-minute reconcile: midnight rollover and goal catch-up while the service is off. */
    fun ensureReconcile(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_RECONCILE,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReconcileWorker>(15, TimeUnit.MINUTES)
                .addTag(WORK_RECONCILE)
                .build(),
        )
    }

    /** One-shot reconcile shortly after boot/app update, in case the foreground service could not start. */
    fun ensureBootReconcile(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_BOOT_RECONCILE,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReconcileWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag(WORK_BOOT_RECONCILE)
                .build(),
        )
    }

    /**
     * Milliseconds from [now] until the next local occurrence of [minutesOfDay].
     * Pure and deterministic for the same zone/now pair (unit-testable by construction).
     */
    fun delayUntilNext(minutesOfDay: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val safeMinutes = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        val target = LocalTime.of(safeMinutes / 60, safeMinutes % 60)
        val current = Instant.ofEpochMilli(now).atZone(zone)
        var next = current.with(target)
        if (!next.isAfter(current)) next = next.plusDays(1)
        return (next.toInstant().toEpochMilli() - now).coerceAtLeast(1_000L)
    }
}

/**
 * One-shot reminder worker; posts the notification (permission-gated and localized inside
 * [Notifications]) and re-arms tomorrow's occurrence.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = runCatching {
            (context as? SabeelApp)?.container?.settings?.current()
        }.getOrNull() ?: return Result.retry()

        if (settings.reminderEnabled) {
            runCatching { Notifications.notifyReminder(context) }
        }
        // Re-arm the next occurrence; REPLACE supersedes the worker that is finishing now.
        runCatching { Reminders.reschedule(context, settings) }
        return Result.success()
    }
}

/**
 * Periodic reconcile worker. Keeps today's row truthful after process death or midnight while the
 * step service is not running and catches up the goal notification.
 *
 * It starts [com.sabeel.steps.service.StepCounterService] only when Android has exempted Sabeel from
 * battery optimisation: that exemption is the documented background foreground-service-start
 * exemption. Without it the service start would be refused (ForegroundServiceStartNotAllowedException)
 * and the worker posts the visible "counting stopped" notification instead. Either outcome is
 * journaled, so a report shows whether a worker self-heal was possible and whether it worked.
 */
class ReconcileWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private companion object {
        /** Time for the started service to reach onCreate and set runningInstance. */
        const val SELF_HEAL_CHECK_MS = 2_500L
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val app = context as? SabeelApp ?: return Result.success()
        val container = app.container
        val settings = runCatching { container.settings.current() }.getOrElse { return Result.retry() }

        // 0. Counting off or paused: the frozen day must not be resurrected by this background pass
        //    (no reconcile HC floor, no paused-gap backfill). Goal catch-up below is still harmless.
        val counting = runCatching { CountingStore.get(context).current() }.getOrNull()
        val countingSuppressed = counting != null &&
            (!counting.enabled || counting.pausedAt > 0L || !counting.onboarded)

        // 1. Midnight rollover and process-death restore: never double count.
        runCatching { container.steps.reconcile() }

        // 2. Health Connect floor while the service cannot run. A background read needs the
        //    provider's background-read grant; when it is missing the read simply fails and the
        //    already-stored day is left untouched (no invented total). Skipped while counting is
        //    deliberately off/paused: the provider total still includes the paused gap.
        var todayRow = runCatching { container.steps.todayFlow().first() }.getOrNull()
        if (!countingSuppressed && HealthConnect.isReady(context)) {
            val read = runCatching { HealthConnect.readToday(context) }.getOrNull()
            if (read != null && read.steps > 0) {
                todayRow = runCatching {
                    container.steps.record(StepSnapshot(hcTotal = read.steps))
                }.getOrNull() ?: todayRow
            }
        }

        // 3. Goal catch-up (the flag guarantees at most one notification per local day, and the
        //    post itself is permission-gated).
        runCatching {
            val row = todayRow
            if (row != null && row.date == DateKey.today()) {
                Notifications.notifyGoalReached(
                    context,
                    row.steps,
                    settings.goalSteps,
                    estimated = row.estimatedSteps > 0,
                )
            }
        }

        // 4. Self-heal when the platform allows it, otherwise tell the user visibly.
        //    A background foreground-service start is legal only while the app holds the
        //    battery-optimisation exemption; when it does not, the start is pointless and the
        //    notification is the honest path. Both branches are journaled for the report.
        runCatching {
            val canCount = Permissions.hasActivityRecognition(context) &&
                (StepSensors.hasAny(context) || HealthConnect.isReady(context))
            val status = StatusStore.read(context)
            if (!countingSuppressed &&
                canCount &&
                !StepCounterService.runningInstance &&
                (status == StepServiceStatus.STOPPED || status == StepServiceStatus.STOPPED_BY_SYSTEM)
            ) {
                val exempt = MidnightBaseline.isExempt(context)
                FlightRecorder.log(
                    "worker_selfheal",
                    mapOf("exempt" to exempt, "status" to status.name, "suppressed" to countingSuppressed),
                )
                if (exempt) {
                    val start = StepCounterService.ensureStarted(context)
                    FlightRecorder.recordWorker(
                        "selfheal:" + start.name,
                        mapOf("exempt" to true, "running_after_request" to StepCounterService.runningInstance),
                    )
                    // onCreate is posted to the main looper; a short wait distinguishes "accepted"
                    // from "accepted then stood down" (permission race, sensor gone).
                    delay(SELF_HEAL_CHECK_MS)
                    if (!StepCounterService.runningInstance) {
                        FlightRecorder.recordWorker("selfheal_refused:" + start.name)
                        Notifications.notifyCountingPaused(context, urgent = true)
                    }
                } else {
                    FlightRecorder.recordWorker("paused_no_exemption")
                    Notifications.notifyCountingPaused(context, urgent = true)
                }
            } else {
                FlightRecorder.recordWorker("no_action")
            }
        }
        return Result.success()
    }
}