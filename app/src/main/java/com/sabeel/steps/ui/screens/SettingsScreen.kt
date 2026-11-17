package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.AppLanguage
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.Units
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.ui.components.SabeelPrimaryButton
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader
import com.sabeel.steps.ui.components.Stepper
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.format.timeOfDayText
import com.sabeel.steps.ui.i18n.AppLocale
import com.sabeel.steps.ui.viewmodel.SettingsViewModel

private val GoalStep = 500
private val HeightStep = 1
private val WeightStep = 1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onDeletedAll: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var permissionEpoch by remember { mutableIntStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context, uri) { result ->
                onMessage(
                    result.fold(
                        onSuccess = { context.getString(R.string.settings_export_success, it) },
                        onFailure = { context.getString(R.string.settings_export_error) },
                    ),
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(context, uri) { result ->
                onMessage(
                    result.fold(
                        onSuccess = { context.getString(R.string.settings_import_success, it) },
                        onFailure = { context.getString(R.string.settings_import_error) },
                    ),
                )
            }
        }
    }
    val notificationPermission = Permissions.notificationPermission()
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionEpoch++
        viewModel.setReminderEnabled(granted)
    }
    val notificationsGranted = remember(permissionEpoch) { Permissions.hasNotifications(context) }

    ScreenScaffold(title = stringResource(R.string.settings_title)) {

        SectionHeader(title = stringResource(R.string.settings_goal))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Stepper(
                    valueText = stringResource(R.string.goal_value, stepsText(settings.goalSteps)),
                    decreaseLabel = stringResource(R.string.cd_decrease),
                    increaseLabel = stringResource(R.string.cd_increase),
                    onDecrease = { viewModel.setGoal(settings.goalSteps - GoalStep) },
                    onIncrease = { viewModel.setGoal(settings.goalSteps + GoalStep) },
                )
                Text(
                    text = stringResource(R.string.settings_goal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionHeader(title = stringResource(R.string.settings_units))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.units == Units.METRIC,
                onClick = { viewModel.setUnits(Units.METRIC) },
                label = { Text(stringResource(R.string.settings_units_metric)) },
            )
            FilterChip(
                selected = settings.units == Units.IMPERIAL,
                onClick = { viewModel.setUnits(Units.IMPERIAL) },
                label = { Text(stringResource(R.string.settings_units_imperial)) },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_reminder))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reminder_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked && notificationPermission != null && !notificationsGranted) {
                                notificationLauncher.launch(notificationPermission)
                            } else {
                                viewModel.setReminderEnabled(checked)
                            }
                        },
                    )
                }
                if (settings.reminderEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_reminder_time),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = timeOfDayText(settings.reminderMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.settings_body_data))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_height),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Stepper(
                        valueText = stringResource(R.string.value_cm, settings.heightCm),
                        decreaseLabel = stringResource(R.string.cd_decrease),
                        increaseLabel = stringResource(R.string.cd_increase),
                        onDecrease = { viewModel.setHeight(settings.heightCm - HeightStep) },
                        onIncrease = { viewModel.setHeight(settings.heightCm + HeightStep) },
                        modifier = Modifier.weight(1.4f),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_weight),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Stepper(
                        valueText = stringResource(R.string.value_kg, settings.weightKg),
                        decreaseLabel = stringResource(R.string.cd_decrease),
                        increaseLabel = stringResource(R.string.cd_increase),
                        onDecrease = { viewModel.setWeight(settings.weightKg - WeightStep) },
                        onIncrease = { viewModel.setWeight(settings.weightKg + WeightStep) },
                        modifier = Modifier.weight(1.4f),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(R.string.settings_gender),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = settings.gender == Gender.UNSPECIFIED,
                        onClick = { viewModel.setGender(Gender.UNSPECIFIED) },
                        label = { Text(stringResource(R.string.gender_unspecified)) },
                    )
                    FilterChip(
                        selected = settings.gender == Gender.MALE,
                        onClick = { viewModel.setGender(Gender.MALE) },
                        label = { Text(stringResource(R.string.gender_male)) },
                    )
                    FilterChip(
                        selected = settings.gender == Gender.FEMALE,
                        onClick = { viewModel.setGender(Gender.FEMALE) },
                        label = { Text(stringResource(R.string.gender_female)) },
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.settings_language))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = AppLocale.current(context) == AppLanguage.EN,
                onClick = {
                    viewModel.syncLanguage(AppLanguage.EN)
                    AppLocale.apply(context, AppLanguage.EN)
                },
                label = { Text(stringResource(R.string.settings_language_en)) },
            )
            FilterChip(
                selected = AppLocale.current(context) == AppLanguage.AR,
                onClick = {
                    viewModel.syncLanguage(AppLanguage.AR)
                    AppLocale.apply(context, AppLanguage.AR)
                },
                label = { Text(stringResource(R.string.settings_language_ar)) },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_data))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_export_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                val exportFileName = stringResource(R.string.settings_export_file_name)
                SabeelSecondaryButton(
                    text = stringResource(R.string.settings_export_csv),
                    onClick = { exportLauncher.launch(exportFileName) },
                    icon = Icons.Filled.Share,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_import_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SabeelSecondaryButton(
                    text = stringResource(R.string.settings_import_csv),
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
                    icon = Icons.Filled.Refresh,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_delete_all_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SabeelPrimaryButton(
                    text = stringResource(R.string.settings_delete_all),
                    onClick = { showDeleteDialog = true },
                    icon = Icons.Filled.Delete,
                )
            }
        }

        SabeelSecondaryButton(
            text = stringResource(R.string.settings_privacy),
            onClick = onOpenPrivacy,
        )
        SabeelSecondaryButton(
            text = stringResource(R.string.settings_about),
            onClick = onOpenAbout,
        )
        Text(
            text = stringResource(R.string.settings_estimate_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = settings.reminderMinutes / 60,
            initialMinute = settings.reminderMinutes % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_reminder_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setReminderMinutes(timeState.hour * 60 + timeState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAll { onDeletedAll() }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
