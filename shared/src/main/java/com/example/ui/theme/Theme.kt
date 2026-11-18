package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = PureSurface,
    primaryContainer = SoftSage,
    onPrimaryContainer = ForestGreen,
    secondary = DeepMoss,
    onSecondary = PureSurface,
    secondaryContainer = MistSage,
    onSecondaryContainer = CharcoalLeaf,
    tertiary = MintHighlight,
    onTertiary = PureSurface,
    tertiaryContainer = HarvestGoldBg,
    onTertiaryContainer = HarvestAmber,
    background = AgrarianCanvas,
    onBackground = CharcoalLeaf,
    surface = PureSurface,
    onSurface = CharcoalLeaf,
    surfaceVariant = MistSage,
    onSurfaceVariant = MutedPine,
    outline = SageOutline,
    outlineVariant = SoftSage,
    error = AlertRust,
    onError = PureSurface,
    errorContainer = AlertRustBg,
    onErrorContainer = AlertRust
)

private val DarkColorScheme = darkColorScheme(
    primary = MeadowMint,
    onPrimary = NightCanopyBg,
    primaryContainer = DeepMoss,
    onPrimaryContainer = SoftSage,
    secondary = MintHighlight,
    onSecondary = NightCanopyBg,
    secondaryContainer = NightCanopyCard,
    onSecondaryContainer = NightSoftText,
    tertiary = SoftSage,
    onTertiary = ForestGreen,
    background = NightCanopyBg,
    onBackground = NightSoftText,
    surface = NightCanopySurface,
    onSurface = NightSoftText,
    surfaceVariant = NightCanopyCard,
    onSurfaceVariant = NightMutedText,
    outline = NightSageBorder,
    outlineVariant = NightCanopyCard,
    error = AlertRust,
    onError = PureSurface
)

val KrishivantriShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KrishivantriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KrishivantriTypography,
        shapes = KrishivantriShapes,
        content = content
    )
}
