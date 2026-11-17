package com.sabeel.steps.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.sabeel.steps.ui.components.PermissionCard
import com.sabeel.steps.ui.components.SabeelPrimaryButton
import com.sabeel.steps.ui.components.SabeelSecondaryButton
import com.sabeel.steps.ui.components.Stepper
import com.sabeel.steps.ui.format.stepsText
import com.sabeel.steps.ui.i18n.AppLocale
import com.sabeel.steps.ui.viewmodel.OnboardingViewModel

private const val PageCount = 4

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(app))
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val settings = ui.settings

    var page by rememberSaveable { mutableIntStateOf(0) }
    var permissionEpoch by remember { mutableIntStateOf(0) }
    val activityPermission = Permissions.activityRecognitionPermission()
    val notificationPermission = Permissions.notificationPermission()
    val activityGranted = remember(permissionEpoch) { Permissions.hasActivityRecognition(context) }
    val notificationsGranted = remember(permissionEpoch) { Permissions.hasNotifications(context) }
    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_step_indicator, page + 1, PageCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (page) {
                0 -> WelcomePage(
                    currentLanguage = AppLocale.current(context),
                    onLanguageSelected = { language ->
                        viewModel.setLanguage(language)
                        AppLocale.apply(context, language)
                    },
                )

                1 -> GoalPage(
                    goal = settings.goalSteps,
                    onGoalChange = viewModel::setGoal,
                )

                2 -> BodyPage(
                    units = settings.units,
                    heightCm = settings.heightCm,
                    weightKg = settings.weightKg,
                    gender = settings.gender,
                    onUnitsChange = viewModel::setUnits,
                    onHeightChange = { height ->
                        viewModel.setBody(height, settings.weightKg, settings.gender)
                    },
                    onWeightChange = { weight ->
                        viewModel.setBody(settings.heightCm, weight, settings.gender)
                    },
                    onGenderChange = { gender ->
                        viewModel.setBody(settings.heightCm, settings.weightKg, gender)
                    },
                )

                else -> PermissionsPage(
                    activityGranted = activityGranted,
                    notificationsGranted = notificationsGranted,
                    onGrantActivity = {
                        activityPermission?.let { activityLauncher.launch(it) }
                    },
                    onGrantNotifications = {
                        notificationPermission?.let { notificationLauncher.launch(it) }
                    },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (page > 0) {
                SabeelSecondaryButton(
                    text = stringResource(R.string.action_back),
                    onClick = { page-- },
                    modifier = Modifier.weight(1f),
                )
            }
            if (page < PageCount - 1) {
                SabeelPrimaryButton(
                    text = stringResource(R.string.action_next),
                    onClick = { page++ },
                    modifier = Modifier.weight(1f),
                )
            } else {
                SabeelPrimaryButton(
                    text = stringResource(R.string.onboarding_finish),
                    onClick = { viewModel.finish { onFinished() } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PageTitle(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WelcomePage(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    PageTitle(
        title = stringResource(R.string.onboarding_welcome_title),
        body = stringResource(R.string.onboarding_welcome_body),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.onboarding_privacy_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
    Text(
        text = stringResource(R.string.onboarding_language_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = currentLanguage == AppLanguage.EN,
            onClick = { onLanguageSelected(AppLanguage.EN) },
            label = { Text(stringResource(R.string.settings_language_en)) },
        )
        FilterChip(
            selected = currentLanguage == AppLanguage.AR,
            onClick = { onLanguageSelected(AppLanguage.AR) },
            label = { Text(stringResource(R.string.settings_language_ar)) },
        )
    }
}

@Composable
private fun GoalPage(goal: Int, onGoalChange: (Int) -> Unit) {
    PageTitle(
        title = stringResource(R.string.onboarding_goal_title),
        body = stringResource(R.string.onboarding_goal_body),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Stepper(
                valueText = stringResource(R.string.goal_value, stepsText(goal)),
                decreaseLabel = stringResource(R.string.cd_decrease),
                increaseLabel = stringResource(R.string.cd_increase),
                onDecrease = { onGoalChange(goal - 500) },
                onIncrease = { onGoalChange(goal + 500) },
            )
        }
    }
    Text(
        text = stringResource(R.string.onboarding_goal_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyPage(
    units: Units,
    heightCm: Int,
    weightKg: Int,
    gender: Gender,
    onUnitsChange: (Units) -> Unit,
    onHeightChange: (Int) -> Unit,
    onWeightChange: (Int) -> Unit,
    onGenderChange: (Gender) -> Unit,
) {
    PageTitle(
        title = stringResource(R.string.onboarding_units_title),
        body = stringResource(R.string.onboarding_units_body),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = units == Units.METRIC,
            onClick = { onUnitsChange(Units.METRIC) },
            label = { Text(stringResource(R.string.settings_units_metric)) },
        )
        FilterChip(
            selected = units == Units.IMPERIAL,
            onClick = { onUnitsChange(Units.IMPERIAL) },
            label = { Text(stringResource(R.string.settings_units_imperial)) },
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_height),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Stepper(
                valueText = stringResource(R.string.value_cm, heightCm),
                decreaseLabel = stringResource(R.string.cd_decrease),
                increaseLabel = stringResource(R.string.cd_increase),
                onDecrease = { onHeightChange(heightCm - 1) },
                onIncrease = { onHeightChange(heightCm + 1) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_weight),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Stepper(
                valueText = stringResource(R.string.value_kg, weightKg),
                decreaseLabel = stringResource(R.string.cd_decrease),
                increaseLabel = stringResource(R.string.cd_increase),
                onDecrease = { onWeightChange(weightKg - 1) },
                onIncrease = { onWeightChange(weightKg + 1) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_gender),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = gender == Gender.UNSPECIFIED,
                    onClick = { onGenderChange(Gender.UNSPECIFIED) },
                    label = { Text(stringResource(R.string.gender_unspecified)) },
                )
                FilterChip(
                    selected = gender == Gender.MALE,
                    onClick = { onGenderChange(Gender.MALE) },
                    label = { Text(stringResource(R.string.gender_male)) },
                )
                FilterChip(
                    selected = gender == Gender.FEMALE,
                    onClick = { onGenderChange(Gender.FEMALE) },
                    label = { Text(stringResource(R.string.gender_female)) },
                )
            }
        }
    }
    Text(
        text = stringResource(R.string.onboarding_body_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PermissionsPage(
    activityGranted: Boolean,
    notificationsGranted: Boolean,
    onGrantActivity: () -> Unit,
    onGrantNotifications: () -> Unit,
) {
    PageTitle(
        title = stringResource(R.string.onboarding_permissions_title),
        body = stringResource(R.string.onboarding_permissions_body),
    )
    PermissionCard(
        title = stringResource(R.string.permission_activity_title),
        body = stringResource(R.string.permission_activity_body),
        granted = activityGranted,
        grantedLabel = stringResource(R.string.permission_granted),
        grantLabel = stringResource(R.string.permission_grant),
        onGrant = onGrantActivity,
    )
    PermissionCard(
        title = stringResource(R.string.permission_notification_title),
        body = stringResource(R.string.permission_notification_body),
        granted = notificationsGranted,
        grantedLabel = stringResource(R.string.permission_granted),
        grantLabel = stringResource(R.string.permission_grant),
        onGrant = onGrantNotifications,
    )
    Text(
        text = stringResource(R.string.onboarding_permissions_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
