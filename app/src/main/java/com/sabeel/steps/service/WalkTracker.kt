package com.sabeel.steps.service

import android.content.Context
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.domain.session.WalkLeg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Process-local live walk state, written by WalkTrackingService and observed by the Walk screen.
 * Contract: the service is the only writer; the UI only collects [state].
 *
 * This object also owns the on-disk checkpoint file. The service is the only writer of its content,
 * but the app shell, the Walk screen and the History/delete-all paths need to ask whether an
 * interrupted walk can be resumed (or drop it) without knowing the file format. The checkpoint is
 * private app storage only; it never leaves the device.
 */
object WalkTracker {
    private const val CHECKPOINT_FILE = "walk_checkpoint.json"
    private const val CHECKPOINT_TMP_SUFFIX = ".tmp"

    data class Live(
        val running: Boolean = false,
        val startedAt: Long = 0L,
        val elapsedSec: Int = 0,
        val distanceMeters: Double = 0.0,
        val steps: Int = 0,
        val estimated: Boolean = false,
        val secondsPerKm: Int = 0,
        val route: List<RoutePoint> = emptyList(),
        val gpsActive: Boolean = false,
        val saving: Boolean = false,
        val savedWalkId: Long? = null,
        val error: String? = null,
        // Custom-session state (v1.3). Defaults describe "no session", so every pre-session UI call
        // site that only reads the walk fields above keeps compiling and rendering unchanged.
        val activityType: ActivityType = ActivityType.WALK,
        val goalType: GoalType = GoalType.NONE,
        /** Goal value in the goal own unit: seconds / steps / metres; ignored for NONE and PLACE. */
        val goalValue: Double = 0.0,
        /** Live goal progress in the same unit as [goalValue]; 0.0/1.0 for PLACE arrival. */
        val goalCurrent: Double = 0.0,
        /** Sticky latch: true once the goal has been reached in this session. */
        val goalReached: Boolean = false,
        val paused: Boolean = false,
        /** Wall clock of the pause moment (0 while not paused); elapsed time is frozen from it. */
        val pausedAt: Long = 0L,
        /** True when the global step count is switched off, so the session cannot advance. */
        val countingOff: Boolean = false,
        /** ISO local date the live projection currently belongs to ("" before the first tick). */
        val sessionDate: String = "",
        /** Calendar days already closed by midnight rollover, oldest first. */
        val legs: List<WalkLeg> = emptyList(),
        val startPlaceId: Long? = null,
        val endPlaceId: Long? = null,
        val presetId: Long? = null,
        val autoStop: Boolean = false,
    )

    private val _state = MutableStateFlow(Live())
    val state: StateFlow<Live> = _state.asStateFlow()

    fun update(transform: (Live) -> Live) {
        _state.update(transform)
    }

    fun reset() {
        _state.value = Live()
    }

    /**
     * Set by [resetSession] (delete-all) so the walk service's teardown flush cannot rewrite the
     * checkpoint after the wipe. Cleared by [allowCheckpoints] when a new walk begins.
     */
    @Volatile private var checkpointsSuppressed = false

    /** The checkpoint file the walk service writes (same private files dir for service and app). */
    fun checkpointFile(context: Context): File =
        File(context.applicationContext.filesDir, CHECKPOINT_FILE)

    /** True when an interrupted walk exists and can be offered for resume. */
    fun hasCheckpoint(context: Context): Boolean = checkpointFile(context).exists()

    /** Removes the checkpoint (and its in-progress temp file). Safe when none exists. */
    fun deleteCheckpoint(context: Context) {
        val target = checkpointFile(context)
        runCatching { target.delete() }
        runCatching { File(target.parentFile, target.name + CHECKPOINT_TMP_SUFFIX).delete() }
    }

    /**
     * Drops an interrupted walk completely: checkpoint file plus live process state. Used by
     * delete-all, which also must not let a stopping service flush the checkpoint back to disk.
     */
    fun resetSession(context: Context) {
        checkpointsSuppressed = true
        deleteCheckpoint(context)
        reset()
    }

    /** Re-enables checkpointing; called when a new walk (fresh or resumed) begins. */
    fun allowCheckpoints() {
        checkpointsSuppressed = false
    }

    /** True while delete-all owns the session: the service must not write a new checkpoint. */
    fun isCheckpointSuppressed(): Boolean = checkpointsSuppressed

}
