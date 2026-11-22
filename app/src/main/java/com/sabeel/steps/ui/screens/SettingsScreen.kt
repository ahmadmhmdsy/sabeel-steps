package com.sabeel.steps.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.health.connect.client.PermissionController
import com.sabeel.steps.platform.HealthConnect
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.StepSensors
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

private val RowHorizontalPadding = 16.dp
private val RowVerticalPadding = 14.dp

/**
 * Settings, grouped into seven cards plus a danger zone (Q1-b design + the Phase 1 counting
 * mirror).
 *
 * Every group is a [SectionHeader] over one [SettingsCard]; every row inside a card follows the
 * same pattern: title, an optional subtitle, and exactly one trailing control (stepper, switch, or
 * value + chevron). Above fontScale 1.4 the trailing control moves below the text so nothing
 * truncates. Destructive deletion lives in its own error-container card instead of the primary
 * button slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onDeletedAll: () -> Unit,
    onMessage: (String) -> Unit,
    /** Counting switch value; null reads the SettingsViewModel's own mirror. */
    countingEnabled: Boolean? = null,
    /** Counting toggle action; null toggles through the SettingsViewModel's controller. */
    onToggleCounting: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val restDays = viewModel.restDays.collectAsStateWithLifecycle().value
    val goalAdvice = viewModel.goalAdvice.collectAsStateWithLifecycle().value
    val viewModelCountingEnabled by viewModel.countingEnabled.collectAsStateWithLifecycle()
    // Null-safe fallbacks: the app shell may pass the switch value and action, but the screen still
    // works on its own (existing call sites keep compiling and stay correct).
    val countingOn = countingEnabled ?: viewModelCountingEnabled
    val externalCountingToggle = onToggleCounting
    val restDayToday = DateKey.today() in restDays
    val stackedRows = LocalConfiguration.current.fontScale > 1.4f
    val language = AppLocale.current(context)
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showGoalSuggestion by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showUnitsDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showGenderDialog by rememberSaveable { mutableStateOf(false) }
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
        // Import merges into the stored days, so confirm before touching the database (F-13).
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

    // Health Connect opt-in only exists where it is the one honest way to get steps: the phone has
    // no hardware step sensor and the provider is installed. The system dialog owns the grant; the
    // returned set is re-checked on resume like every other permission.
    val healthConnectAvailability = remember(permissionEpoch) { HealthConnect.availability(context) }
    val noMeasuredStepSensor = remember(permissionEpoch) {
        StepSensors.availability(context).noMeasuredStepSensor
    }
    val healthConnectLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { permissionEpoch++ }

    // Recreate with the new locale and re-create notification channels so their names switch too.
    val chooseLanguage: (AppLanguage) -> Unit = { chosen ->
        viewModel.syncLanguage(chosen)
        AppLocale.apply(context, chosen)
        Notifications.ensureChannels(AppLocale.wrap(context))
    }

    ScreenScaffold(title = stringResource(R.string.settings_title)) {

        SectionHeader(title = stringResource(R.string.settings_goal))
        SettingsCard {
            SettingsStepperRow(
                title = stringResource(R.string.settings_goal_steps_per_day),
                // The row title already says "Steps per day", so the control shows the number only;
                // at 360dp with the serif value the "%1$s steps" form wrapped onto two lines.
                valueText = stepsText(settings.goalSteps),
                decreaseLabel = stringResource(R.string.cd_decrease),
                increaseLabel = stringResource(R.string.cd_increase),
                onDecrease = { viewModel.setGoal(settings.goalSteps - GoalStep) },
                onIncrease = { viewModel.setGoal(settings.goalSteps + GoalStep) },
                stacked = stackedRows,
            )
            SettingsDivider()
            GoalAdviceRow(
                advice = goalAdvice,
                stacked = stackedRows,
                onOpenSuggestion = { showGoalSuggestion = true },
            )
        }

        // The Settings mirror of the Home ring switch: one persisted intent, two controls. The
        // subtitle is the honest limit, not a promise that Android can never stop counting.
        SectionHeader(title = stringResource(R.string.settings_section_counting))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_counting_title),
                subtitle = stringResource(R.string.settings_counting_switch_desc),
                checked = countingOn,
                onCheckedChange = { checked ->
                    val external = externalCountingToggle
                    if (external != null) {
                        external()
                    } else {
                        viewModel.setCountingEnabled(checked)
                    }
                },
                stacked = stackedRows,
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_tracking))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_rest_day_title),
                subtitle = stringResource(R.string.settings_rest_day_desc),
                hint = if (restDayToday) stringResource(R.string.settings_rest_day_hint) else null,
                checked = restDayToday,
                onCheckedChange = { checked -> viewModel.setRestDayToday(checked) },
                stacked = stackedRows,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_reminder),
                subtitle = stringResource(R.string.settings_reminder_desc),
                checked = settings.reminderEnabled,
                onCheckedChange = { checked ->
                    if (checked && notificationPermission != null && !notificationsGranted) {
                        notificationLauncher.launch(notificationPermission)
                    } else {
                        viewModel.setReminderEnabled(checked)
                    }
                },
                stacked = stackedRows,
            )
            if (settings.reminderEnabled) {
                SettingsDivider()
                SettingsClickableRow(
                    title = stringResource(R.string.settings_reminder_time),
                    value = timeOfDayText(settings.reminderMinutes),
                    onClick = { showTimePicker = true },
                    stacked = stackedRows,
                )
            }
            SettingsDivider()
            SettingsClickableRow(
                title = stringResource(R.string.settings_diagnostics),
                subtitle = stringResource(R.string.settings_counting_desc),
                onClick = onOpenDiagnostics,
                stacked = stackedRows,
            )
        }

        if (noMeasuredStepSensor && healthConnectAvailability != HealthConnect.Availability.UNAVAILABLE) {
            SectionHeader(title = stringResource(R.string.settings_health_connect_title))
            SettingsCard {
                if (healthConnectAvailability == HealthConnect.Availability.READY) {
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_health_connect_connected),
                        subtitle = stringResource(R.string.settings_health_connect_note),
                        value = stringResource(R.string.settings_health_connect_manage),
                        onClick = {
                            runCatching { context.startActivity(HealthConnect.settingsIntent(context)) }
                                .onFailure {
                                    onMessage(context.getString(R.string.diagnostics_error_no_settings))
                                }
                        },
                        stacked = stackedRows,
                    )
                } else {
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_health_connect_row_title),
                        subtitle = stringResource(R.string.settings_health_connect_desc),
                        value = stringResource(R.string.settings_health_connect_connect),
                        onClick = {
                            healthConnectLauncher.launch(HealthConnect.requiredPermissions(context))
                        },
                        stacked = stackedRows,
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.settings_section_units_language))
        SettingsCard {
            SettingsClickableRow(
                title = stringResource(R.string.settings_units),
                subtitle = if (settings.units == Units.IMPERIAL) {
                    // Imperial only affects distance/pace labels; body data stays cm/kg (honest copy).
                    stringResource(R.string.settings_units_body_note)
                } else {
                    null
                },
                value = stringResource(
                    if (settings.units == Units.METRIC) {
                        R.string.settings_units_metric
                    } else {
                        R.string.settings_units_imperial
                    },
                ),
                onClick = { showUnitsDialog = true },
                stacked = stackedRows,
            )
            SettingsDivider()
            SettingsClickableRow(
                title = stringResource(R.string.settings_language),
                value = stringResource(
                    if (language == AppLanguage.AR) {
                        R.string.settings_language_ar
                    } else {
                        R.string.settings_language_en
                    },
                ),
                onClick = { showLanguageDialog = true },
                stacked = stackedRows,
            )
        }

        SectionHeader(title = stringResource(R.string.settings_body_data))
        SettingsCard {
            SettingsStepperRow(
                title = stringResource(R.string.settings_height),
                valueText = stringResource(R.string.value_cm, settings.heightCm),
                decreaseLabel = stringResource(R.string.cd_decrease),
                increaseLabel = stringResource(R.string.cd_increase),
                onDecrease = { viewModel.setHeight(settings.heightCm - HeightStep) },
                onIncrease = { viewModel.setHeight(settings.heightCm + HeightStep) },
                stacked = stackedRows,
            )
            SettingsDivider()
            SettingsStepperRow(
                title = stringResource(R.string.settings_weight),
                valueText = stringResource(R.string.value_kg, settings.weightKg),
                decreaseLabel = stringResource(R.string.cd_decrease),
                increaseLabel = stringResource(R.string.cd_increase),
                onDecrease = { viewModel.setWeight(settings.weightKg - WeightStep) },
                onIncrease = { viewModel.setWeight(settings.weightKg + WeightStep) },
                stacked = stackedRows,
            )
            SettingsDivider()
            SettingsClickableRow(
                title = stringResource(R.string.settings_gender),
                value = stringResource(
                    when (settings.gender) {
                        Gender.UNSPECIFIED -> R.string.gender_unspecified
                        Gender.MALE -> R.string.gender_male
                        Gender.FEMALE -> R.string.gender_female
                    },
                ),
                onClick = { showGenderDialog = true },
                stacked = stackedRows,
            )
        }

        SectionHeader(title = stringResource(R.string.settings_data))
        SettingsCard {
            val exportFileName = stringResource(R.string.settings_export_file_name)
            SettingsClickableRow(
                title = stringResource(R.string.settings_export_csv),
                subtitle = stringResource(R.string.settings_export_desc),
                onClick = { exportLauncher.launch(exportFileName) },
                stacked = stackedRows,
            )
            SettingsDivider()
            SettingsClickableRow(
                title = stringResource(R.string.settings_import_csv),
                subtitle = stringResource(R.string.settings_import_desc),
                onClick = {
                    importLauncher.launch(
                        arrayOf("text/csv", "text/comma-separated-values", "text/plain"),
                    )
                },
                stacked = stackedRows,
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_app))
        SettingsCard {
            SettingsClickableRow(
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_desc),
                onClick = onOpenPrivacy,
                stacked = stackedRows,
            )
            if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                SettingsDivider()
                SettingsClickableRow(
                    title = stringResource(R.string.settings_privacy_web),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                            )
                        }
                    },
                    stacked = stackedRows,
                )
            }
            SettingsDivider()
            SettingsClickableRow(
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(R.string.about_version, versionName),
                onClick = onOpenAbout,
                stacked = stackedRows,
            )
        }

        DangerZoneCard(
            title = stringResource(R.string.settings_delete_all),
            subtitle = stringResource(R.string.settings_delete_all_desc),
            onClick = { showDeleteDialog = true },
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

    if (showUnitsDialog) {
        AlertDialog(
            onDismissRequest = { showUnitsDialog = false },
            title = { Text(stringResource(R.string.settings_units_choose_title)) },
            text = {
                Column {
                    ChoiceRow(
                        label = stringResource(R.string.settings_units_metric),
                        selected = settings.units == Units.METRIC,
                        onClick = {
                            showUnitsDialog = false
                            viewModel.setUnits(Units.METRIC)
                        },
                    )
                    ChoiceRow(
                        label = stringResource(R.string.settings_units_imperial),
                        selected = settings.units == Units.IMPERIAL,
                        onClick = {
                            showUnitsDialog = false
                            viewModel.setUnits(Units.IMPERIAL)
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showUnitsDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language_choose_title)) },
            text = {
                Column {
                    ChoiceRow(
                        label = stringResource(R.string.settings_language_en),
                        selected = language == AppLanguage.EN,
                        onClick = {
                            showLanguageDialog = false
                            chooseLanguage(AppLanguage.EN)
                        },
                    )
                    ChoiceRow(
                        label = stringResource(R.string.settings_language_ar),
                        selected = language == AppLanguage.AR,
                        onClick = {
                            showLanguageDialog = false
                            chooseLanguage(AppLanguage.AR)
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text(stringResource(R.string.settings_gender)) },
            text = {
                Column {
                    ChoiceRow(
                        label = stringResource(R.string.gender_unspecified),
                        selected = settings.gender == Gender.UNSPECIFIED,
                        onClick = {
                            showGenderDialog = false
                            viewModel.setGender(Gender.UNSPECIFIED)
                        },
                    )
                    ChoiceRow(
                        label = stringResource(R.string.gender_male),
                        selected = settings.gender == Gender.MALE,
                        onClick = {
                            showGenderDialog = false
                            viewModel.setGender(Gender.MALE)
                        },
                    )
                    ChoiceRow(
                        label = stringResource(R.string.gender_female),
                        selected = settings.gender == Gender.FEMALE,
                        onClick = {
                            showGenderDialog = false
                            viewModel.setGender(Gender.FEMALE)
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGenderDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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
 * Dynamic goal advice under the stepper: a tappable suggestion when the advisor has one, otherwise
 * an honest explanation of what is missing (or that the routine already matches the goal).
 */
@Composable
private fun GoalAdviceRow(
    advice: GoalAdvisor.Advice,
    stacked: Boolean,
    onOpenSuggestion: () -> Unit,
) {
    val suggestion = advice.suggestion
    val subtitle = when {
        suggestion != null -> stringResource(
            R.string.settings_goal_advice_hint,
            suggestion.recordedDays,
        )

        advice.recordedDays < GoalAdvisor.MIN_RECORDED_DAYS -> stringResource(
            R.string.settings_goal_advice_waiting,
        )

        else -> stringResource(
            R.string.settings_goal_advice_close,
            stepsText(advice.medianSteps ?: 0),
        )
    }
    SettingsClickableRow(
        title = stringResource(R.string.settings_goal_advice_button),
        subtitle = subtitle,
        onClick = if (suggestion != null) onOpenSuggestion else null,
        stacked = stacked,
    )
}

/** Card holding a group of rows; the plain surfaceContainerLow card from the design. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Title + optional subtitle (+ an optional small enabled hint), the text part of every row. */
@Composable
private fun SettingsTextBlock(title: String, subtitle: String? = null, hint: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hint != null) {
            Text(
                text = hint,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A row that opens something: title + optional subtitle, and the trailing "value + chevron" (or
 * just a chevron). Rows without an action are informational and stay non-clickable.
 */
@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    stacked: Boolean,
) {
    val action = onClick
    val clickableModifier = if (action != null) {
        Modifier.clickable(role = Role.Button, onClick = action)
    } else {
        Modifier
    }
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(clickableModifier)
        .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding)
    if (stacked) {
        Column(modifier = rowModifier) {
            SettingsTextBlock(title = title, subtitle = subtitle)
            if (action != null) {
                Spacer(Modifier.height(10.dp))
                // Stacked rows put the single control on the trailing edge, matching the
                // unstacked layout instead of leaving a lone chevron under the text.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TrailingValue(value = value)
                }
            }
        }
    } else {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                SettingsTextBlock(title = title, subtitle = subtitle)
            }
            if (action != null) {
                Spacer(Modifier.width(12.dp))
                TrailingValue(value = value)
            }
        }
    }
}

/** Whole-row switch target: one TalkBack node and a 48dp+ touch area (F-19). */
@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    hint: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    stacked: Boolean,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
        .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding)
    if (stacked) {
        Column(modifier = rowModifier) {
            SettingsTextBlock(title = title, subtitle = subtitle, hint = hint)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Switch(checked = checked, onCheckedChange = null)
            }
        }
    } else {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                SettingsTextBlock(title = title, subtitle = subtitle, hint = hint)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/** Title beside a stepper; stacked once the text is large (F-10). */
@Composable
private fun SettingsStepperRow(
    title: String,
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
                .padding(horizontal = RowHorizontalPadding, vertical = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
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
                .padding(horizontal = RowHorizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
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

/** Trailing "value + chevron" control. The chevron is decorative and flips in RTL. */
@Composable
private fun TrailingValue(value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
        }
        Chevron(tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Chevron(tint: Color) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp),
    )
}

/** Read-only choice inside a chooser dialog; the whole row is the radio target. */
@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Deletion moved out of the primary slot into an error-container card (Q1-b design). */
@Composable
private fun DangerZoneCard(title: String, subtitle: String, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val contentColor = MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            Chevron(tint = contentColor)
        }
    }
}