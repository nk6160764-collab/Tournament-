package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GamerZoneColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = PitchBlack,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = NeonGreen,
    secondary = ElectricEmerald,
    onSecondary = PitchBlack,
    secondaryContainer = CyberCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldTrophy,
    onTertiary = PitchBlack,
    background = PitchBlack,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = CrimsonLive,
    onError = TextPrimary
)

@Composable
fun GamerZoneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamerZoneColorScheme,
        typography = Typography,
        content = content
    )
}
