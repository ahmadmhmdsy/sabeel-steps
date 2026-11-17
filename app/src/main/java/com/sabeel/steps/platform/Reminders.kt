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
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.UserSettings
import com.sabeel.steps.service.StepCounterService
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

    /** Periodic 15-minute reconcile: midnight rollover, goal catch-up, service keep-alive. */
    fun ensureReconcile(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_RECONCILE,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReconcileWorker>(15, TimeUnit.MINUTES)
                .addTag(WORK_RECONCILE)
                .build(),
        )
    }

    /** One-shot reconcile shortly after boot, in case the foreground service could not start. */
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

/** One-shot reminder worker; posts the notification and re-arms tomorrow's occurrence. */
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
            Notifications.notifyReminder(context)
        }
        // Re-arm the next occurrence; REPLACE supersedes the worker that is finishing now.
        runCatching { Reminders.reschedule(context, settings) }
        return Result.success()
    }
}

/**
 * Periodic reconcile worker. Keeps today's row truthful after process death or midnight while
 * the step service is not running, catches up the goal notification, and tries (best effort) to
 * restart the foreground service. Android 12+ may refuse a background FGS start; that is caught
 * and never crashes the app.
 */
class ReconcileWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val app = context as? SabeelApp ?: return Result.success()
        val container = app.container
        val settings = runCatching { container.settings.current() }.getOrElse { return Result.retry() }

        // 1. Midnight rollover and process-death restore: never double count.
        runCatching { container.steps.reconcile() }

        // 2. Goal catch-up (the flag guarantees at most one notification per local day).
        runCatching {
            val todayRow = container.database.dailyStepsDao().getAll().firstOrNull { it.date == DateKey.today() }
            if (todayRow != null) {
                Notifications.notifyGoalReached(context, todayRow.steps, settings.goalSteps)
            }
        }

        // 3. Best effort: make sure counting is running (ignored when the OS forbids a background start).
        if (settings.onboardingDone) {
            runCatching { StepCounterService.start(context) }
        }
        return Result.success()
    }
}
