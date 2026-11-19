package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.service.WalkTracker
import com.sabeel.steps.ui.components.EmptyState
import com.sabeel.steps.ui.components.PermissionCard
import com.sabeel.steps.ui.components.RouteSketch
import com.sabeel.steps.ui.components.SabeelPrimaryButton
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.StatCard
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.paceText
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.viewmodel.WalkViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn

@Composable
fun WalkScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: WalkViewModel = viewModel(factory = WalkViewModel.factory(app))
    val live by viewModel.live.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val walkUi by viewModel.state.collectAsStateWithLifecycle()

    var permissionEpoch by rememberSaveable { mutableIntStateOf(0) }
    var pendingStart by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    // The user may grant location in system Settings while this screen stays in the back stack.
    LifecycleResumeEffect(Unit) {
        permissionEpoch++
        onPauseOrDispose { }
    }
    val hasLocation = remember(permissionEpoch) { Permissions.hasLocation(context) }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionEpoch++
        if (pendingStart) {
            pendingStart = false
            viewModel.start()
        }
    }

    val requestLocationAndStart = {
        val permissions = Permissions.locationPermissions()
        if (permissions.isEmpty() || hasLocation) {
            viewModel.start()
        } else {
            pendingStart = true
            locationLauncher.launch(permissions.toTypedArray())
        }
    }

    val showSummary = live.savedWalkId != null && !live.running
    val title = if (live.running) stringResource(R.string.walk_live_title) else stringResource(R.string.walk_title)
    ScreenScaffold(title = title, onBack = onBack) {
        when {
            showSummary -> WalkSummary(
                live = live,
                units = settings.units,
                onDone = {
                    viewModel.clearSummary()
                    onDone()
                },
            )

            live.running -> WalkLive(
                live = live,
                units = settings.units,
                onSave = { viewModel.finish() },
                onDiscardRequest = { showDiscardDialog = true },
                onOpenPermission = {
                    permissionEpoch++
                    locationLauncher.launch(Permissions.locationPermissions().toTypedArray())
                },
            )

            else -> WalkIdle(
                hasLocation = hasLocation,
                pendingCheckpoint = walkUi.pendingCheckpoint,
                onGrantLocation = {
                    locationLauncher.launch(Permissions.locationPermissions().toTypedArray())
                },
                onStart = requestLocationAndStart,
                onResumePending = { viewModel.resumePending() },
                onDiscardPending = { viewModel.discardPending() },
            )
        }
        if (walkUi.startBlocked && !live.running && !showSummary) {
            Text(
                text = stringResource(R.string.walk_start_blocked),
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.walk_discard_confirm_title)) },
            text = { Text(stringResource(R.string.walk_discard_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discard()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.walk_discard),
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
private fun WalkIdle(
    hasLocation: Boolean,
    pendingCheckpoint: Boolean,
    onGrantLocation: () -> Unit,
    onStart: () -> Unit,
    onResumePending: () -> Unit,
    onDiscardPending: () -> Unit,
) {
    EmptyState(
        icon = Icons.Filled.LocationOn,
        title = stringResource(R.string.walk_idle_title),
        body = stringResource(R.string.walk_idle_body),
    )
    if (pendingCheckpoint) {
        // An interrupted walk was checkpointed on disk: offer both honest choices, resume or discard.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.walk_resume_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.walk_resume_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                SabeelPrimaryButton(
                    text = stringResource(R.string.walk_resume_action),
                    onClick = onResumePending,
                )
                Spacer(Modifier.height(8.dp))
                SabeelSecondaryButton(
                    text = stringResource(R.string.walk_resume_discard),
                    onClick = onDiscardPending,
                )
            }
        }
    }
    PermissionCard(
        title = stringResource(R.string.permission_location_title),
        body = stringResource(R.string.permission_location_body),
        granted = hasLocation,
        grantedLabel = stringResource(R.string.walk_location_ready),
        grantLabel = stringResource(R.string.permission_grant),
        onGrant = onGrantLocation,
    )
    if (!hasLocation) {
        Text(
            text = stringResource(R.string.walk_location_fallback),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SabeelPrimaryButton(
        text = stringResource(R.string.walk_start),
        onClick = onStart,
    )
}

@Composable
private fun WalkLive(
    live: WalkTracker.Live,
    units: Units,
    onSave: () -> Unit,
    onDiscardRequest: () -> Unit,
    onOpenPermission: () -> Unit,
) {
    val fontScale = LocalConfiguration.current.fontScale
    val stepsValue = if (live.estimated) {
        stringResource(R.string.estimated_steps_value, stepsText(live.steps))
    } else {
        stepsText(live.steps)
    }
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.walk_live_time),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = StepMath.formatDuration(live.elapsedSec.toLong()),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
    if (fontScale > 1.3f) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            StatCard(
                value = stepsValue,
                label = stringResource(R.string.walk_live_steps),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            StatCard(
                value = stepsValue,
                label = stringResource(R.string.walk_live_steps),
                modifier = Modifier.weight(1f),
            )
        }
    }
    Text(
        text = stringResource(
            if (live.gpsActive) R.string.walk_gps_on else R.string.walk_gps_off,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (live.estimated) {
        Text(
            text = stringResource(R.string.walk_estimated_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    if (!live.gpsActive) {
        TextButton(onClick = onOpenPermission) {
            Text(stringResource(R.string.permission_grant))
        }
    }
    SabeelPrimaryButton(
        text = stringResource(R.string.walk_save),
        onClick = onSave,
        enabled = !live.saving,
    )
    SabeelSecondaryButton(
        text = stringResource(R.string.walk_discard),
        onClick = onDiscardRequest,
        enabled = !live.saving,
    )
}

@Composable
private fun WalkSummary(
    live: WalkTracker.Live,
    units: Units,
    onDone: () -> Unit,
) {
    val stepsValue = if (live.estimated) {
        stringResource(R.string.estimated_steps_value, stepsText(live.steps))
    } else {
        stepsText(live.steps)
    }
    Text(
        text = stringResource(R.string.walk_summary_title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = stringResource(R.string.walk_summary_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            value = StepMath.formatDuration(live.elapsedSec.toLong()),
            label = stringResource(R.string.walk_live_time),
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
        StatCard(
            value = stepsValue,
            label = stringResource(R.string.walk_live_steps),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (live.estimated) {
        Text(
            text = stringResource(R.string.walk_estimated_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (live.route.size >= 2) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
    Spacer(Modifier.height(8.dp))
}