package com.sabeel.steps.service

import android.content.Context
import com.sabeel.steps.data.prefs.SettingsRepository
import com.sabeel.steps.platform.CountingStore
import com.sabeel.steps.platform.StatusStore
import com.sabeel.steps.platform.StepServiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one place that turns the user's counting intent into a service action.
 *
 * Rules this class exists to guarantee (council A1/A2):
 *  - **write before act**: the flag is persisted synchronously in [CountingStore] before any
 *    start/stop is requested, so a process death between tap and action can never resurrect a
 *    state the user rejected;
 *  - **serialized**: the service actions run one at a time on an internal coroutine mutex and each
 *    action re-reads the persisted flag at execution time, so rapid taps converge on the last tap
 *    instead of leaving a zombie service or notification behind;
 *  - **idempotent**: a start when already running is a no-op for the pipeline, a stop when already
 *    stopped removes nothing;
 *  - **destructive-safe**: while delete-all is erasing the database, toggles are ignored;
 *  - **never throws**: every entry point is wrapped; a refused foreground start is reported as a
 *    [StepCounterService.StartResult], never as an exception in a Compose callback.
 *
 * The service never self-enables. [onBoot] is a legal re-arm from BOOT_COMPLETED/MY_PACKAGE_REPLACED
 * and still refuses when the persisted intent is off.
 */
class CountingController(
    context: Context,
    private val counting: CountingStore,
    private val settings: SettingsRepository,
    private val status: StatusStore,
) {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val actionMutex = Mutex()

    /**
     * Persists [enabled] and reconciles the service with it. Persistence happens on the calling
     * thread (write-before-act); the service action is serialized off the main thread.
     */
    fun setEnabled(enabled: Boolean, reason: String) {
        try {
            if (isEraseInProgress()) return
            persist(enabled, reason)
            dispatchReconcile()
        } catch (ignored: Exception) {
            // A toggle must never crash the UI; the flow still shows the persisted intent.
        }
    }

    /** Flips the persisted intent (ring tap / Settings switch) and reconciles the service. */
    fun toggle() {
        try {
            if (isEraseInProgress()) return
            persist(!counting.current().enabled, CountingStore.REASON_USER)
            dispatchReconcile()
        } catch (ignored: Exception) {
            // Never throw from a Compose callback.
        }
    }

    /**
     * App-open reconciliation: starts counting when the intent is on, stops it when the intent is
     * off, and flushes the running service so a delayed sensor delivery is persisted now.
     */
    fun syncOnAppOpen() {
        try {
            if (isEraseInProgress()) return
            if (counting.current().enabled) {
                val result = StepCounterService.ensureStarted(appContext)
                if (result == StepCounterService.StartResult.STARTED &&
                    StepCounterService.runningInstance
                ) {
                    StepCounterService.requestFlush(appContext)
                }
            } else {
                StepCounterService.requestStop(appContext)
            }
            refreshOnboardedInBackground()
        } catch (ignored: Exception) {
            // App open is an entry point, not a failure surface.
        }
    }

    /**
     * Boot/package-replace re-arm. Returns the start outcome so [com.sabeel.steps.platform.BootReceiver]
     * can complete the boot-attempt record. An off intent is never overridden here.
     */
    fun onBoot(action: String): StepCounterService.StartResult = try {
        if (isEraseInProgress()) {
            StepCounterService.StartResult.REFUSED
        } else if (!counting.current().enabled) {
            runCatching { status.mark(appContext, StepServiceStatus.OFF_BY_USER) }
            StepCounterService.StartResult.DISABLED
        } else {
            StepCounterService.ensureStarted(appContext)
        }
    } catch (ignored: Exception) {
        StepCounterService.StartResult.REFUSED
    }

    /**
     * Onboarding completed: the intent becomes an explicit on (not just the default) and the service
     * is started. The pause marker, if any, is left for the service to consume as an explicit resume.
     */
    fun markOnboardingDone() {
        try {
            if (isEraseInProgress()) return
            counting.setOnboarded()
            persist(true, CountingStore.REASON_ONBOARDING)
            dispatchReconcile()
        } catch (ignored: Exception) {
            // Onboarding completion must not crash on a platform refusal.
        }
    }

    private fun persist(enabled: Boolean, reason: String) {
        counting.setEnabled(enabled, reason)
        if (enabled) {
            // pausedAt is deliberately NOT cleared here: the service needs it to run the explicit
            // StepPipeline.resume rebase (delta 0) instead of counting the paused gap.
        } else {
            counting.markPaused(reason)
        }
    }

    private fun dispatchReconcile() {
        scope.launch {
            try {
                actionMutex.withLock { reconcile() }
            } catch (ignored: Exception) {
                // The next entry point (app open, boot) retries; nothing to report from here.
            }
        }
    }

    /** Converges the service with the *currently persisted* flag (safe after rapid taps). */
    private fun reconcile() {
        if (isEraseInProgress()) return
        if (counting.current().enabled) {
            StepCounterService.ensureStarted(appContext)
        } else {
            StepCounterService.requestStop(appContext)
        }
    }

    /** Existing v1.2 installs have onboardingDone in DataStore but no CountingStore.onboarded yet. */
    private fun refreshOnboardedInBackground() {
        if (counting.current().onboarded) return
        scope.launch {
            runCatching {
                if (settings.current().onboardingDone) counting.setOnboarded()
            }
        }
    }

    private fun isEraseInProgress(): Boolean = StepCounterService.isEraseInProgress()
}
