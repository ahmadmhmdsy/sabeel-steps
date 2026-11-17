package com.sabeel.steps.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.service.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-arms the platform layer after a reboot: the step foreground service (best effort; Android 15+
 * may refuse FGS starts from boot) and the reminder/reconcile WorkManager chain.
 * The broadcast is always short: the work happens on a background coroutine with goAsync().
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT_POWERON) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = runCatching {
                    (appContext as? SabeelApp)?.container?.settings?.current()
                }.getOrNull()
                if (settings?.onboardingDone == true) {
                    // Non-fatal: the OS may restrict foreground-service starts from boot.
                    runCatching { StepCounterService.start(appContext) }
                }
                if (settings != null) {
                    runCatching { Reminders.reschedule(appContext, settings) }
                }
                runCatching { Reminders.ensureReconcile(appContext) }
                runCatching { Reminders.ensureBootReconcile(appContext) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
