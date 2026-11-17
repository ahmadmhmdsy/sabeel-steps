package com.sabeel.steps.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * "Desert Dawn" palette from docs/product/spec.md §7. Original colours, no borrowed assets.
 * Everything the UI paints comes from here or from MaterialTheme.colorScheme.
 */

// Brand constants
val DesertTeal = Color(0xFF0F6B5C)
val DesertMint = Color(0xFF4FD1B5)
val DesertClay = Color(0xFFB4653A)
val DesertSand = Color(0xFFD9A441)
val DesertBackgroundLight = Color(0xFFFAF7F2)
val DesertBackgroundDark = Color(0xFF101817)
val DesertSurfaceLight = Color(0xFFFFFFFF)
val DesertSurfaceDark = Color(0xFF18211F)
val DesertError = Color(0xFFB3261E)

/**
 * Ring gradient: teal (light) or mint (dark) towards a soft dawn tint.
 *
 * Light-theme non-text contrast (WCAG 2.2 SC 1.4.11, >= 3:1 on the #FAF7F2 background): the old
 * mint end #4FD1B5 measured only 1.77:1 and vanished for low-vision users, so the light gradient
 * ends at #1F8B72 (3.93:1). The dark gradient is unchanged (9.5:1 on #101817).
 */
fun ringGradientColors(dark: Boolean): List<Color> =
    if (dark) listOf(Color(0xFF4FD1B5), Color(0xFFA9F0DF))
    else listOf(Color(0xFF0F6B5C), Color(0xFF1F8B72))

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F6B5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4EDE7),
    onPrimaryContainer = Color(0xFF00201A),
    inversePrimary = Color(0xFFA8DAD1),
    secondary = Color(0xFFB4653A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBE0D1),
    onSecondaryContainer = Color(0xFF33160A),
    // Goal line / warning graphics. The old sand #D9A441 measured 2.10:1 on the background;
    // WCAG 2.2 SC 1.4.11 needs >= 3:1 for meaningful graphics, so the light goal line is #8C6212 (5.08:1).
    tertiary = Color(0xFF8C6212),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBEBCB),
    onTertiaryContainer = Color(0xFF2A1E00),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFF0EBE2),
    onSurfaceVariant = Color(0xFF494640),
    surfaceTint = Color(0xFF0F6B5C),
    inverseSurface = Color(0xFF2F3130),
    inverseOnSurface = Color(0xFFF1F1F0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF79746C),
    outlineVariant = Color(0xFFD9D3C9),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE7E2DA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F3EC),
    surfaceContainer = Color(0xFFEFE9E1),
    surfaceContainerHigh = Color(0xFFE7E1D8),
    surfaceContainerHighest = Color(0xFFDFD9D0),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD1B5),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF0C4F44),
    onPrimaryContainer = Color(0xFFB7EEE2),
    inversePrimary = Color(0xFF0F6B5C),
    secondary = Color(0xFFE4A07B),
    onSecondary = Color(0xFF4A200C),
    secondaryContainer = Color(0xFF6B3A22),
    onSecondaryContainer = Color(0xFFFBDCCB),
    tertiary = Color(0xFFE8C56A),
    onTertiary = Color(0xFF3A2E00),
    tertiaryContainer = Color(0xFF544500),
    onTertiaryContainer = Color(0xFFFFE59B),
    background = Color(0xFF101817),
    onBackground = Color(0xFFDFE5E2),
    surface = Color(0xFF18211F),
    onSurface = Color(0xFFDFE5E2),
    surfaceVariant = Color(0xFF23302D),
    onSurfaceVariant = Color(0xFFC1CCC7),
    surfaceTint = Color(0xFF4FD1B5),
    inverseSurface = Color(0xFFDFE5E2),
    inverseOnSurface = Color(0xFF2C3331),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8B9792),
    outlineVariant = Color(0xFF3B4844),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF26312E),
    surfaceDim = Color(0xFF101817),
    surfaceContainerLowest = Color(0xFF0B1211),
    surfaceContainerLow = Color(0xFF141D1B),
    surfaceContainer = Color(0xFF18211F),
    surfaceContainerHigh = Color(0xFF222C29),
    surfaceContainerHighest = Color(0xFF2D3734),
)