package com.sabeel.steps.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.ui.components.BarChart
import com.sabeel.steps.ui.components.BarChartEntry
import com.sabeel.steps.ui.components.EmptyState
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.format.dayLabel
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.shortDayLabel
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()

    val todayKey = DateKey.today()
    val sortedAscending = remember(ui.days) { ui.days.sortedBy { it.date } }
    val entries = remember(sortedAscending, ui.range) {
        sortedAscending.map { day -> BarChartEntry(label = "", value = day.steps) }
    }
    val labelled = entries.mapIndexed { index, entry ->
        entry.copy(label = if (entries.size <= 7) shortDayLabel(sortedAscending[index].date) else "")
    }
    val chartDescription = stringResource(
        R.string.history_cd_chart,
        ui.range,
        ui.days.sumOf { it.steps } / ui.days.size.coerceAtLeast(1),
    )
    val descending = remember(ui.days) { ui.days.sortedByDescending { it.date } }
    val totalSteps = remember(ui.days) { ui.days.sumOf { it.steps } }

    ScreenScaffold(title = stringResource(R.string.history_title), scrollable = false) {
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
                    text = stringResource(R.string.history_range_label, ui.range),
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
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(descending, key = { it.date }) { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
    }
}
