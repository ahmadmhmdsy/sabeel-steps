package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepSource
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.StepServiceStatus
import com.sabeel.steps.ui.components.BarChart
import com.sabeel.steps.ui.components.BarChartEntry
import com.sabeel.steps.ui.components.CountingStatusCard
import com.sabeel.steps.ui.components.PermissionCard
import com.sabeel.steps.ui.components.ProgressRing
import com.sabeel.steps.ui.components.SabeelPrimaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader
import com.sabeel.steps.ui.components.StatCard
import com.sabeel.steps.ui.components.StreakChip
import com.sabeel.steps.ui.format.caloriesText
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.shortDayLabel
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.format.timeText
import com.sabeel.steps.ui.viewmodel.HomeUiState
import com.sabeel.steps.ui.viewmodel.HomeViewModel

/**
 * Stateful entry point kept for callers that do not hoist the ViewModel: it owns the home
 * ViewModel and forwards its [HomeViewModel.startCounting] to the status card.
 */
@Composable
fun HomeScreen(onStartWalk: () -> Unit, onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        ui = ui,
        onStartWalk = onStartWalk,
        onStartCounting = { viewModel.startCounting() },
        onOpenDiagnostics = onOpenDiagnostics,
    )
}

/** Stateful entry point that lets the caller supply the counting action (Lead wiring). */
@Composable
fun HomeScreen(
    onStartWalk: () -> Unit,
    onStartCounting: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        ui = ui,
        onStartWalk = onStartWalk,
        onStartCounting = onStartCounting,
        onOpenDiagnostics = onOpenDiagnostics,
    )
}

/**
 * Stateless home screen: the ViewModel state is passed in and the counting and diagnostics
 * actions are supplied by the caller.
 */
@Composable
fun HomeScreen(
    ui: HomeUiState,
    onStartWalk: () -> Unit,
    onStartCounting: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    var permissionEpoch by remember { mutableIntStateOf(0) }
    // Permission state can change in system Settings while Home stays on screen; re-check on resume.
    LifecycleResumeEffect(Unit) {
        permissionEpoch++
        onPauseOrDispose { }
    }
    val activityPermission = Permissions.activityRecognitionPermission()
    val hasActivityPermission = remember(permissionEpoch, activityPermission) {
        Permissions.hasActivityRecognition(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }

    HomeContent(
        ui = ui,
        fontScale = LocalConfiguration.current.fontScale,
        activityPermissionGranted = hasActivityPermission,
        onGrantPermission = {
            activityPermission?.let { permissionLauncher.launch(it) }
        },
        onStartWalk = onStartWalk,
        onStartCounting = onStartCounting,
        onOpenDiagnostics = onOpenDiagnostics,
    )
}

@Composable
private fun HomeContent(
    ui: HomeUiState,
    fontScale: Float,
    activityPermissionGranted: Boolean,
    onGrantPermission: () -> Unit,
    onStartWalk: () -> Unit,
    onStartCounting: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val steps = ui.today?.steps ?: 0
    // The marker follows the data, not just the live source: a day that contains estimated steps
    // stays marked even after a real sensor takes over again.
    val estimated = ui.sourceStatus.source == StepSource.MOTION_ESTIMATE ||
        (ui.today?.estimatedSteps ?: 0) > 0
    val synced = ui.sourceStatus.source == StepSource.HEALTH_CONNECT
    val goal = ui.settings.goalSteps.coerceAtLeast(1)
    val progress = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val stackedCards = fontScale > 1.3f
    // Shrink the ring and its number as text grows so a 5-digit count never ellipsises.
    val ringSize = when {
        fontScale > 1.5f -> 180.dp
        fontScale > 1.2f -> 210.dp
        else -> 230.dp
    }
    val stepsTextValue = stepsText(steps)
    val stepsLabel = if (estimated) {
        stringResource(R.string.estimated_steps_value, stepsTextValue)
    } else {
        stepsTextValue
    }
    val bigStyle = when {
        fontScale > 1.5f && stepsLabel.length >= 6 -> MaterialTheme.typography.titleLarge
        fontScale > 1.5f -> MaterialTheme.typography.headlineSmall
        stepsLabel.length >= 6 -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.headlineLarge
    }
    val todayKey = DateKey.today()
    val byDate = remember(ui.last7, todayKey) { ui.last7.associateBy { it.date } }
    val days = remember(todayKey) { (6 downTo 0).map { offset -> DateKey.minusDays(todayKey, offset) } }
    val entries = remember(days, byDate) {
        days.map { date ->
            BarChartEntry(label = "", value = byDate[date]?.steps ?: 0)
        }
    }
    val labelledEntries = entries.mapIndexed { index, entry ->
        entry.copy(label = shortDayLabel(days[index]))
    }
    val average = if (entries.isEmpty()) 0 else entries.sumOf { it.value } / entries.size
    val rangeLabel = pluralStringResource(R.plurals.history_range_days, days.size, days.size)
    val chartDescription = stringResource(R.string.home_cd_chart, rangeLabel, average)
    val ringDescription = stringResource(
        R.string.home_cd_ring,
        (progress * 100f).toInt(),
        stepsLabel,
        stepsText(goal),
    )

    ScreenScaffold(title = stringResource(R.string.home_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StreakChip(days = ui.streak)
            Text(
                text = stringResource(R.string.home_goal_label, stepsText(goal)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Counting health first: a stopped service or a missing sensor is the cause the user can act
        // on. NO_PERMISSION keeps the permission card; STOPPED/NO_SENSOR get their own status card.
        if (!activityPermissionGranted || ui.serviceStatus == StepServiceStatus.NO_PERMISSION) {
            PermissionCard(
                title = stringResource(R.string.permission_activity_title),
                body = stringResource(R.string.permission_activity_body),
                granted = false,
                grantedLabel = stringResource(R.string.permission_granted),
                grantLabel = stringResource(R.string.permission_grant),
                onGrant = onGrantPermission,
            )
        }
        when (ui.serviceStatus) {
            StepServiceStatus.STOPPED -> CountingStatusCard(
                title = stringResource(R.string.home_status_stopped_title),
                body = stringResource(R.string.home_status_stopped_body),
                actionLabel = stringResource(R.string.home_status_action_start),
                onAction = onStartCounting,
            )

            StepServiceStatus.NO_SENSOR -> CountingStatusCard(
                title = stringResource(R.string.home_status_no_sensor_title),
                body = stringResource(R.string.home_status_no_sensor_body),
            )

            else -> Unit
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                contentDescription = ringDescription,
                ringSize = ringSize,
                showCompleteBadge = progress >= 1f,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stepsLabel,
                        style = bigStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.home_ring_goal, stepsText(goal)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // An estimate is never presented as a measured fact. The marker sits on the number and the
        // plain-language note explains where it came from.
        if (estimated) {
            Text(
                text = stringResource(R.string.home_estimated_note),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else if (synced) {
            // Synced steps are real measurements made elsewhere, not an estimate, but they are not
            // live either: say where they came from and that they can lag.
            Text(
                text = stringResource(R.string.home_health_connect_note),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Active minutes live under the ring rather than in the metric row: a third card cannot fit
        // at 360dp without ellipsising its value at larger font scales.
        Text(
            text = pluralStringResource(
                R.plurals.active_minutes_phrase,
                ui.today?.activeMinutes ?: 0,
                ui.today?.activeMinutes ?: 0,
            ),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Text(
            text = statusLine(steps = steps, goal = goal),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Honest data age: the stored row's own update time, not a claim that the sensor is alive.
        val updatedAt = ui.today?.updatedAt ?: 0L
        if (updatedAt > 0L) {
            Text(
                text = stringResource(R.string.home_updated_at, timeText(updatedAt)),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        // A rest day is a deliberate pause, so it is named on Home rather than hidden in Settings.
        if (ui.restDayToday) {
            Text(
                text = stringResource(R.string.home_rest_day_note),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }

        // Primary CTA before the stats and the 7-day chart, so it stays above the fold on 360x640.
        SabeelPrimaryButton(
            text = stringResource(R.string.home_start_walk),
            onClick = onStartWalk,
            iconRes = R.drawable.ic_walk,
        )

        if (stackedCards) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = distanceText(ui.today?.distanceMeters ?: 0.0, ui.settings.units),
                    label = stringResource(R.string.home_distance),
                    modifier = Modifier.fillMaxWidth(),
                )
                StatCard(
                    value = caloriesText(ui.today?.calories ?: 0.0),
                    label = stringResource(R.string.home_calories),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = distanceText(ui.today?.distanceMeters ?: 0.0, ui.settings.units),
                    label = stringResource(R.string.home_distance),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = caloriesText(ui.today?.calories ?: 0.0),
                    label = stringResource(R.string.home_calories),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionHeader(title = stringResource(R.string.home_last_7_days))
        BarChart(
            entries = labelledEntries,
            goal = goal,
            contentDescription = chartDescription,
            chartHeight = 120.dp,
        )
        TextButton(
            onClick = onOpenDiagnostics,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.home_diagnostics_link),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun statusLine(steps: Int, goal: Int): String = when {
    steps <= 0 -> stringResource(R.string.home_status_start)
    steps >= goal -> stringResource(R.string.home_status_done)
    steps >= (goal * 0.5f) -> stringResource(R.string.home_status_mid)
    else -> stringResource(R.string.home_status_early)
}
