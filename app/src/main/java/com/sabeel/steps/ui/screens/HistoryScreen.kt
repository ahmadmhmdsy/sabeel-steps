package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.ui.components.BarChart
import com.sabeel.steps.ui.components.BarChartEntry
import com.sabeel.steps.ui.components.EmptyState
import com.sabeel.steps.ui.components.RouteSketch
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader
import com.sabeel.steps.ui.format.dateTimeText
import com.sabeel.steps.ui.format.dayLabel
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.paceText
import com.sabeel.steps.ui.format.shortDayLabel
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

/** One saved walk, shaped for the history list by the ViewModel (frozen contract). */
data class SavedWalkUi(
    val id: Long,
    val startedAtMillis: Long,
    val durationSeconds: Long,
    val steps: Int,
    val distanceMeters: Double,
    val secondsPerKm: Int,
    val route: List<RoutePoint>,
    val estimated: Boolean = false,
    /** Custom-session fields (Room v3). Legacy v1.2 walks default to WALK / NONE. */
    val activityType: ActivityType = ActivityType.WALK,
    val goalType: GoalType = GoalType.NONE,
    val goalValue: Double = 0.0,
    val targetMet: Boolean = false,
)

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()

    val sortedAscending = remember(ui.days) { ui.days.sortedBy { it.date } }
    val entries = remember(sortedAscending) {
        sortedAscending.map { day -> BarChartEntry(label = "", value = day.steps) }
    }
    val labelled = entries.mapIndexed { index, entry ->
        entry.copy(label = if (entries.size <= 7) shortDayLabel(sortedAscending[index].date) else "")
    }
    val rangeLabel = pluralStringResource(R.plurals.history_range_days, ui.range, ui.range)
    val average = remember(ui.days) {
        if (ui.days.isEmpty()) 0 else ui.days.sumOf { it.steps } / ui.days.size
    }
    val chartDescription = stringResource(R.string.history_cd_chart, rangeLabel, average)
    val descending = remember(ui.days) { ui.days.sortedByDescending { it.date } }
    val totalSteps = remember(ui.days) { ui.days.sumOf { it.steps } }

    var selectedWalkId by rememberSaveable { mutableStateOf(-1L) }
    val selectedWalk = ui.walks.firstOrNull { it.id == selectedWalkId }

    // Session export UI: the destination file is created through the system file picker (SAF), so
    // there is no storage permission and nothing leaves the phone except the file the user chose.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sessionActive by viewModel.sessionActive.collectAsStateWithLifecycle()
    // Survives the SAF activity round-trip so the GPX callback knows which session to write.
    var pendingGpxWalkId by rememberSaveable { mutableStateOf(-1L) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportAllSessionsCsv(context, uri) { result ->
                result.fold(
                    onSuccess = { count ->
                        showMessage(context.getString(R.string.history_export_csv_success, count))
                    },
                    onFailure = { showMessage(context.getString(R.string.history_export_error)) },
                )
            }
        }
    }
    val gpxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml"),
    ) { uri ->
        val walkId = pendingGpxWalkId
        pendingGpxWalkId = -1L
        if (uri != null && walkId > 0L) {
            viewModel.exportSessionGpx(context, uri, walkId) { result ->
                result.fold(
                    onSuccess = { exported ->
                        showMessage(
                            context.getString(
                                if (exported) {
                                    R.string.history_export_gpx_success
                                } else {
                                    R.string.history_export_gpx_missing
                                },
                            ),
                        )
                    },
                    onFailure = { showMessage(context.getString(R.string.history_export_error)) },
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(title = stringResource(R.string.history_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = ui.range == 7,
                    onClick = { viewModel.setRange(7) },
                    label = { Text(stringResource(R.string.history_range_7)) },
                )
                FilterChip(
                    selected = ui.range == 30,
                    onClick = { viewModel.setRange(30) },
                    label = { Text(stringResource(R.string.history_range_30)) },
                )
            }

            if (ui.days.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                )
            } else {
                BarChart(
                    entries = labelled,
                    goal = ui.settings.goalSteps.coerceAtLeast(1),
                    contentDescription = chartDescription,
                    chartHeight = 160.dp,
                    showLabels = ui.range <= 7,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.history_range_label, rangeLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.history_total, stepsText(totalSteps)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column {
                    descending.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dayLabel(day.date),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = distanceText(day.distanceMeters, ui.settings.units),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (day.estimatedSteps > 0) {
                                    Text(
                                        text = stringResource(R.string.history_estimated_badge),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stepsText(day.steps),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.history_sessions_title))
            SabeelSecondaryButton(
                text = stringResource(R.string.history_export_csv),
                onClick = {
                    if (!sessionActive && ui.walks.isNotEmpty()) {
                        csvExportLauncher.launch(EXPORT_CSV_FILE_NAME)
                    }
                },
                enabled = !sessionActive && ui.walks.isNotEmpty(),
            )
            if (sessionActive) {
                Text(
                    text = stringResource(R.string.history_export_running_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ui.walks.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_sessions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    ui.walks.forEach { walk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = stringResource(R.string.history_session_detail_title),
                                ) { selectedWalkId = walk.id }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dateTimeText(walk.startedAtMillis),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = listOf(
                                        sessionActivityLabel(walk.activityType),
                                        StepMath.formatDuration(walk.durationSeconds),
                                        distanceText(walk.distanceMeters, ui.settings.units),
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (walk.goalType != GoalType.NONE) {
                                    val goalText = sessionGoalTargetText(
                                        goalType = walk.goalType,
                                        goalValue = walk.goalValue,
                                        units = ui.settings.units,
                                    )
                                    val metText = if (walk.targetMet) {
                                        stringResource(R.string.history_session_goal_met)
                                    } else {
                                        stringResource(R.string.history_session_goal_missed)
                                    }
                                    Text(
                                        text = stringResource(R.string.session_goal_chip_label, goalText) +
                                            " · " + metText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (walk.targetMet) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                                if (walk.estimated) {
                                    Text(
                                        text = stringResource(R.string.history_estimated_badge),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.goal_value, stepsText(walk.steps)),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }

    if (selectedWalk != null) {
        SavedWalkDialog(
            walk = selectedWalk,
            units = ui.settings.units,
            exportEnabled = !sessionActive,
            onExportGpx = {
                pendingGpxWalkId = selectedWalk.id
                selectedWalkId = -1L
                gpxExportLauncher.launch(exportGpxFileName(selectedWalk.id))
            },
            onClose = { selectedWalkId = -1L },
        )
    }
}

@Composable
private fun SavedWalkDialog(
    walk: SavedWalkUi,
    units: com.sabeel.steps.domain.Units,
    exportEnabled: Boolean,
    onExportGpx: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.history_session_detail_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = dateTimeText(walk.startedAtMillis),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DetailRow(
                    label = stringResource(R.string.session_activity_label),
                    value = sessionActivityLabel(walk.activityType),
                )
                DetailRow(
                    label = stringResource(R.string.session_goal_label),
                    value = sessionGoalTargetText(
                        goalType = walk.goalType,
                        goalValue = walk.goalValue,
                        units = units,
                    ),
                )
                if (walk.goalType != GoalType.NONE) {
                    DetailRow(
                        label = stringResource(R.string.session_goal_result),
                        value = stringResource(
                            if (walk.targetMet) {
                                R.string.history_session_goal_met
                            } else {
                                R.string.history_session_goal_missed
                            },
                        ),
                    )
                }
                DetailRow(
                    label = stringResource(R.string.walk_live_time),
                    value = StepMath.formatDuration(walk.durationSeconds),
                )
                DetailRow(
                    label = stringResource(R.string.walk_live_steps),
                    value = stepsText(walk.steps),
                )
                DetailRow(
                    label = stringResource(R.string.walk_live_distance),
                    value = distanceText(walk.distanceMeters, units),
                )
                DetailRow(
                    label = stringResource(R.string.walk_live_pace),
                    value = paceText(walk.secondsPerKm, units),
                )
                if (walk.estimated) {
                    Text(
                        text = stringResource(R.string.walk_estimated_saved_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (walk.route.isNotEmpty()) {
                    RouteSketch(
                        route = walk.route,
                        contentDescription = stringResource(R.string.walk_cd_route, walk.route.size),
                        sketchHeight = 150.dp,
                    )
                }
                SabeelSecondaryButton(
                    text = stringResource(R.string.history_export_gpx),
                    onClick = onExportGpx,
                    enabled = exportEnabled,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.history_walk_close))
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
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

/** Suggested SAF file names: stable, ASCII and not localized, so the exported file is portable. */
private const val EXPORT_CSV_FILE_NAME = "sabeel-sessions.csv"

private fun exportGpxFileName(walkId: Long): String = "sabeel-session-" + walkId + ".gpx"