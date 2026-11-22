package com.sabeel.steps.service

import android.content.Intent
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType

/**
 * Immutable description of one tracked custom session (activity, goal, GPS, places, preset).
 *
 * The object is deliberately Android-light: it is built by the session setup screen, serialized
 * into the [WalkTrackingService.ACTION_START] Intent and restored from the walk checkpoint, so the
 * service never depends on a ViewModel or on DataStore to know what the user asked to track.
 *
 * Defaults describe the pre-v1.3 plain walk: WALK / NONE / 0.0 / GPS on / no places / no preset /
 * no auto-stop. GPS is on by default because the legacy "start a walk" entry point always tried to
 * record the route when the permission existed; the setup screen passes its own value explicitly.
 *
 * Place and preset ids are database ids (positive) or null. The extras and the checkpoint store
 * them as [NO_ID] because a Bundle/JSON null round trip for a Long is easy to get wrong.
 */
data class SessionConfig(
    val activityType: ActivityType = ActivityType.WALK,
    val goalType: GoalType = GoalType.NONE,
    /** Goal value in the goal own unit: seconds (TIME), steps (STEPS), metres (DISTANCE). */
    val goalValue: Double = 0.0,
    val useGps: Boolean = true,
    val startPlaceId: Long? = null,
    val endPlaceId: Long? = null,
    val presetId: Long? = null,
    /** Stop and save automatically once the goal is reached (numeric goals and place arrival). */
    val autoStop: Boolean = false,
) {
    companion object {
        /** Sentinel stored in an Intent/JSON id slot for "no place/preset". Database ids are >= 1. */
        const val NO_ID = -1L

        private const val EXTRA_PREFIX = "com.sabeel.steps.extra.session."
        const val EXTRA_ACTIVITY = EXTRA_PREFIX + "activity"
        const val EXTRA_GOAL = EXTRA_PREFIX + "goal"
        const val EXTRA_GOAL_VALUE = EXTRA_PREFIX + "goal_value"
        const val EXTRA_USE_GPS = EXTRA_PREFIX + "use_gps"
        const val EXTRA_START_PLACE = EXTRA_PREFIX + "start_place"
        const val EXTRA_END_PLACE = EXTRA_PREFIX + "end_place"
        const val EXTRA_PRESET = EXTRA_PREFIX + "preset"
        const val EXTRA_AUTO_STOP = EXTRA_PREFIX + "auto_stop"

        /** Writes every field of [config] into [intent] under namespaced extras. */
        fun write(intent: Intent, config: SessionConfig) {
            intent.putExtra(EXTRA_ACTIVITY, config.activityType.name)
            intent.putExtra(EXTRA_GOAL, config.goalType.name)
            intent.putExtra(EXTRA_GOAL_VALUE, safeGoalValue(config.goalValue))
            intent.putExtra(EXTRA_USE_GPS, config.useGps)
            intent.putExtra(EXTRA_START_PLACE, config.startPlaceId ?: NO_ID)
            intent.putExtra(EXTRA_END_PLACE, config.endPlaceId ?: NO_ID)
            intent.putExtra(EXTRA_PRESET, config.presetId ?: NO_ID)
            intent.putExtra(EXTRA_AUTO_STOP, config.autoStop)
        }

        /**
         * Reads a config back from [intent]; a null/unrelated Intent yields the documented defaults.
         * Unknown enum names fall back through the tolerant storage parsers (WALK / NONE), so an
         * older Intent can never crash the service.
         */
        fun read(intent: Intent?): SessionConfig {
            if (intent == null) return SessionConfig()
            return SessionConfig(
                activityType = ActivityType.fromStorage(intent.getStringExtra(EXTRA_ACTIVITY)),
                goalType = GoalType.fromStorage(intent.getStringExtra(EXTRA_GOAL)),
                goalValue = safeGoalValue(intent.getDoubleExtra(EXTRA_GOAL_VALUE, 0.0)),
                useGps = intent.getBooleanExtra(EXTRA_USE_GPS, SessionConfig().useGps),
                startPlaceId = readId(intent, EXTRA_START_PLACE),
                endPlaceId = readId(intent, EXTRA_END_PLACE),
                presetId = readId(intent, EXTRA_PRESET),
                autoStop = intent.getBooleanExtra(EXTRA_AUTO_STOP, false),
            )
        }

        private fun readId(intent: Intent, key: String): Long? {
            val id = intent.getLongExtra(key, NO_ID)
            return if (id > 0L) id else null
        }

        private fun safeGoalValue(value: Double): Double = if (value.isFinite()) value else 0.0
    }
}
