package com.sabeel.steps.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sabeel.steps.diagnostics.FlightRecorder
import com.sabeel.steps.diagnostics.MidnightBaseline
import com.sabeel.steps.service.StepCounterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Post-boot self-check (~1 minute after the boot attempt): did the step service actually come up?
 *
 * A background foreground-service start is legal only while the app holds the battery-optimisation
 * exemption. When it does, the final attempt starts the service itself; when it does not, the
 * worker only observes, records the answer on the boot-attempt evidence and posts the recovery
 * notification whose tap opens the app — an entry point where the FGS start is legal again.
 *
 * Every outcome is journaled for the support report.
 */
class BootHealthWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            val context = applicationContext
            val counting = runCatching { CountingStore.get(context).current() }.getOrNull()
            val enabled = counting?.enabled ?: true
            // Before onboarding there is nothing to recover; the app is not counting yet by design.
            val onboarded = counting?.onboarded ?: false
            val alive = StepCounterService.runningInstance
            val exempt = MidnightBaseline.isExempt(context)
            runCatching { StatusStore.markBootCheck(context, alive) }
            FlightRecorder.log(
                "boot_health",
                mapOf(
                    "enabled" to enabled,
                    "onboarded" to onboarded,
                    "alive" to alive,
                    "exempt" to exempt,
                    "attempt" to runAttemptCount,
                ),
            )
            when {
                !enabled || !onboarded -> Result.success()
                alive -> Result.success()
                // One bounded re-check before telling the user anything (a slow cold start is normal).
                runAttemptCount < MAX_ATTEMPTS - 1 -> Result.retry()
                exempt -> {
                    val start = StepCounterService.ensureStarted(context)
                    delay(SELF_HEAL_CHECK_MS)
                    val recovered = StepCounterService.runningInstance
                    FlightRecorder.recordWorker(
                        "boot_health_selfheal",
                        mapOf("start" to start.name, "recovered" to recovered),
                    )
                    if (!recovered) Notifications.notifyCountingPaused(context, urgent = true)
                    Result.success()
                }
                else -> {
                    FlightRecorder.recordWorker("boot_health_no_exemption")
                    runCatching { Notifications.notifyRecoveryNeeded(context) }
                    runCatching { Notifications.notifyCountingPaused(context, urgent = true) }
                    Result.success()
                }
            }
        } catch (ignored: Exception) {
            // A self-check must never fail the job in a way that retries forever.
            Result.success()
        }
    }

    private companion object {
        /** First check + one retry; WorkManager backs off ~30 s between attempts. */
        const val MAX_ATTEMPTS = 2

        /** Time for the freshly requested service to reach onCreate and set runningInstance. */
        const val SELF_HEAL_CHECK_MS = 2_500L
    }
}
