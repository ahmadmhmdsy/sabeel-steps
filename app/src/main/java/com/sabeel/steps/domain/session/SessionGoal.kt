package com.sabeel.steps.domain.session

/**
 * What a custom session goal measures. Stored as its enum name in `walks.goalType` and
 * `session_presets.goalType`; never rename or reorder without a database migration.
 */
enum class GoalType {
    /** No goal: the session simply runs until the user stops it. */
    NONE,

    /** Reach a duration in seconds. */
    TIME,

    /** Reach a number of steps. */
    STEPS,

    /** Reach a distance in metres. */
    DISTANCE,

    /** Arrive within the radius of the session's end place. */
    PLACE;

    companion object {
        /** Tolerant storage parse: null, blank and unknown legacy values become [NONE]. */
        fun fromStorage(value: String?): GoalType =
            entries.firstOrNull { it.name == value?.trim() } ?: NONE
    }
}

/**
 * One goal evaluation. [current] and [target] are in the goal's own unit: seconds for [GoalType.TIME],
 * steps for [GoalType.STEPS], metres for [GoalType.DISTANCE], 0.0/1.0 for [GoalType.PLACE].
 *
 * [label] is a stable, non-localized descriptor ("TIME", "STEPS", "DISTANCE", "PLACE", "" for
 * [GoalType.NONE]); the UI localizes using [GoalType] and [target]. The domain layer intentionally
 * does no string formatting.
 */
data class GoalProgress(
    val current: Double,
    val target: Double,
    val reached: Boolean,
    val label: String,
)

/**
 * Pure goal evaluation for a custom session. No Android, no clocks, no I/O: the same inputs always
 * produce the same [GoalProgress], so the service can poll it every tick and the result is testable.
 *
 * "Reached exactly once" is a caller concern: this object never latches. The session service keeps a
 * latch (or compares [GoalProgress.reached] with its previous value) so the finish prompt, the saved
 * flag and any one-shot notification happen once. Targets that are not finite or not positive are
 * unreachable (except [GoalType.NONE], which is never reached by definition).
 */
object SessionGoal {
    /** Comparing metres/seconds: absorbs double rounding without making reaching easier. */
    private const val EPSILON = 1e-9

    /** Sentinel target for [GoalType.PLACE]: arrival is binary. */
    private const val PLACE_TARGET = 1.0

    /**
     * Evaluates the goal now.
     *
     * @param target configured goal value (seconds / steps / metres; ignored for PLACE and NONE).
     * @param elapsedSec wall time elapsed in the session, negative values are clamped to 0.
     * @param steps session steps, negative values are clamped to 0.
     * @param distanceMeters session distance in metres, negative or non-finite values are clamped to 0.
     * @param atPlace true when the device is currently inside the goal place's radius.
     */
    fun evaluate(
        type: GoalType,
        target: Double,
        elapsedSec: Long,
        steps: Int,
        distanceMeters: Double,
        atPlace: Boolean,
    ): GoalProgress {
        val safeTarget = if (target.isFinite()) target else 0.0
        return when (type) {
            GoalType.NONE -> GoalProgress(current = 0.0, target = 0.0, reached = false, label = "")
            GoalType.TIME -> measured(type, elapsedSec.coerceAtLeast(0).toDouble(), safeTarget)
            GoalType.STEPS -> measured(type, steps.coerceAtLeast(0).toDouble(), safeTarget)
            GoalType.DISTANCE -> measured(
                type = type,
                current = if (distanceMeters.isFinite()) distanceMeters.coerceAtLeast(0.0) else 0.0,
                target = safeTarget,
            )
            GoalType.PLACE -> GoalProgress(
                current = if (atPlace) PLACE_TARGET else 0.0,
                target = PLACE_TARGET,
                reached = atPlace,
                label = GoalType.PLACE.name,
            )
        }
    }

    /**
     * True when [target] can make [type] reachable: PLACE and NONE ignore the number, every measured
     * goal needs a finite value greater than zero. Used by the setup screen before it saves a preset.
     */
    fun isValidTarget(type: GoalType, target: Double): Boolean = when (type) {
        GoalType.NONE, GoalType.PLACE -> true
        else -> target.isFinite() && target > 0.0
    }

    private fun measured(type: GoalType, current: Double, target: Double): GoalProgress {
        val safeTarget = if (target.isFinite()) target.coerceAtLeast(0.0) else 0.0
        val reached = safeTarget > 0.0 && current >= safeTarget - EPSILON
        return GoalProgress(current = current, target = safeTarget, reached = reached, label = type.name)
    }
}
