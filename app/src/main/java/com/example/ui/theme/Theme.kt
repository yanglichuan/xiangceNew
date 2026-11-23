package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MuseumLightColorScheme = lightColorScheme(
    primary = MuseumInk,
    onPrimary = MuseumPaper,
    primaryContainer = MuseumBrassContainer,
    onPrimaryContainer = MuseumInk,
    secondary = MuseumBrassGold,
    onSecondary = MuseumCanvas,
    secondaryContainer = MuseumWarmStone,
    onSecondaryContainer = MuseumInk,
    tertiary = MuseumFilmRed,
    onTertiary = MuseumCanvas,
    background = MuseumPaper,
    onBackground = MuseumInk,
    surface = MuseumCanvas,
    onSurface = MuseumInk,
    surfaceVariant = MuseumWarmStone,
    onSurfaceVariant = MuseumSlate,
    surfaceContainerLow = MuseumPaper,
    surfaceContainer = MuseumWarmStone,
    surfaceContainerHigh = MuseumSurfaceHighest,
    outline = MuseumHairline,
    outlineVariant = MuseumHairline
)

private val ScreeningDarkColorScheme = darkColorScheme(
    primary = ScreeningChampagne,
    onPrimary = ScreeningObsidian,
    primaryContainer = ScreeningSurfaceHigh,
    onPrimaryContainer = ScreeningIvory,
    secondary = ScreeningChampagne,
    onSecondary = ScreeningObsidian,
    secondaryContainer = ScreeningSurface,
    onSecondaryContainer = ScreeningIvory,
    tertiary = MuseumFilmRed,
    onTertiary = ScreeningIvory,
    background = ScreeningObsidian,
    onBackground = ScreeningIvory,
    surface = ScreeningSurface,
    onSurface = ScreeningIvory,
    surfaceVariant = ScreeningSurfaceHigh,
    onSurfaceVariant = ScreeningMuted,
    surfaceContainerLow = ScreeningObsidian,
    surfaceContainer = ScreeningSurface,
    surfaceContainerHigh = ScreeningSurfaceHigh,
    outline = ScreeningBorder,
    outlineVariant = ScreeningBorder
)

val AtelierShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AtelierGalleryTheme(
    darkScreeningRoom: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkScreeningRoom) ScreeningDarkColorScheme else MuseumLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AtelierTypography,
        shapes = AtelierShapes,
        content = content
    )
}
