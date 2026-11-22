package com.sabeel.steps.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sabeel.steps.service.StepCounterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Post-boot self-check (~1 minute after the boot attempt): did the step service actually come up?
 *
 * This worker deliberately never starts the foreground service: Android 12+ forbids starting an FGS
 * from a WorkManager background job (ForegroundServiceStartNotAllowedException). It only observes,
 * records the answer on the boot-attempt evidence and, when counting is meant to run but the service
 * is not alive after one bounded retry, posts the recovery notification whose tap opens the app — an
 * entry point where the FGS start is legal again.
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
            runCatching { StatusStore.markBootCheck(context, alive) }
            when {
                !enabled || !onboarded -> Result.success()
                alive -> Result.success()
                // One bounded re-check before telling the user anything (a slow cold start is normal).
                runAttemptCount < MAX_ATTEMPTS - 1 -> Result.retry()
                else -> {
                    runCatching { Notifications.notifyRecoveryNeeded(context) }
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
    }
}
