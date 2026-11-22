package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.sabeel.steps.ui.components.CountingPill
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
 * ViewModel, forwards its [HomeViewModel.startCounting] to the status card and its
 * [HomeViewModel.toggleCounting] to the ring and pill.
 *
 * [countingEnabled] and [onToggleCounting] are optional so an existing call site keeps compiling;
 * when null the screen reads the switch value from its own ViewModel state and toggles through the
 * same ViewModel. The app shell passes both when it wants to own the 5 s Undo snackbar.
 */
@Composable
fun HomeScreen(
    onStartWalk: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    countingEnabled: Boolean? = null,
    onToggleCounting: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        ui = ui,
        countingEnabled = countingEnabled ?: ui.countingEnabled,
        onStartWalk = onStartWalk,
        onStartCounting = { viewModel.startCounting() },
        onToggleCounting = onToggleCounting ?: { viewModel.toggleCounting() },
        onOpenDiagnostics = onOpenDiagnostics,
    )
}

/** Stateful entry point that lets the caller supply the counting action (Lead wiring). */
@Composable
fun HomeScreen(
    onStartWalk: () -> Unit,
    onStartCounting: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    countingEnabled: Boolean? = null,
    onToggleCounting: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        ui = ui,
        countingEnabled = countingEnabled ?: ui.countingEnabled,
        onStartWalk = onStartWalk,
        onStartCounting = onStartCounting,
        onToggleCounting = onToggleCounting ?: { viewModel.toggleCounting() },
        onOpenDiagnostics = onOpenDiagnostics,
    )
}

/**
 * Stateless home screen: the ViewModel state is passed in, [countingEnabled] is the switch value
 * for the ring (the same value shown by the pill), and [onToggleCounting] is the single toggle
 * action the ring and the pill dispatch.
 */
@Composable
fun HomeScreen(
    ui: HomeUiState,
    countingEnabled: Boolean,
    onStartWalk: () -> Unit,
    onStartCounting: () -> Unit,
    onToggleCounting: () -> Unit,
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

    // One haptic for every counting toggle (ring and pill): the control changes a running state, so
    // it confirms the tap even before TalkBack has announced the new switch state.
    val haptic = LocalHapticFeedback.current
    val toggleWithHaptic: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onToggleCounting()
    }

    HomeContent(
        ui = ui,
        countingEnabled = countingEnabled,
        fontScale = LocalConfiguration.current.fontScale,
        activityPermissionGranted = hasActivityPermission,
        onGrantPermission = {
            activityPermission?.let { permissionLauncher.launch(it) }
        },
        onStartWalk = onStartWalk,
        onStartCounting = onStartCounting,
        onToggleCounting = toggleWithHaptic,
        onOpenDiagnostics = onOpenDiagnostics,
    )
}

@Composable
private fun HomeContent(
    ui: HomeUiState,
    countingEnabled: Boolean,
    fontScale: Float,
    activityPermissionGranted: Boolean,
    onGrantPermission: () -> Unit,
    onStartWalk: () -> Unit,
    onStartCounting: () -> Unit,
    onToggleCounting: () -> Unit,
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
    val systemStoppedBody = if (ui.lastHeartbeatAt > 0L) {
        stringResource(R.string.home_system_stopped_body, timeText(ui.lastHeartbeatAt))
    } else {
        stringResource(R.string.home_system_stopped_body_none)
    }

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

        // Counting health first: a missing sensor or a missing permission is the cause the user can
        // act on. A deliberate OFF is not a failure, so it never gets a warning card - the ring, the
        // pill and the paused caption carry that state instead.
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
        when {
            ui.serviceStatus == StepServiceStatus.NO_SENSOR -> CountingStatusCard(
                title = stringResource(R.string.home_status_no_sensor_title),
                body = stringResource(R.string.home_status_no_sensor_body),
            )

            !countingEnabled -> Unit

            // Counting is wanted but the service is gone: say who stopped it and offer a restart,
            // never "you stopped it".
            ui.serviceStatus == StepServiceStatus.STOPPED_BY_SYSTEM ||
                ui.countingStoppedBySystem -> CountingStatusCard(
                title = stringResource(R.string.home_system_stopped_title),
                body = systemStoppedBody,
                actionLabel = stringResource(R.string.home_system_stopped_action),
                onAction = onStartCounting,
            )

            ui.serviceStatus == StepServiceStatus.STOPPED -> CountingStatusCard(
                title = stringResource(R.string.home_status_stopped_title),
                body = stringResource(R.string.home_status_stopped_body),
                actionLabel = stringResource(R.string.home_status_action_start),
                onAction = onStartCounting,
            )

            else -> Unit
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                contentDescription = ringDescription,
                ringSize = ringSize,
                showCompleteBadge = progress >= 1f,
                // Ring = the counting switch. The number inside stays the day total; TalkBack gets
                // the step progress plus the switch state in one merged node.
                countingEnabled = countingEnabled,
                onToggleCounting = onToggleCounting,
                toggleOnLabel = stringResource(R.string.home_ring_action_stop),
                toggleOffLabel = stringResource(R.string.home_ring_action_start),
                stateDescriptionOn = stringResource(R.string.home_ring_state_on),
                stateDescriptionOff = stringResource(R.string.home_ring_state_off),
                dimmed = !countingEnabled,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!countingEnabled) {
                        PauseGlyph()
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = stepsLabel,
                        style = bigStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (countingEnabled) {
                            stringResource(R.string.home_ring_goal, stepsText(goal))
                        } else {
                            stringResource(R.string.home_paused_caption)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Visible state + action under the ring: the owner can stop/start without discovering that
        // the ring itself is a switch.
        CountingPill(
            text = if (countingEnabled) {
                stringResource(R.string.home_counting_pill_on)
            } else {
                stringResource(R.string.home_counting_pill_off)
            },
            onClick = onToggleCounting,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

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
            text = if (countingEnabled) {
                statusLine(steps = steps, goal = goal)
            } else {
                stringResource(R.string.home_paused_line)
            },
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
        // The label deliberately names a session, not counting: the ring owns global counting.
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

/** Two rounded bars: the pause glyph at the top of the ring while counting is deliberately off. */
@Composable
private fun PauseGlyph() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun statusLine(steps: Int, goal: Int): String = when {
    steps <= 0 -> stringResource(R.string.home_status_start)
    steps >= goal -> stringResource(R.string.home_status_done)
    steps >= (goal * 0.5f) -> stringResource(R.string.home_status_mid)
    else -> stringResource(R.string.home_status_early)
}
