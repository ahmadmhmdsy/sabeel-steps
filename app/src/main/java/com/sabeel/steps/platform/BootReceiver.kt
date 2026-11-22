package com.sabeel.steps.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.service.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Re-arms the platform layer after a reboot or an app update: the step foreground service and the
 * reminder/reconcile WorkManager chain.
 *
 * BOOT_COMPLETED and MY_PACKAGE_REPLACED are both background-foreground-service-start exemptions and
 * protected system broadcasts, so this receiver is exported=false: the system can still deliver them,
 * no other app can send them. The OEM-only QUICKBOOT_POWERON action was dropped: it is not an
 * Android-defined broadcast and has no foreground-service-start exemption, so the attempted start
 * was always refused on Android 12+.
 *
 * Every attempt is recorded (before the start) as [BootAttempt] evidence, so a device where the OEM
 * silently blocks the service can be diagnosed later from Diagnostics instead of guessed at. A short
 * bounded self-check retries once; the ~1 minute [BootHealthWorker] check completes the evidence and
 * posts the recovery nudge if the service still is not alive.
 *
 * The broadcast is always short: the work happens on a background coroutine with goAsync().
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleBoot(appContext, action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleBoot(appContext: Context, action: String) {
        val container = runCatching { (appContext as? SabeelApp)?.container }.getOrNull()
        val settings = runCatching { container?.settings?.current() }.getOrNull()
        val onboarded = settings?.onboardingDone == true

        // v1.2 upgrade: the old install has "onboarding done" in DataStore but no CountingStore row.
        val countingBefore = runCatching { CountingStore.get(appContext).current() }.getOrNull()
        if (onboarded && countingBefore?.onboarded == false) {
            runCatching { CountingStore.get(appContext).setOnboarded() }
        }
        val counting = runCatching { CountingStore.get(appContext).current() }.getOrNull()

        // Evidence before action: if the process is killed between the start request and its result,
        // the attempt still shows up (result PENDING / null aliveAfterCheck).
        val attempt = BootAttempt(
            at = System.currentTimeMillis(),
            action = action,
            enabled = counting?.enabled ?: true,
            onboarded = onboarded,
            hasActivityRecognition = runCatching { Permissions.hasActivityRecognition(appContext) }
                .getOrDefault(false),
            hasSensor = runCatching { StepSensors.hasAny(appContext) }.getOrDefault(false),
            healthConnectReady = runCatching { HealthConnect.isReady(appContext) }.getOrDefault(false),
            result = RESULT_PENDING,
        )
        runCatching { StatusStore.recordBootAttempt(appContext, attempt) }

        var exception: String? = null
        var result: String
        if (!onboarded) {
            result = RESULT_SKIPPED_NOT_ONBOARDED
        } else {
            val outcome = runCatching {
                container?.countingController?.onBoot(action)
                    ?: StepCounterService.ensureStarted(appContext)
            }.onFailure { exception = it.javaClass.simpleName }.getOrNull()
            result = outcome?.name ?: StepCounterService.StartResult.REFUSED.name
            // Short bounded self-check: a slow cold start is normal, an immediate death is not.
            if (outcome != null && outcome.name == StepCounterService.StartResult.STARTED.name) {
                delay(BOOT_SELF_CHECK_DELAY_MS)
                if (!StepCounterService.runningInstance) {
                    val retried = runCatching {
                        container?.countingController?.onBoot(action)
                            ?: StepCounterService.ensureStarted(appContext)
                    }.onFailure { exception = it.javaClass.simpleName }.getOrNull()
                    result = if (retried != null) retried.name + "+RETRY" else result
                }
            }
        }
        runCatching { StatusStore.updateLastBootAttempt(appContext, result, exception) }

        if (settings != null) {
            runCatching { Reminders.reschedule(appContext, settings) }
        }
        runCatching { Reminders.ensureReconcile(appContext) }
        runCatching { Reminders.ensureBootReconcile(appContext) }
        runCatching {
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                Reminders.WORK_BOOT_HEALTH,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<BootHealthWorker>()
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .addTag(Reminders.WORK_BOOT_HEALTH)
                    .build(),
            )
        }
    }

    private companion object {
        /** Time for the new process to bind the service before the one bounded retry. */
        const val BOOT_SELF_CHECK_DELAY_MS = 2_500L
        const val RESULT_PENDING = "PENDING"
        const val RESULT_SKIPPED_NOT_ONBOARDED = "SKIPPED_NOT_ONBOARDED"
    }
}
