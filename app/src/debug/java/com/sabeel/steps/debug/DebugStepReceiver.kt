package com.sabeel.steps.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.repo.StepSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only step injector for emulator and QA instrumentation.
 *
 * The emulator has no hardware step counter, so the real pipeline cannot be exercised end to end.
 * This receiver feeds the SAME [StepSnapshot] path the foreground service uses, which means Home,
 * the daily row, Health Connect merging and the session projection all behave exactly as they would
 * for a real sensor. It lives only in the debug source set and is absent from release builds.
 *
 * Usage:
 *   adb shell am broadcast -a com.sabeel.steps.DEBUG_STEPS --ei steps 100 \
 *     -n com.sabeel.steps/.debug.DebugStepReceiver
 */
class DebugStepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val count = intent.getIntExtra(EXTRA_STEPS, 0).coerceIn(0, MAX_STEPS_PER_BROADCAST)
        if (count <= 0) return
        val application = context.applicationContext as? SabeelApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                application.container.steps.record(
                    StepSnapshot(detectorSteps = count),
                    activeMinutesToday = -1,
                )
            } catch (ignored: Exception) {
                // Debug tooling must never crash the debug build.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val EXTRA_STEPS = "steps"
        const val MAX_STEPS_PER_BROADCAST = 50_000
    }
}
