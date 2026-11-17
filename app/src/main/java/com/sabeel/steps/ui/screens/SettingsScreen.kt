package com.sabeel.steps.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.BuildConfig
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.AppLanguage
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.Gender
import com.sabeel.steps.domain.GoalAdvisor
import com.sabeel.steps.domain.Units
import com.sabeel.steps.platform.Notifications
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
    onOpenDiagnostics: () -> Unit,
    onDeletedAll: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val restDays = viewModel.restDays.collectAsStateWithLifecycle().value
    val goalAdvice = viewModel.goalAdvice.collectAsStateWithLifecycle().value
    val restDayToday = DateKey.today() in restDays
    val fontScale = LocalConfiguration.current.fontScale
    val stackedRows = fontScale > 1.4f

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showGoalSuggestion by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // The chosen file survives rotation as a string; the confirm dialog then re-opens.
    var pendingImportUri by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionEpoch by remember { mutableIntStateOf(0) }
    // Notifications can be revoked in system Settings while this screen stays in the back stack.
    LifecycleResumeEffect(Unit) {
        permissionEpoch++
        onPauseOrDispose { }
    }

    val onExportResult: (Result<Int>) -> Unit = { result ->
        onMessage(
            result.fold(
                onSuccess = { context.getString(R.string.settings_export_success, it) },
                onFailure = { context.getString(R.string.settings_export_error) },
            ),
        )
    }
    val onImportResult: (Result<Int>) -> Unit = { result ->
        onMessage(
            result.fold(
                onSuccess = { context.getString(R.string.settings_import_success, it) },
                onFailure = { context.getString(R.string.settings_import_error) },
            ),
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context, uri, onExportResult)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        // Import replaces stored days, so confirm before touching the database (F-13).
        if (uri != null) pendingImportUri = uri.toString()
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
                GoalAdviceSection(
                    advice = goalAdvice,
                    onSuggest = { showGoalSuggestion = true },
                )
            }
        }

        SectionHeader(title = stringResource(R.string.settings_streak))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The whole row is the switch target: one TalkBack node and a 48dp+ touch area.
                        .toggleable(
                            value = restDayToday,
                            role = Role.Switch,
                        ) { checked ->
                            viewModel.setRestDayToday(checked)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_rest_day_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_rest_day_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = restDayToday,
                        onCheckedChange = null,
                    )
                }
                if (restDayToday) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = stringResource(R.string.settings_rest_day_hint),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        if (settings.units == Units.IMPERIAL) {
            // Imperial only affects distance/pace labels; body data stays cm/kg (honest copy).
            Text(
                text = stringResource(R.string.settings_units_body_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        // The whole row is one switch target: a 48dp+ touch area and one TalkBack
                        // node instead of a bare Switch floating beside an unlabelled description (F-19).
                        .toggleable(
                            value = settings.reminderEnabled,
                            role = Role.Switch,
                        ) { checked ->
                            if (checked && notificationPermission != null && !notificationsGranted) {
                                notificationLauncher.launch(notificationPermission)
                            } else {
                                viewModel.setReminderEnabled(checked)
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reminder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_reminder_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = null,
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
                BodyDataRow(
                    label = stringResource(R.string.settings_height),
                    valueText = stringResource(R.string.value_cm, settings.heightCm),
                    decreaseLabel = stringResource(R.string.cd_decrease),
                    increaseLabel = stringResource(R.string.cd_increase),
                    onDecrease = { viewModel.setHeight(settings.heightCm - HeightStep) },
                    onIncrease = { viewModel.setHeight(settings.heightCm + HeightStep) },
                    stacked = stackedRows,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BodyDataRow(
                    label = stringResource(R.string.settings_weight),
                    valueText = stringResource(R.string.value_kg, settings.weightKg),
                    decreaseLabel = stringResource(R.string.cd_decrease),
                    increaseLabel = stringResource(R.string.cd_increase),
                    onDecrease = { viewModel.setWeight(settings.weightKg - WeightStep) },
                    onIncrease = { viewModel.setWeight(settings.weightKg + WeightStep) },
                    stacked = stackedRows,
                )
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
        // Recreate with the new locale and re-create notification channels so their names switch too.
        val chooseLanguage: (AppLanguage) -> Unit = { language ->
            viewModel.syncLanguage(language)
            AppLocale.apply(context, language)
            Notifications.ensureChannels(AppLocale.wrap(context))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = AppLocale.current(context) == AppLanguage.EN,
                onClick = { chooseLanguage(AppLanguage.EN) },
                label = { Text(stringResource(R.string.settings_language_en)) },
            )
            FilterChip(
                selected = AppLocale.current(context) == AppLanguage.AR,
                onClick = { chooseLanguage(AppLanguage.AR) },
                label = { Text(stringResource(R.string.settings_language_ar)) },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_counting))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_counting_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SabeelSecondaryButton(
                    text = stringResource(R.string.settings_diagnostics),
                    onClick = onOpenDiagnostics,
                )
            }
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
                    onClick = {
                        importLauncher.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain"),
                        )
                    },
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
        if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
            SabeelSecondaryButton(
                text = stringResource(R.string.settings_privacy_web),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                        )
                    }
                },
            )
        }
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

    val suggestion = goalAdvice.suggestion
    if (showGoalSuggestion && suggestion != null) {
        AlertDialog(
            onDismissRequest = { showGoalSuggestion = false },
            title = { Text(stringResource(R.string.settings_goal_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.settings_goal_dialog_body,
                        stepsText(suggestion.medianSteps),
                        stepsText(suggestion.proposedGoal),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGoalSuggestion = false
                        viewModel.applySuggestedGoal(suggestion)
                    },
                ) {
                    Text(
                        text = stringResource(
                            R.string.settings_goal_dialog_apply,
                            stepsText(suggestion.proposedGoal),
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalSuggestion = false }) {
                    Text(
                        text = stringResource(
                            R.string.settings_goal_dialog_keep,
                            stepsText(settings.goalSteps),
                        ),
                    )
                }
            },
        )
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.settings_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_import_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val raw = pendingImportUri
                        pendingImportUri = null
                        if (raw != null) {
                            viewModel.importCsv(context, Uri.parse(raw), onImportResult)
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_import_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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

/**
 * Goal guidance under the stepper. A suggestion is only offered (never applied) after about two
 * weeks of recorded days; otherwise the section states what is missing or that the routine already
 * matches the goal - both are honest outcomes, so neither pretends a suggestion exists.
 */
@Composable
private fun GoalAdviceSection(
    advice: GoalAdvisor.Advice,
    onSuggest: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    val suggestion = advice.suggestion
    when {
        suggestion != null -> {
            SabeelSecondaryButton(
                text = stringResource(R.string.settings_goal_advice_button),
                onClick = onSuggest,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_goal_advice_hint, suggestion.recordedDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        advice.recordedDays < GoalAdvisor.MIN_RECORDED_DAYS -> Text(
            text = stringResource(R.string.settings_goal_advice_waiting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> Text(
            text = stringResource(
                R.string.settings_goal_advice_close,
                stepsText(advice.medianSteps ?: 0),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Height/weight row: label beside the stepper, stacked once the text is large (F-10). */
@Composable
private fun BodyDataRow(
    label: String,
    valueText: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    stacked: Boolean,
) {
    if (stacked) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Stepper(
                valueText = valueText,
                decreaseLabel = decreaseLabel,
                increaseLabel = increaseLabel,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Stepper(
                valueText = valueText,
                decreaseLabel = decreaseLabel,
                increaseLabel = increaseLabel,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}