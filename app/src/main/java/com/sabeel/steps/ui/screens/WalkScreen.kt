@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.service.WalkTracker
import com.sabeel.steps.ui.components.CountingStatusCard
import com.sabeel.steps.ui.components.EmptyState
import com.sabeel.steps.ui.components.RouteSketch
import com.sabeel.steps.ui.components.SabeelPrimaryButton
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader
import com.sabeel.steps.ui.components.StatCard
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.paceText
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.viewmodel.WalkViewModel

/**
 * The live custom-session screen (plan Phase 2): activity and goal, progress to the goal, elapsed
 * time, session-only steps, distance and route, pause/resume, and finish/discard with confirmation.
 *
 * A session is never silently harnessed to a second counter: steps shown here are the session slice
 * of the global day count, and the warning card appears when counting was switched off mid-session.
 * The screen stays honest about location: if it is not recording, the route area says so and offers
 * the permission instead of drawing a fake line.
 *
 * @param onBack leaves the screen (the caller decides where to).
 * @param onDone leaves after the summary was dismissed.
 * @param onStartSetup opens the setup screen when no session is running; defaults to [onDone] so the
 * old walk route keeps compiling until the NavHost points it at the session flow.
 */
@Composable
fun WalkScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onStartSetup: () -> Unit = onDone,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: WalkViewModel = viewModel(factory = WalkViewModel.factory(app))
    val live by viewModel.live.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val walkUi by viewModel.state.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()

    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    val showSummary = live.savedWalkId != null && !live.running
    val placeLabel = places.firstOrNull { it.id == live.endPlaceId }?.label

    ScreenScaffold(
        title = stringResource(R.string.session_live_title),
        onBack = {
            if (live.running) {
                showExitDialog = true
            } else {
                onBack()
            }
        },
    ) {
        when {
            showSummary -> SessionSummary(
                live = live,
                units = settings.units,
                placeLabel = placeLabel,
                onDone = {
                    viewModel.clearSummary()
                    onDone()
                },
                onStartAnother = {
                    viewModel.clearSummary()
                    onStartSetup()
                },
            )

            live.running -> SessionLive(
                live = live,
                units = settings.units,
                placeLabel = placeLabel,
                onPause = { viewModel.pauseSession() },
                onResume = { viewModel.resumeSession() },
                onFinish = { viewModel.stopSession() },
                onDiscardRequest = { showDiscardDialog = true },
                onTurnOnCounting = { viewModel.turnOnCounting() },
                onOpenPermission = {
                    locationLauncher.launch(Permissions.locationPermissions().toTypedArray())
                },
            )

            else -> NoSession(
                pendingCheckpoint = walkUi.pendingCheckpoint,
                onResumePending = { viewModel.resumePending() },
                onDiscardPending = { viewModel.discardPending() },
                onStartSetup = onStartSetup,
            )
        }
        if (walkUi.startBlocked && !live.running && !showSummary) {
            Text(
                text = stringResource(R.string.session_start_blocked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (live.error != null && !live.running && !showSummary) {
            Text(
                text = stringResource(R.string.walk_error_generic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (live.saving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.walk_saving),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.session_exit_title)) },
            text = { Text(stringResource(R.string.session_exit_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.stopSession()
                    },
                ) {
                    Text(stringResource(R.string.session_finish))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            showDiscardDialog = true
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.session_discard),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.session_discard_confirm_title)) },
            text = { Text(stringResource(R.string.session_discard_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardSession()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.session_discard),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SessionLive(
    live: WalkTracker.Live,
    units: Units,
    placeLabel: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscardRequest: () -> Unit,
    onTurnOnCounting: () -> Unit,
    onOpenPermission: () -> Unit,
) {
    val fontScale = LocalConfiguration.current.fontScale
    val timerStyle = if (fontScale > 1.5f) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.displaySmall
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoChip(text = sessionActivityLabel(live.activityType))
        InfoChip(text = liveGoalChipText(live, units, placeLabel))
    }

    if (live.paused) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.session_paused_label),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.session_paused_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }

    if (live.countingOff) {
        CountingStatusCard(
            title = stringResource(R.string.session_counting_off_title),
            body = stringResource(R.string.session_counting_off_live_body),
            actionLabel = stringResource(R.string.session_turn_on_counting),
            onAction = onTurnOnCounting,
        )
    }

    if (live.goalType != GoalType.NONE) {
        GoalProgressBlock(live = live, units = units, placeLabel = placeLabel)
    }

    if (live.goalReached && live.goalType != GoalType.NONE) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.session_goal_met),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (live.autoStop) {
                            stringResource(R.string.session_goal_met_autostop)
                        } else {
                            stringResource(R.string.session_goal_met_body)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.walk_live_time),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = StepMath.formatDuration(live.elapsedSec.toLong()),
            style = timerStyle,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    val stepsValue = if (live.estimated) {
        stringResource(R.string.estimated_steps_value, stepsText(live.steps))
    } else {
        stepsText(live.steps)
    }
    if (fontScale > 1.3f) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = stepsValue,
                label = stringResource(R.string.walk_live_steps),
                modifier = Modifier.fillMaxWidth(),
            )
            StatCard(
                value = distanceText(live.distanceMeters, units),
                label = stringResource(R.string.walk_live_distance),
                modifier = Modifier.fillMaxWidth(),
            )
            StatCard(
                value = paceText(live.secondsPerKm, units),
                label = stringResource(R.string.walk_live_pace),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = stepsValue,
                label = stringResource(R.string.walk_live_steps),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = distanceText(live.distanceMeters, units),
                label = stringResource(R.string.walk_live_distance),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = paceText(live.secondsPerKm, units),
                label = stringResource(R.string.walk_live_pace),
                modifier = Modifier.weight(1f),
            )
        }
    }
    Text(
        text = stringResource(R.string.session_steps_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (live.estimated) {
        Text(
            text = stringResource(R.string.walk_estimated_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text(
        text = stringResource(
            if (live.gpsActive) R.string.walk_gps_on else R.string.session_gps_inactive,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // While paused, location is intentionally not recording: offering the permission action there
    // would suggest a grant is missing even when the user already allowed it.
    if (!live.gpsActive && !live.paused) {
        TextButton(
            onClick = onOpenPermission,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.permission_grant))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (live.route.size < 2) {
                Text(
                    text = stringResource(R.string.walk_route_waiting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                RouteSketch(
                    route = live.route,
                    contentDescription = stringResource(R.string.walk_cd_route, live.route.size),
                    sketchHeight = 180.dp,
                )
            }
        }
    }

    if (live.paused) {
        SabeelPrimaryButton(
            text = stringResource(R.string.session_resume),
            onClick = onResume,
            enabled = !live.saving,
        )
    } else {
        SabeelSecondaryButton(
            text = stringResource(R.string.session_pause),
            onClick = onPause,
            enabled = !live.saving,
        )
    }
    TextButton(
        onClick = onDiscardRequest,
        enabled = !live.saving,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(R.string.session_discard),
            color = MaterialTheme.colorScheme.error,
        )
    }
    SabeelPrimaryButton(
        text = stringResource(R.string.session_finish),
        onClick = onFinish,
        enabled = !live.saving,
    )
}

@Composable
private fun GoalProgressBlock(
    live: WalkTracker.Live,
    units: Units,
    placeLabel: String?,
) {
    val targetText = sessionGoalTargetText(
        goalType = live.goalType,
        goalValue = live.goalValue,
        units = units,
        placeLabel = placeLabel,
    )
    if (live.goalType == GoalType.PLACE) {
        Text(
            text = if (live.goalReached) {
                stringResource(R.string.session_goal_met)
            } else {
                stringResource(R.string.session_goal_place_target, targetText)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val currentText = when (live.goalType) {
        GoalType.TIME -> StepMath.formatDuration(live.goalCurrent.toLong())
        GoalType.STEPS -> stepsText(live.goalCurrent.toInt())
        else -> distanceText(live.goalCurrent, units)
    }
    val fraction = if (live.goalValue > 0.0) {
        (live.goalCurrent / live.goalValue).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    SectionHeader(title = stringResource(R.string.session_goal_label))
    Text(
        text = stringResource(R.string.session_goal_progress, currentText, targetText),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NoSession(
    pendingCheckpoint: Boolean,
    onResumePending: () -> Unit,
    onDiscardPending: () -> Unit,
    onStartSetup: () -> Unit,
) {
    if (pendingCheckpoint) {
        // An interrupted session was checkpointed on disk: offer both honest choices.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.session_resume_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.session_resume_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                SabeelPrimaryButton(
                    text = stringResource(R.string.session_resume_action),
                    onClick = onResumePending,
                )
                Spacer(Modifier.height(8.dp))
                SabeelSecondaryButton(
                    text = stringResource(R.string.session_resume_discard),
                    onClick = onDiscardPending,
                )
            }
        }
    }
    EmptyState(
        icon = Icons.Filled.LocationOn,
        title = stringResource(R.string.session_none_title),
        body = stringResource(R.string.session_none_body),
        action = {
            SabeelPrimaryButton(
                text = stringResource(R.string.home_start_walk),
                onClick = onStartSetup,
            )
        },
    )
}

@Composable
private fun SessionSummary(
    live: WalkTracker.Live,
    units: Units,
    placeLabel: String?,
    onDone: () -> Unit,
    onStartAnother: () -> Unit,
) {
    Text(
        text = stringResource(R.string.session_summary_title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = stringResource(R.string.session_summary_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val goalResult = when {
        live.goalType == GoalType.NONE -> stringResource(R.string.session_summary_no_goal)
        live.goalReached -> stringResource(R.string.session_summary_goal_met)
        else -> stringResource(R.string.session_summary_goal_missed)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SummaryRow(
                label = stringResource(R.string.session_activity_label),
                value = sessionActivityLabel(live.activityType),
            )
            SummaryRow(
                label = stringResource(R.string.session_goal_label),
                value = sessionGoalTargetText(
                    goalType = live.goalType,
                    goalValue = live.goalValue,
                    units = units,
                    placeLabel = placeLabel,
                ),
            )
            SummaryRow(label = stringResource(R.string.session_goal_result), value = goalResult)
            SummaryRow(
                label = stringResource(R.string.walk_live_time),
                value = StepMath.formatDuration(live.elapsedSec.toLong()),
            )
            SummaryRow(
                label = stringResource(R.string.walk_live_steps),
                value = stepsText(live.steps),
            )
            SummaryRow(
                label = stringResource(R.string.walk_live_distance),
                value = distanceText(live.distanceMeters, units),
            )
            SummaryRow(
                label = stringResource(R.string.walk_live_pace),
                value = paceText(live.secondsPerKm, units),
            )
            live.savedWalkId?.let { id ->
                Text(
                    text = stringResource(R.string.session_summary_saved_id, id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (live.estimated) {
        Text(
            text = stringResource(R.string.walk_estimated_saved_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (live.route.size >= 2) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            RouteSketch(
                route = live.route,
                contentDescription = stringResource(R.string.walk_cd_route, live.route.size),
                sketchHeight = 160.dp,
            )
        }
    }
    SabeelPrimaryButton(
        text = stringResource(R.string.action_done),
        onClick = onDone,
    )
    SabeelSecondaryButton(
        text = stringResource(R.string.home_start_walk),
        onClick = onStartAnother,
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Small state chip above the session stats (activity, goal). */
@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun liveGoalChipText(
    live: WalkTracker.Live,
    units: Units,
    placeLabel: String?,
): String = when (live.goalType) {
    GoalType.NONE -> stringResource(R.string.session_goal_chip_none)
    GoalType.PLACE -> stringResource(
        R.string.session_goal_place_target,
        placeLabel ?: stringResource(R.string.session_goal_place),
    )
    else -> stringResource(
        R.string.session_goal_chip_label,
        sessionGoalTargetText(live.goalType, live.goalValue, units),
    )
}
