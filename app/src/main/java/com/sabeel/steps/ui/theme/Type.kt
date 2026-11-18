package com.sabeel.steps.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sabeel.steps.R

/**
 * Design intent "authored accents": one bundled serif for display, headlines and the screen
 * title, while body copy and controls stay on the system sans. Fraunces carries Latin only, so
 * Arabic headlines fall back to the platform Naskh face rather than shipping a second font in v1.
 */
private val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, weight = FontWeight.SemiBold),
)

private val Base = Typography()

val SabeelTypography: Typography = Typography(
    displaySmall = Base.displaySmall.copy(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = Base.headlineLarge.copy(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = Base.titleLarge.copy(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
