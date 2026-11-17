package com.sabeel.steps.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sabeel.steps.R
import com.sabeel.steps.ui.components.ScreenScaffold
import com.sabeel.steps.ui.components.SectionHeader

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.privacy_title), onBack = onBack) {
        Text(
            text = stringResource(R.string.privacy_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        InfoCard(
            title = stringResource(R.string.privacy_offline_title),
            body = stringResource(R.string.privacy_offline_body),
        )
        InfoCard(
            title = stringResource(R.string.privacy_ads_title),
            body = stringResource(R.string.privacy_ads_body),
        )
        InfoCard(
            title = stringResource(R.string.privacy_account_title),
            body = stringResource(R.string.privacy_account_body),
        )
        InfoCard(
            title = stringResource(R.string.privacy_control_title),
            body = stringResource(R.string.privacy_control_body),
        )
        SectionHeader(title = stringResource(R.string.privacy_permissions_title))
        InfoCard(
            title = stringResource(R.string.permission_activity_title),
            body = stringResource(R.string.privacy_permission_activity_body),
        )
        InfoCard(
            title = stringResource(R.string.permission_notification_title),
            body = stringResource(R.string.privacy_permission_notification_body),
        )
        InfoCard(
            title = stringResource(R.string.permission_location_title),
            body = stringResource(R.string.privacy_permission_location_body),
        )
        Text(
            text = stringResource(R.string.privacy_permissions_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionHeader(title = stringResource(R.string.privacy_estimates_title))
        Text(
            text = stringResource(R.string.privacy_estimates_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
