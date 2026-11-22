package com.sabeel.steps.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user's persisted counting intent: the single authority every entry point reads before it
 * starts or stops the step service.
 *
 * Why a synchronous SharedPreferences file (and not the Settings DataStore): the boot receiver and
 * the service lifecycle paths cannot afford a cold DataStore read, and a missed read there is a
 * silent "counting never came back" failure. This file is tiny, read synchronously and written with
 * commit() *before* any service action, so the intent survives process death and a reboot.
 *
 * The service never self-enables and never infers intent from "onboarding done": if this flag is
 * false, no entry point may start counting, and a START_STICKY restart must stop itself.
 *
 * State is mirrored as a [StateFlow] for Compose. [current] is a volatile cached read, safe from the
 * main thread; the first [get] opens the preferences file like the rest of the platform layer does.
 */
data class CountingState(
    /** The user's intent: true means counting should run whenever Android lets it. */
    val enabled: Boolean = true,
    /** True once onboarding completed; kept for diagnostics and for the boot-attempt record. */
    val onboarded: Boolean = false,
    /** Wall-clock time of the last explicit on/off change (0 = never changed explicitly). */
    val changedAt: Long = 0L,
    /** Wall-clock time counting was deliberately paused (0 = no deliberate pause pending resume). */
    val pausedAt: Long = 0L,
    /** Why counting is paused (for diagnostics; see the REASON_* constants). */
    val pauseReason: String = "",
)

class CountingStore private constructor(sharedPreferences: SharedPreferences) {

    private val prefs: SharedPreferences = sharedPreferences

    private val mutableState = MutableStateFlow(read())

    /** Live mirror of [current] for Compose. Safe to collect on the main thread. */
    val state: StateFlow<CountingState> = mutableState.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /** Volatile cached read for hot paths (service gates, boot receiver). */
    fun current(): CountingState = mutableState.value

    /**
     * Persists the intent. Synchronous by design: the caller (or its action) must never run before
     * the flag is durably visible, or a process death could resurrect a state the user rejected.
     */
    fun setEnabled(enabled: Boolean, reason: String) {
        write { it.putBoolean(KEY_ENABLED, enabled).putLong(KEY_CHANGED_AT, System.currentTimeMillis()) }
    }

    /** Onboarding finished: the default-on intent becomes an explicit on intent. */
    fun setOnboarded() {
        write {
            it.putBoolean(KEY_ONBOARDED, true)
            if (!prefs.contains(KEY_ENABLED)) it.putBoolean(KEY_ENABLED, true)
        }
    }

    /**
     * Records that counting was deliberately paused. [pausedAt] > 0 is the signal the service uses
     * on the next start to run the explicit [com.sabeel.steps.domain.StepPipeline.resume] rebase
     * instead of the counter catch-up path.
     */
    fun markPaused(reason: String) {
        write {
            it.putLong(KEY_PAUSED_AT, System.currentTimeMillis())
                .putString(KEY_PAUSE_REASON, reason)
        }
    }

    /** Clears the pause marker once the service has actually resumed (baseline rebased). */
    fun clearPaused() {
        write { it.putLong(KEY_PAUSED_AT, 0L).putString(KEY_PAUSE_REASON, "") }
    }

    /** Reads the file fresh (the StateFlow is already the fast path; this is for strict reads). */
    private fun read(): CountingState {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val onboarded = prefs.getBoolean(KEY_ONBOARDED, false)
        val changedAt = prefs.getLong(KEY_CHANGED_AT, 0L)
        val pausedAt = prefs.getLong(KEY_PAUSED_AT, 0L)
        val pauseReason = prefs.getString(KEY_PAUSE_REASON, "") ?: ""
        return CountingState(enabled, onboarded, changedAt, pausedAt, pauseReason)
    }

    private fun refresh() {
        val next = read()
        if (next != mutableState.value) mutableState.value = next
    }

    private fun write(block: (SharedPreferences.Editor) -> Unit) {
        val editor = prefs.edit()
        block(editor)
        editor.commit()
        refresh()
    }

    companion object {
        /** Preferences file name. Kept apart from StatusStore on purpose: this is intent, not live state. */
        const val PREFS = "sabeel_counting"

        /** The user tapped the ring / the Settings switch. */
        const val REASON_USER = "user"

        /** Onboarding completion explicitly turned counting on. */
        const val REASON_ONBOARDING = "onboarding"

        /** The app came to the foreground and reconciled the intent with the service. */
        const val REASON_APP_OPEN = "app_open"

        /** A boot/package-replace entry point reconciled the intent. */
        const val REASON_BOOT = "boot"

        /** The system stopped the service while the intent was still on. */
        const val REASON_SYSTEM = "system"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_CHANGED_AT = "changed_at"
        private const val KEY_PAUSED_AT = "paused_at"
        private const val KEY_PAUSE_REASON = "pause_reason"

        @Volatile
        private var instance: CountingStore? = null

        /** Process-wide singleton; the application context owns the preferences file. */
        fun get(context: Context): CountingStore {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val prefs = context.applicationContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                return CountingStore(prefs).also { instance = it }
            }
        }
    }
}
