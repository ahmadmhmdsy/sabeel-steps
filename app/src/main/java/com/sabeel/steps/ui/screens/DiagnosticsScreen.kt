package com.sabeel.steps.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.domain.StepSource
import com.sabeel.steps.platform.BootAttempt
import com.sabeel.steps.platform.HealthConnect
import com.sabeel.steps.platform.OemBatteryGuide
import com.sabeel.steps.platform.Permissions
import com.sabeel.steps.platform.StepSensors
import com.sabeel.steps.platform.StepServiceStatus
import com.sabeel.steps.ui.components.CountingStatusCard
import com.sabeel.steps.ui.components.PermissionCard
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.format.timeText
import com.sabeel.steps.ui.viewmodel.DiagnosticsViewModel

/**
 * "Why is my count low?" - an evidence screen, not a coaching one.
 *
 * It shows what the app can actually observe (counting intent, permission, sensor feature, service
 * heartbeat, boot start attempts, today's stored row and its update time) and then the settings
 * phone makers use to restrict background work. It never asks for a new permission, never changes a
 * setting by itself and never claims steps can be recovered after the sensor was not read: honesty
 * over a bigger number. The two actions are deliberate: restart counting, or share this evidence.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: DiagnosticsViewModel = viewModel(factory = DiagnosticsViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()

    var permissionEpoch by remember { mutableIntStateOf(0) }
    // Permission and battery settings can change in system Settings while this screen is covered.
    LifecycleResumeEffect(Unit) {
        permissionEpoch++
        onPauseOrDispose { }
    }
    val hasActivityPermission = remember(permissionEpoch) { Permissions.hasActivityRecognition(context) }
    val hasSource = remember(permissionEpoch) {
        StepSensors.hasAny(context) || HealthConnect.isReady(context)
    }
    val activityPermission = Permissions.activityRecognitionPermission()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }

    var settingsError by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf(false) }
    val batterySettingsIntent = remember {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val canOpenBatterySettings = remember {
        runCatching { context.packageManager.resolveActivity(batterySettingsIntent, 0) != null }
            .getOrDefault(false)
    }
    val appSettingsIntent = remember {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val openSettings: (Intent) -> Unit = { intent ->
        settingsError = !runCatching { context.startActivity(intent) }.isSuccess
    }

    val guide = remember { OemBatteryGuide.guideFor(Build.MANUFACTURER) }
    // Counting health outranks source facts: "off" and "Android stopped it" are the answers the
    // owner needs before any estimate/sync label can explain a low number.
    val statusText = when {
        !hasActivityPermission || ui.serviceStatus == StepServiceStatus.NO_PERMISSION ->
            stringResource(R.string.diagnostics_status_no_permission)
        !hasSource || ui.serviceStatus == StepServiceStatus.NO_SENSOR ->
            stringResource(R.string.diagnostics_status_no_sensor)
        !ui.countingEnabled || ui.serviceStatus == StepServiceStatus.OFF_BY_USER ->
            stringResource(R.string.diagnostics_status_off_user)
        ui.serviceStatus == StepServiceStatus.STOPPED_BY_SYSTEM ->
            stringResource(R.string.diagnostics_status_stopped_system)
        ui.sourceStatus.source == StepSource.MOTION_ESTIMATE || (ui.today?.estimatedSteps ?: 0) > 0 ->
            stringResource(R.string.diagnostics_status_estimated)
        ui.sourceStatus.source == StepSource.HEALTH_CONNECT ->
            stringResource(R.string.diagnostics_status_synced)
        ui.serviceStatus == StepServiceStatus.COUNTING ->
            stringResource(R.string.diagnostics_status_counting)
        else -> stringResource(R.string.diagnostics_status_stopped)
    }
    val sourceLastEvent = ui.sourceStatus.lastEventAt
    val lastSensorText = if (sourceLastEvent > 0L) {
        timeText(sourceLastEvent)
    } else {
        stringResource(R.string.diagnostics_last_event_none)
    }
    val fallbacksText = if (ui.sourceStatus.switches > 0) {
        ui.sourceStatus.switches.toString()
    } else {
        stringResource(R.string.diagnostics_fallbacks_none)
    }
    val updatedAt = ui.today?.updatedAt ?: 0L
    val lastUpdateText = if (updatedAt > 0L) {
        timeText(updatedAt)
    } else {
        stringResource(R.string.diagnostics_last_update_none)
    }
    // The raw last heartbeat, deliberately not staleness-masked: "07:42" next to a status of
    // "stopped by Android" is the evidence that counting has not run since 07:42.
    val lastHeartbeatText = if (ui.lastHeartbeatAt > 0L) {
        timeText(ui.lastHeartbeatAt)
    } else {
        stringResource(R.string.diagnostics_heartbeat_none)
    }
    val storageText = if (ui.storageFaultAt > 0L) {
        stringResource(R.string.diagnostics_storage_failed, timeText(ui.storageFaultAt))
    } else {
        stringResource(R.string.diagnostics_storage_ok)
    }
    val notificationsText = stringResource(
        if (ui.notificationsGranted) R.string.diagnostics_permission_granted
        else R.string.diagnostics_permission_missing,
    )
    val batteryText = stringResource(
        if (ui.batteryOptimized) R.string.diagnostics_battery_restricted
        else R.string.diagnostics_battery_unrestricted,
    )
    val shareSubject = stringResource(R.string.diagnostics_share_subject)
    val shareTitle = stringResource(R.string.diagnostics_share_title)
    val shareStatusLabel = stringResource(R.string.diagnostics_status_label)
    val shareTodayLabel = stringResource(R.string.diagnostics_today_label)
    val shareLastUpdateLabel = stringResource(R.string.diagnostics_last_update_label)
    val shareHeartbeatLabel = stringResource(R.string.diagnostics_heartbeat_label)
    val shareSourceLabel = stringResource(R.string.diagnostics_source_label)
    val sharePermissionLabel = stringResource(R.string.diagnostics_permission_label)
    val shareNotificationsLabel = stringResource(R.string.diagnostics_notifications_label)
    val shareBatteryLabel = stringResource(R.string.diagnostics_battery_optimization_label)
    val shareBootLabel = stringResource(R.string.diagnostics_boot_section)
    val sharePermissionText = stringResource(
        if (hasActivityPermission) R.string.diagnostics_permission_granted
        else R.string.diagnostics_permission_missing,
    )
    val shareFooter = stringResource(R.string.diagnostics_share_footer)
    val shareErrorText = stringResource(R.string.diagnostics_share_error)
    val shareChooserTitle = stringResource(R.string.diagnostics_share_chooser)
    val todayText = stepsText(ui.today?.steps ?: 0)
    val sourceLabel = stringResource(StepSensors.labelRes(ui.sourceStatus.source))
    val shareText = buildString {
        appendLine(shareTitle)
        appendLine()
        appendLine("$shareStatusLabel: $statusText")
        appendLine("$shareTodayLabel: $todayText")
        appendLine("$shareLastUpdateLabel: $lastUpdateText")
        appendLine("$shareHeartbeatLabel: $lastHeartbeatText")
        appendLine("$shareSourceLabel: $sourceLabel")
        appendLine("$sharePermissionLabel: $sharePermissionText")
        appendLine("$shareNotificationsLabel: $notificationsText")
        appendLine("$shareBatteryLabel: $batteryText")
        appendLine("$shareBootLabel: ${ui.bootAttempts.size}")
        appendLine()
        append(shareFooter)
    }
    val shareAction: () -> Unit = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(send, shareChooserTitle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        shareError = !runCatching { context.startActivity(chooser) }.isSuccess
    }

    ScreenScaffold(title = stringResource(R.string.diagnostics_title), onBack = onBack) {
        Text(
            text = stringResource(R.string.diagnostics_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeader(title = stringResource(R.string.diagnostics_now_title))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column {
                InfoRow(
                    label = stringResource(R.string.diagnostics_status_label),
                    value = statusText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_today_label),
                    value = stringResource(
                        R.string.diagnostics_today_value,
                        todayText,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_last_update_label),
                    value = lastUpdateText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_heartbeat_label),
                    value = lastHeartbeatText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_source_label),
                    value = sourceLabel,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(
                        if (ui.sourceStatus.source == StepSource.HEALTH_CONNECT) {
                            R.string.diagnostics_last_sync_label
                        } else {
                            R.string.diagnostics_last_event_label
                        },
                    ),
                    value = lastSensorText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_fallbacks_label),
                    value = fallbacksText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_permission_label),
                    value = sharePermissionText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_notifications_label),
                    value = notificationsText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_battery_optimization_label),
                    value = batteryText,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(
                    label = stringResource(R.string.diagnostics_storage_label),
                    value = storageText,
                )
            }
        }

        // The honest limit of the switch lives here too, not only in Settings.
        Text(
            text = stringResource(R.string.settings_counting_switch_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!hasActivityPermission || ui.serviceStatus == StepServiceStatus.NO_PERMISSION) {
            PermissionCard(
                title = stringResource(R.string.permission_activity_title),
                body = stringResource(R.string.diagnostics_permission_body),
                granted = false,
                grantedLabel = stringResource(R.string.permission_granted),
                grantLabel = stringResource(R.string.permission_grant),
                onGrant = { activityPermission?.let { permissionLauncher.launch(it) } },
            )
            SabeelSecondaryButton(
                text = stringResource(R.string.diagnostics_open_settings),
                onClick = { openSettings(appSettingsIntent) },
            )
        }
        if (!hasSource || ui.serviceStatus == StepServiceStatus.NO_SENSOR) {
            CountingStatusCard(
                title = stringResource(R.string.home_status_no_sensor_title),
                body = stringResource(R.string.diagnostics_no_sensor_note),
            )
        }

        // Boot evidence: what happened when Android started the phone. This is the only place the
        // owner (or support) can see that a boot start was refused and why.
        SectionHeader(title = stringResource(R.string.diagnostics_boot_section))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            if (ui.bootAttempts.isEmpty()) {
                Text(
                    text = stringResource(R.string.diagnostics_boot_empty),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    // Platform bounds the list already; the UI bound keeps the card scannable.
                    ui.bootAttempts.take(10).forEachIndexed { index, attempt ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        BootAttemptBlock(attempt)
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.diagnostics_body_title))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DiagnosticsBodyText(stringResource(R.string.diagnostics_source_note))
                DiagnosticsBodyText(stringResource(R.string.diagnostics_body_sensor))
                DiagnosticsBodyText(stringResource(R.string.diagnostics_body_service))
                DiagnosticsBodyText(stringResource(R.string.diagnostics_body_battery))
                DiagnosticsBodyText(stringResource(R.string.diagnostics_body_rest))
            }
        }

        SectionHeader(title = stringResource(R.string.diagnostics_oem_title, guide.brandLabel))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.diagnostics_oem_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                guide.stepResIds.forEach { resId ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(resId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(4.dp))
                SabeelSecondaryButton(
                    text = stringResource(R.string.diagnostics_battery_button),
                    onClick = { openSettings(batterySettingsIntent) },
                    enabled = canOpenBatterySettings,
                )
                Spacer(Modifier.height(8.dp))
                SabeelSecondaryButton(
                    text = stringResource(R.string.diagnostics_open_settings),
                    onClick = { openSettings(appSettingsIntent) },
                )
            }
        }

        if (settingsError) {
            Text(
                text = stringResource(R.string.diagnostics_error_no_settings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // The two deliberate actions: re-arm counting (idempotent), or hand the evidence to support.
        SabeelSecondaryButton(
            text = stringResource(R.string.diagnostics_restart_counting),
            onClick = { viewModel.restartCounting() },
        )
        SabeelSecondaryButton(
            text = stringResource(R.string.diagnostics_share),
            onClick = shareAction,
        )
        if (shareError) {
            Text(
                text = shareErrorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.diagnostics_share_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.diagnostics_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** One recorded boot start attempt: time, action, StartResult name, the flags it saw, the health
 * check that ran after it, and the exception class when one was thrown. */
@Composable
private fun BootAttemptBlock(attempt: BootAttempt) {
    // ifBlank is inline, but keeping the composable call out of the lambda is simpler to verify.
    val resultText = if (attempt.result.isNotBlank()) {
        attempt.result
    } else {
        stringResource(R.string.diagnostics_unknown)
    }
    val aliveText = when (attempt.aliveAfterCheck) {
        true -> stringResource(R.string.diagnostics_yes)
        false -> stringResource(R.string.diagnostics_no)
        null -> stringResource(R.string.diagnostics_not_checked)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(
                R.string.diagnostics_boot_attempt_title,
                timeText(attempt.at),
                attempt.action,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.diagnostics_boot_result, resultText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.diagnostics_boot_flags,
                yesNo(attempt.onboarded),
                yesNo(attempt.hasActivityRecognition),
                yesNo(attempt.hasSensor),
                yesNo(attempt.healthConnectReady),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.diagnostics_boot_alive, aliveText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        attempt.exception?.let { exception ->
            Text(
                text = stringResource(R.string.diagnostics_boot_exception, exception),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(
    if (value) R.string.diagnostics_yes else R.string.diagnostics_no,
)

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DiagnosticsBodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}