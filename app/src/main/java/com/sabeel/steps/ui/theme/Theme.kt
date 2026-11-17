package com.sabeel.steps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App theme. Dynamic colour is deliberately OFF: the app ships one original palette in light and
 * dark so the product looks the same on every phone (spec §7).
 */
@Composable
fun SabeelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = SabeelTypography,
        shapes = SabeelShapes,
        content = content,
    )
}
