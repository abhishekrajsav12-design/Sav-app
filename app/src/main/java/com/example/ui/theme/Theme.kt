package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SavLightColorScheme = lightColorScheme(
    primary = SavNavyPrimary,
    onPrimary = SavPureWhite,
    primaryContainer = SavNavySoft,
    onPrimaryContainer = SavNavyDeep,
    secondary = SavGoldPrimary,
    onSecondary = SavNavyDeep,
    secondaryContainer = SavGoldSoft,
    onSecondaryContainer = SavGoldDark,
    tertiary = SavNavyElevated,
    onTertiary = SavPureWhite,
    tertiaryContainer = SavSurfaceVariantLight,
    onTertiaryContainer = SavNavyPrimary,
    background = SavIvoryBackground,
    onBackground = SavInkDark,
    surface = SavPureWhite,
    onSurface = SavInkDark,
    surfaceVariant = SavSurfaceVariantLight,
    onSurfaceVariant = SavInkMuted,
    outline = SavOutlineLight,
    outlineVariant = Color(0xFFE4E9F2),
    error = SavCrimson,
    onError = SavPureWhite,
    errorContainer = SavCrimsonContainer,
    onErrorContainer = SavCrimson
)

private val SavDarkColorScheme = darkColorScheme(
    primary = SavGoldBright,
    onPrimary = SavNavyDeep,
    primaryContainer = SavNavyElevated,
    onPrimaryContainer = SavPureWhite,
    secondary = SavGoldPrimary,
    onSecondary = SavNavyDeep,
    secondaryContainer = Color(0xFF332911),
    onSecondaryContainer = SavGoldSoft,
    tertiary = SavNavySoft,
    onTertiary = SavNavyDeep,
    tertiaryContainer = SavDarkSurfaceElevated,
    onTertiaryContainer = SavDarkInk,
    background = SavDarkBackground,
    onBackground = SavDarkInk,
    surface = SavDarkSurface,
    onSurface = SavDarkInk,
    surfaceVariant = SavDarkSurfaceElevated,
    onSurfaceVariant = SavDarkInkMuted,
    outline = SavDarkOutline,
    outlineVariant = Color(0xFF1E2D47),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val SavShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SavFamilyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SavDarkColorScheme else SavLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SavTypography,
        shapes = SavShapes,
        content = content
    )
}
