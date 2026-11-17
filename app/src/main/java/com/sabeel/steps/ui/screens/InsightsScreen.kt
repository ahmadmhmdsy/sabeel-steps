package com.sabeel.steps.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.ui.components.EmptyState
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.StatCard
import com.sabeel.steps.ui.components.StreakChip
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.dayLabel
import com.sabeel.steps.ui.format.percentText
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.viewmodel.InsightsViewModel

@Composable
fun InsightsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val stackedCards = LocalConfiguration.current.fontScale > 1.3f

    ScreenScaffold(title = stringResource(R.string.insights_title)) {
        if (ui.days.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Star,
                title = stringResource(R.string.insights_empty_title),
                body = stringResource(R.string.insights_empty_body),
            )
            return@ScreenScaffold
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            StreakChip(days = ui.streak)
        }
        val rangeLabel = pluralStringResource(R.plurals.history_range_days, ui.days.size, ui.days.size)
        if (stackedCards) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = stepsText(ui.averageSteps),
                    label = stringResource(R.string.insights_average),
                    modifier = Modifier.fillMaxWidth(),
                )
                StatCard(
                    value = stepsText(ui.bestDay?.steps ?: 0),
                    label = stringResource(R.string.insights_best_day),
                    modifier = Modifier.fillMaxWidth(),
                )
                StatCard(
                    value = percentText(ui.goalHitRate.toFloat()),
                    label = stringResource(R.string.insights_goal_rate),
                    modifier = Modifier.fillMaxWidth(),
                )
                StatCard(
                    value = distanceText(ui.totalDistanceMeters, ui.settings.units),
                    label = stringResource(R.string.insights_total_distance),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = stepsText(ui.averageSteps),
                    label = stringResource(R.string.insights_average),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = stepsText(ui.bestDay?.steps ?: 0),
                    label = stringResource(R.string.insights_best_day),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = percentText(ui.goalHitRate.toFloat()),
                    label = stringResource(R.string.insights_goal_rate),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = distanceText(ui.totalDistanceMeters, ui.settings.units),
                    label = stringResource(R.string.insights_total_distance),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.insights_sentence_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.insights_sentence,
                        stepsText(ui.averageSteps),
                        ui.goalHitDays,
                        rangeLabel,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (ui.bestDay != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.insights_best_sentence,
                            dayLabel(ui.bestDay?.date ?: ""),
                            stepsText(ui.bestDay?.steps ?: 0),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.insights_estimate_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}