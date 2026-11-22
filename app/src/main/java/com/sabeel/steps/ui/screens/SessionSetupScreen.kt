@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sabeel.steps.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.domain.session.Place
import com.sabeel.steps.domain.session.SessionGoal
import com.sabeel.steps.domain.session.SessionPreset
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.service.SessionConfig
import com.sabeel.steps.ui.components.SabeelPrimaryButton
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader
import com.sabeel.steps.ui.format.distanceText
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.viewmodel.PLACE_RADIUS_MAX_M
import com.sabeel.steps.ui.viewmodel.PLACE_RADIUS_MIN_M
import com.sabeel.steps.ui.viewmodel.WalkViewModel
import java.util.Locale

/** Routes for the custom-session flow. The NavHost lives in SabeelRoot; these names are the contract. */
object SessionRoutes {
    /** Setup screen: activity, optional location, goal, presets, start. */
    const val SETUP = "session/setup"

    /** Live session screen (replaces the old direct walk route). */
    const val LIVE = "session"
}

/**
 * Version 1.3 session setup (plan Phase 2, amendment A7): choose walk or run, whether location is
 * used (foreground only, no map tiles), a goal, the auto-stop choice and a saved preset.
 *
 * Nothing is guessed: the start button stays disabled until the goal is valid, a PLACE goal can only
 * pick a locally saved place, and a session never counts on its own - when counting is off, starting
 * first asks to turn it on (amendment A8).
 *
 * The draft is kept in rememberSaveable, so rotating the phone or coming back does not lose it.
 */
@Composable
fun SessionSetupScreen(
    onBack: () -> Unit,
    onSessionStarted: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: WalkViewModel = viewModel(factory = WalkViewModel.factory(app))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val countingEnabled by viewModel.countingEnabled.collectAsStateWithLifecycle()

    val units = settings.units

    var activityName by rememberSaveable { mutableStateOf(ActivityType.WALK.name) }
    var goalName by rememberSaveable { mutableStateOf(GoalType.NONE.name) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var useGps by rememberSaveable { mutableStateOf(true) }
    var autoStop by rememberSaveable { mutableStateOf(false) }
    var endPlaceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var presetsApplied by rememberSaveable { mutableStateOf(false) }
    var gpsDenied by rememberSaveable { mutableStateOf(false) }
    var startFailed by rememberSaveable { mutableStateOf(false) }
    var showCountingDialog by rememberSaveable { mutableStateOf(false) }
    var showPresetDialog by rememberSaveable { mutableStateOf(false) }
    var savedPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var showPlacesDialog by rememberSaveable { mutableStateOf(false) }
    var showAddPlaceDialog by rememberSaveable { mutableStateOf(false) }
    var placeToDelete by remember { mutableStateOf<Place?>(null) }
    var presetToDelete by remember { mutableStateOf<SessionPreset?>(null) }

    val activity = ActivityType.entries.firstOrNull { it.name == activityName } ?: ActivityType.WALK
    val goal = GoalType.entries.firstOrNull { it.name == goalName } ?: GoalType.NONE
    val goalValue = goalCanonicalValue(goal, goalText, units)
    val goalValid = when (goal) {
        GoalType.NONE -> true
        GoalType.PLACE -> places.any { it.id == endPlaceId }
        else -> goalValue != null && SessionGoal.isValidTarget(goal, goalValue)
    }

    var pendingConfig by remember { mutableStateOf<SessionConfig?>(null) }
    var pendingCountingConfig by remember { mutableStateOf<SessionConfig?>(null) }

    fun startNow(config: SessionConfig) {
        val started = viewModel.startSession(config)
        startFailed = !started
        if (started) onSessionStarted()
    }

    fun applyPreset(preset: SessionPreset) {
        activityName = preset.activityType.name
        goalName = preset.goalType.name
        goalText = goalTextFor(preset.goalType, preset.goalValue, units)
        useGps = preset.useGps
        autoStop = false
        endPlaceId = preset.endPlaceId
        gpsDenied = false
        startFailed = false
        savedPreset = null
    }

    // Explicit external Maps action (geo: VIEW intent); Sabeel itself has no map SDK and no network.
    fun openInMaps(place: Place): Boolean {
        val uri = Uri.parse(
            "geo:" + place.lat + "," + place.lon + "?q=" + place.lat + "," + place.lon +
                "(" + Uri.encode(place.label) + ")",
        )
        return runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
    }

    // A PLACE goal needs one of the saved places; select the first one as soon as any exists, so a
    // place added from the manager dialog immediately makes the goal startable.
    LaunchedEffect(places, goal) {
        if (goal == GoalType.PLACE && endPlaceId == null && places.isNotEmpty()) {
            endPlaceId = places.first().id
        }
    }

    // The most recently used preset pre-fills the form once, like "same as last time".
    LaunchedEffect(presets) {
        if (!presetsApplied && presets.isNotEmpty()) {
            presetsApplied = true
            applyPreset(presets.maxByOrNull { it.lastUsedAt } ?: presets.first())
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val config = pendingConfig
        pendingConfig = null
        if (config != null) {
            if (Permissions.hasLocation(context)) {
                startNow(config)
            } else {
                // Honest fallback: the session still measures steps, just without a route.
                gpsDenied = true
                useGps = false
            }
        }
    }

    fun startRequested(config: SessionConfig) {
        if (config.useGps && !Permissions.hasLocation(context) &&
            Permissions.locationPermissions().isNotEmpty()
        ) {
            pendingConfig = config
            locationLauncher.launch(Permissions.locationPermissions().toTypedArray())
        } else {
            startNow(config)
        }
    }

    fun requestStart() {
        if (!goalValid) return
        val config = buildSessionConfig(
            activity = activity,
            goal = goal,
            goalValue = goalValue ?: 0.0,
            useGps = useGps,
            autoStop = autoStop,
            endPlaceId = endPlaceId,
        )
        if (!countingEnabled) {
            // A session cannot silently count while the user turned counting off (A8).
            pendingCountingConfig = config
            showCountingDialog = true
        } else {
            startRequested(config)
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.session_setup_title),
        onBack = onBack,
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(title = stringResource(R.string.session_activity_label))
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(
                    label = stringResource(R.string.session_activity_walk),
                    selected = activity == ActivityType.WALK,
                    onClick = {
                        activityName = ActivityType.WALK.name
                        startFailed = false
                    },
                )
                ChoiceChip(
                    label = stringResource(R.string.session_activity_run),
                    selected = activity == ActivityType.RUN,
                    onClick = {
                        activityName = ActivityType.RUN.name
                        startFailed = false
                    },
                )
            }

            SettingSwitchRow(
                title = stringResource(R.string.session_gps_label),
                subtitle = stringResource(R.string.session_gps_desc),
                checked = useGps,
                onCheckedChange = {
                    useGps = it
                    gpsDenied = false
                },
            )
            if (gpsDenied) {
                Text(
                    text = stringResource(R.string.session_gps_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            SectionHeader(title = stringResource(R.string.session_goal_label))
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GoalType.entries.forEach { type ->
                    ChoiceChip(
                        label = goalChipLabel(type),
                        selected = goal == type,
                        onClick = {
                            goalName = type.name
                            goalText = if (type == GoalType.NONE || type == GoalType.PLACE) "" else goalText
                            autoStop = if (type == GoalType.NONE) false else autoStop
                            endPlaceId = if (type == GoalType.PLACE) {
                                endPlaceId ?: places.firstOrNull()?.id
                            } else {
                                null
                            }
                            startFailed = false
                        },
                    )
                }
            }

            when (goal) {
                GoalType.NONE -> Unit

                GoalType.TIME, GoalType.STEPS, GoalType.DISTANCE -> {
                    val unitLabel = stringResource(goalUnitLabel(goal, units))
                    val helper = stringResource(goalHelperText(goal))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = sanitizeGoalInput(goal, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(goalChipLabel(goal)) },
                        suffix = { Text(unitLabel) },
                        supportingText = {
                            Text(
                                text = if (goalText.isNotBlank() && !goalValid) {
                                    stringResource(R.string.session_goal_invalid)
                                } else {
                                    helper
                                },
                            )
                        },
                        isError = goalText.isNotBlank() && !goalValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    GoalQuickValues(
                        goal = goal,
                        units = units,
                        selectedText = goalText,
                        onPick = { goalText = it },
                    )
                }

                GoalType.PLACE -> {
                    Text(
                        text = stringResource(R.string.session_goal_helper_place),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (places.isEmpty()) {
                        Text(
                            text = stringResource(R.string.session_place_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.session_place_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FlowRow(
                            modifier = Modifier.selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            places.forEach { place ->
                                ChoiceChip(
                                    label = place.label,
                                    selected = endPlaceId == place.id,
                                    onClick = { endPlaceId = place.id },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showPlacesDialog = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.session_places_manage))
                    }
                }
            }

            if (goal != GoalType.NONE) {
                SettingSwitchRow(
                    title = stringResource(R.string.session_auto_stop_label),
                    subtitle = stringResource(R.string.session_auto_stop_desc),
                    checked = autoStop,
                    onCheckedChange = { autoStop = it },
                )
            }

            SectionHeader(title = stringResource(R.string.session_presets_label))
            if (presets.isEmpty()) {
                Text(
                    text = stringResource(R.string.session_presets_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.sortedByDescending { it.lastUsedAt }.forEach { preset ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ActionChip(
                                label = preset.name,
                                onClick = { applyPreset(preset) },
                            )
                            IconButton(onClick = { presetToDelete = preset }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(
                                        R.string.session_preset_delete,
                                        preset.name,
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            SabeelSecondaryButton(
                text = stringResource(R.string.session_preset_save),
                onClick = { showPresetDialog = true },
                enabled = goalValid,
            )
            if (savedPreset != null) {
                Text(
                    text = stringResource(R.string.session_preset_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (startFailed) {
            Text(
                text = stringResource(R.string.session_start_blocked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        SabeelPrimaryButton(
            text = stringResource(R.string.session_start),
            onClick = { requestStart() },
            enabled = goalValid,
        )
        SabeelSecondaryButton(
            text = stringResource(R.string.action_cancel),
            onClick = onBack,
        )
    }

    if (showCountingDialog) {
        AlertDialog(
            onDismissRequest = {
                showCountingDialog = false
                pendingCountingConfig = null
            },
            title = { Text(stringResource(R.string.session_counting_off_title)) },
            text = { Text(stringResource(R.string.session_counting_off_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val config = pendingCountingConfig
                        showCountingDialog = false
                        pendingCountingConfig = null
                        if (config != null) {
                            viewModel.turnOnCounting()
                            startRequested(config)
                        }
                    },
                ) {
                    Text(stringResource(R.string.session_counting_off_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCountingDialog = false
                        pendingCountingConfig = null
                    },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showPresetDialog) {
        var presetName by remember { mutableStateOf("") }
        val configToSave = buildSessionConfig(
            activity = activity,
            goal = goal,
            goalValue = goalValue ?: 0.0,
            useGps = useGps,
            autoStop = autoStop,
            endPlaceId = endPlaceId,
        )
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(stringResource(R.string.session_preset_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.session_preset_name_label)) },
                        supportingText = {
                            if (presetName.isBlank()) {
                                Text(stringResource(R.string.session_preset_name_empty))
                            }
                        },
                        singleLine = true,
                    )
                    if (!goalValid) {
                        Text(
                            text = stringResource(R.string.session_goal_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = presetName.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.savePreset(trimmed, configToSave)
                            savedPreset = trimmed
                            showPresetDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank() && goalValid,
                ) {
                    Text(stringResource(R.string.session_preset_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showPlacesDialog) {
        PlacesManagerDialog(
            places = places,
            onAddPlace = { showAddPlaceDialog = true },
            onDeletePlace = { place -> placeToDelete = place },
            onOpenMaps = { place -> openInMaps(place) },
            onDismiss = { showPlacesDialog = false },
        )
    }

    if (showAddPlaceDialog) {
        AddPlaceDialog(
            loadLastKnown = { viewModel.lastKnownLocation() },
            onSave = { label, lat, lon, radius ->
                viewModel.savePlace(label, lat, lon, radius)
                showAddPlaceDialog = false
            },
            onDismiss = { showAddPlaceDialog = false },
        )
    }

    placeToDelete?.let { place ->
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text(stringResource(R.string.session_place_delete_title)) },
            text = { Text(stringResource(R.string.session_place_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlace(place.id)
                        if (endPlaceId == place.id) {
                            endPlaceId = places.firstOrNull { it.id != place.id }?.id
                        }
                        placeToDelete = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.session_place_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { placeToDelete = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.session_preset_delete_title)) },
            text = { Text(stringResource(R.string.session_preset_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePreset(preset.id)
                        if (savedPreset == preset.name) savedPreset = null
                        presetToDelete = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.session_preset_delete_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { presetToDelete = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Activity label used by the session screens and by History. */
@Composable
internal fun sessionActivityLabel(type: ActivityType): String = stringResource(
    when (type) {
        ActivityType.WALK -> R.string.session_activity_walk
        ActivityType.RUN -> R.string.session_activity_run
    },
)

/** The goal target in the user's unit: a duration, a step count, a distance, a place or "No goal". */
@Composable
internal fun sessionGoalTargetText(
    goalType: GoalType,
    goalValue: Double,
    units: Units,
    placeLabel: String? = null,
): String = when (goalType) {
    GoalType.NONE -> stringResource(R.string.session_goal_none)
    GoalType.TIME -> StepMath.formatDuration(goalValue.toLong())
    GoalType.STEPS -> stepsText(goalValue.toInt())
    GoalType.DISTANCE -> distanceText(goalValue, units)
    GoalType.PLACE -> placeLabel ?: stringResource(R.string.session_goal_place)
}

/** Two-line setting row with a switch; the whole row is one 48dp touch target and TalkBack node. */
@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Radio-style choice chip (activity, goal, place): exclusive, labelled, at least 48dp tall. */
@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = contentColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Action chip (preset name or quick goal value): a tap, not a persistent selection. */
@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Quick goal-value chips with the sensible presets from the plan (5/10/15/30/60 min, etc.). */
@Composable
private fun GoalQuickValues(
    goal: GoalType,
    units: Units,
    selectedText: String,
    onPick: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val picks = when (goal) {
            GoalType.TIME -> listOf(5, 10, 15, 30, 60)
            GoalType.STEPS -> listOf(1000, 2000, 5000, 10000)
            else -> listOf(1, 2, 5, 10)
        }
        picks.forEach { number ->
            ActionChip(
                label = quickValueLabel(goal, number, units),
                selected = selectedText.trim() == number.toString(),
                onClick = { onPick(number.toString()) },
            )
        }
    }
}

@Composable
private fun goalChipLabel(type: GoalType): String = stringResource(
    when (type) {
        GoalType.NONE -> R.string.session_goal_none
        GoalType.TIME -> R.string.session_goal_time
        GoalType.STEPS -> R.string.session_goal_steps
        GoalType.DISTANCE -> R.string.session_goal_distance
        GoalType.PLACE -> R.string.session_goal_place
    },
)

@Composable
private fun goalUnitLabel(goal: GoalType, units: Units): Int = when (goal) {
    GoalType.TIME -> R.string.session_goal_unit_time
    GoalType.STEPS -> R.string.session_goal_unit_steps
    GoalType.DISTANCE -> if (units == Units.METRIC) {
        R.string.session_goal_unit_distance
    } else {
        R.string.session_goal_unit_mile
    }
    else -> R.string.session_goal_unit_steps
}

private fun goalHelperText(goal: GoalType): Int = when (goal) {
    GoalType.TIME -> R.string.session_goal_helper_time
    GoalType.STEPS -> R.string.session_goal_helper_steps
    GoalType.DISTANCE -> R.string.session_goal_helper_distance
    else -> R.string.session_goal_helper_place
}

@Composable
private fun quickValueLabel(goal: GoalType, number: Int, units: Units): String = when (goal) {
    GoalType.TIME -> pluralStringResource(R.plurals.minutes_value, number, number)
    GoalType.STEPS -> stringResource(R.string.goal_value, stepsText(number))
    GoalType.DISTANCE -> distanceText(
        meters = if (units == Units.METRIC) number * 1000.0 else number * 1609.344,
        units = units,
    )
    else -> number.toString()
}

/** Keeps only what a number field can parse; steps are whole numbers. */
private fun sanitizeGoalInput(goal: GoalType, raw: String): String {
    val filtered = if (goal == GoalType.STEPS) {
        raw.filter { it.isDigit() }
    } else {
        raw.filter { it.isDigit() || it == '.' || it == ',' }
    }
    return filtered.take(9)
}

/**
 * The goal value in its canonical unit (seconds / steps / metres). The setup field shows minutes for
 * TIME, whole steps for STEPS and km (or miles for imperial settings) for DISTANCE.
 */
private fun goalCanonicalValue(goal: GoalType, text: String, units: Units): Double? {
    if (goal == GoalType.NONE || goal == GoalType.PLACE) return 0.0
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!value.isFinite() || value <= 0.0) return null
    return when (goal) {
        GoalType.TIME -> value * 60.0
        GoalType.STEPS -> value
        GoalType.DISTANCE -> if (units == Units.METRIC) value * 1000.0 else value * 1609.344
        else -> value
    }
}

/** Shows a canonical goal value back in the setup field's unit. */
private fun goalTextFor(type: GoalType, value: Double, units: Units): String = when (type) {
    GoalType.TIME -> formatCompact(value / 60.0)
    GoalType.STEPS -> value.toInt().toString()
    GoalType.DISTANCE -> formatCompact(
        if (units == Units.METRIC) value / 1000.0 else value / 1609.344,
    )
    else -> ""
}

private fun formatCompact(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return ""
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

/** Builds the service config from the current form; PLACE needs one of the saved places. */
private fun buildSessionConfig(
    activity: ActivityType,
    goal: GoalType,
    goalValue: Double,
    useGps: Boolean,
    autoStop: Boolean,
    endPlaceId: Long?,
): SessionConfig = SessionConfig(
    activityType = activity,
    goalType = goal,
    goalValue = if (goal == GoalType.NONE || goal == GoalType.PLACE) 0.0 else goalValue,
    useGps = useGps,
    startPlaceId = null,
    endPlaceId = if (goal == GoalType.PLACE) endPlaceId else null,
    presetId = null,
    autoStop = goal != GoalType.NONE && autoStop,
)

/**
 * Saved-places manager opened from the PLACE goal: list, explicit "Open in Maps" (external geo:
 * intent, no SDK, no network) and delete with confirmation. Adding opens [AddPlaceDialog].
 */
@Composable
private fun PlacesManagerDialog(
    places: List<Place>,
    onAddPlace: () -> Unit,
    onDeletePlace: (Place) -> Unit,
    onOpenMaps: (Place) -> Boolean,
    onDismiss: () -> Unit,
) {
    var mapsFailed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_places_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (places.isEmpty()) {
                    Text(
                        text = stringResource(R.string.session_place_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    places.forEachIndexed { index, place ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = place.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    R.string.session_place_radius_value,
                                    place.radiusM.toInt().toString(),
                                ) + " · " + coordinateInputText(place.lat) + ", " +
                                    coordinateInputText(place.lon),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { mapsFailed = !onOpenMaps(place) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text(stringResource(R.string.session_place_open_maps))
                                }
                                TextButton(
                                    onClick = { onDeletePlace(place) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text(stringResource(R.string.session_place_delete))
                                }
                            }
                        }
                    }
                }
                if (mapsFailed) {
                    Text(
                        text = stringResource(R.string.session_place_maps_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddPlace, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.session_place_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.history_walk_close))
            }
        },
    )
}

/**
 * Add-a-place dialog. Latitude/longitude are pre-filled from the last known fix only when a location
 * permission is already granted; otherwise the user types them. Radius defaults to 100 m and must
 * stay between 25 and 1000 m.
 */
@Composable
private fun AddPlaceDialog(
    loadLastKnown: suspend () -> Pair<Double, Double>?,
    onSave: (label: String, lat: Double, lon: Double, radiusM: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var latText by rememberSaveable { mutableStateOf("") }
    var lonText by rememberSaveable { mutableStateOf("") }
    var radiusText by rememberSaveable { mutableStateOf("100") }
    var prefillAttempted by rememberSaveable { mutableStateOf(false) }
    var prefillUnavailable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!prefillAttempted) {
            prefillAttempted = true
            val fix = loadLastKnown()
            if (fix != null) {
                latText = coordinateInputText(fix.first)
                lonText = coordinateInputText(fix.second)
            } else {
                prefillUnavailable = true
            }
        }
    }

    val lat = latText.trim().replace(',', '.').toDoubleOrNull()
    val lon = lonText.trim().replace(',', '.').toDoubleOrNull()
    val radius = radiusText.trim().replace(',', '.').toDoubleOrNull()
    val latitudeValid = lat != null && lat.isFinite() && lat in -90.0..90.0
    val longitudeValid = lon != null && lon.isFinite() && lon in -180.0..180.0
    val coordinatesValid = latitudeValid && longitudeValid
    val radiusValid = radius != null && radius.isFinite() &&
        radius >= PLACE_RADIUS_MIN_M && radius <= PLACE_RADIUS_MAX_M
    val nameValid = name.isNotBlank()
    val coordinatesTouched = latText.isNotBlank() || lonText.isNotBlank()

    val nameSupport: (@Composable () -> Unit)? = if (!nameValid && name.isNotEmpty()) {
        { Text(stringResource(R.string.session_place_name_required)) }
    } else {
        null
    }
    val coordinatesSupport: (@Composable () -> Unit)? = when {
        prefillUnavailable -> {
            { Text(stringResource(R.string.session_place_current_unavailable)) }
        }
        coordinatesTouched && !coordinatesValid -> {
            { Text(stringResource(R.string.session_place_invalid_coordinates)) }
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_place_add)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.session_place_name_label)) },
                    isError = !nameValid && name.isNotEmpty(),
                    supportingText = nameSupport,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = sanitizeCoordinateInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.session_place_lat_label)) },
                    isError = coordinatesTouched && !coordinatesValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = sanitizeCoordinateInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.session_place_lon_label)) },
                    isError = coordinatesTouched && !coordinatesValid,
                    supportingText = coordinatesSupport,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = sanitizeRadiusInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.session_place_radius_label)) },
                    isError = radiusText.isNotBlank() && !radiusValid,
                    supportingText = {
                        Text(
                            if (radiusText.isNotBlank() && !radiusValid) {
                                stringResource(R.string.session_place_invalid_radius)
                            } else {
                                stringResource(R.string.session_place_radius_helper)
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latValue = lat
                    val lonValue = lon
                    val radiusValue = radius
                    if (latValue != null && lonValue != null && radiusValue != null) {
                        onSave(name.trim(), latValue, lonValue, radiusValue)
                    }
                },
                enabled = nameValid && coordinatesValid && radiusValid,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.session_place_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Coordinates are edited as plain decimal text; a comma is accepted as the decimal separator. */
private fun coordinateInputText(value: Double): String {
    if (!value.isFinite()) return ""
    val formatted = String.format(Locale.US, "%.6f", value)
    return formatted.trimEnd('0').trimEnd('.').ifEmpty { "0" }
}

/** Keeps only characters a coordinate field can parse. */
private fun sanitizeCoordinateInput(raw: String): String =
    raw.filter { it.isDigit() || it == '-' || it == '.' || it == ',' }.take(12)

/** Keeps only characters a radius field can parse. */
private fun sanitizeRadiusInput(raw: String): String =
    raw.filter { it.isDigit() || it == '.' || it == ',' }.take(7)

