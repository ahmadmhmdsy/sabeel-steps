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
 * Re-arms the platform layer after a reboot or an app update: the step foreground service and the
 * reminder/reconcile WorkManager chain.
 *
 * BOOT_COMPLETED and MY_PACKAGE_REPLACED are both background-foreground-service-start exemptions and
 * protected system broadcasts, so this receiver is exported=false: the system can still deliver them,
 * no other app can send them. The OEM-only QUICKBOOT_POWERON action was dropped: it is not an
 * Android-defined broadcast and has no foreground-service-start exemption, so the attempted start
 * was always refused on Android 12+.
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
                val settings = runCatching {
                    (appContext as? SabeelApp)?.container?.settings?.current()
                }.getOrNull()
                if (settings?.onboardingDone == true) {
                    // Legal from these two broadcasts; ensureStarted still gates on the health
                    // prerequisite and never throws, so a skipped permission cannot crash boot.
                    runCatching { StepCounterService.ensureStarted(appContext) }
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
}
