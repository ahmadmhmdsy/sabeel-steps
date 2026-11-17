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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.ui.components.BarChart
import com.sabeel.steps.ui.components.BarChartEntry
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
import com.sabeel.steps.ui.viewmodel.HomeUiState
import com.sabeel.steps.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onStartWalk: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()

    var permissionEpoch by remember { mutableIntStateOf(0) }
    val activityPermission = Permissions.activityRecognitionPermission()
    val hasActivityPermission = remember(permissionEpoch, activityPermission) {
        Permissions.hasActivityRecognition(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }

    val fontScale = LocalConfiguration.current.fontScale
    HomeContent(
        ui = ui,
        fontScale = fontScale,
        activityPermissionGranted = hasActivityPermission,
        onGrantPermission = {
            activityPermission?.let { permissionLauncher.launch(it) }
        },
        onStartWalk = onStartWalk,
    )
}

@Composable
private fun HomeContent(
    ui: HomeUiState,
    fontScale: Float,
    activityPermissionGranted: Boolean,
    onGrantPermission: () -> Unit,
    onStartWalk: () -> Unit,
) {
    val steps = ui.today?.steps ?: 0
    val goal = ui.settings.goalSteps.coerceAtLeast(1)
    val progress = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val stackedCards = fontScale > 1.3f
    val ringSize = if (fontScale > 1.5f) 190.dp else 230.dp
    val bigStyle = if (fontScale > 1.4f) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.headlineLarge
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
    val chartDescription = stringResource(R.string.home_cd_chart, entries.size, average)
    val ringDescription = stringResource(
        R.string.home_cd_ring,
        (progress * 100f).toInt(),
        stepsText(steps),
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

        if (!activityPermissionGranted) {
            PermissionCard(
                title = stringResource(R.string.permission_activity_title),
                body = stringResource(R.string.permission_activity_body),
                granted = false,
                grantedLabel = stringResource(R.string.permission_granted),
                grantLabel = stringResource(R.string.permission_grant),
                onGrant = onGrantPermission,
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                contentDescription = ringDescription,
                ringSize = ringSize,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stepsText(steps),
                        style = bigStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.home_ring_goal, stepsText(goal)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = statusLine(steps = steps, goal = goal),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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
                StatCard(
                    value = stringResource(R.string.value_minutes, ui.today?.activeMinutes ?: 0),
                    label = stringResource(R.string.home_active_minutes),
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
                StatCard(
                    value = stringResource(R.string.value_minutes, ui.today?.activeMinutes ?: 0),
                    label = stringResource(R.string.home_active_minutes),
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

        Spacer(Modifier.height(4.dp))
        SabeelPrimaryButton(
            text = stringResource(R.string.home_start_walk),
            onClick = onStartWalk,
            icon = Icons.Filled.PlayArrow,
        )
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
