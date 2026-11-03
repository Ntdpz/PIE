package com.ntdpz.pie.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PieLightColors = lightColorScheme(
    primary = Color(0xFFB94F39),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8DAD0),
    onPrimaryContainer = Color(0xFF652919),
    secondary = Color(0xFFB94F39),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8DAD0),
    onSecondaryContainer = Color(0xFF652919),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF352D32),
    surface = Color.White,
    onSurface = Color(0xFF352D32),
    onSurfaceVariant = Color(0xFF77717A),
    outlineVariant = Color(0xFFE6E4E8),
    tertiary = Color(0xFF466D59),
    surfaceVariant = Color(0xFFEEEBEF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainerHigh = Color(0xFFEEEBEF),
    surfaceContainerHighest = Color(0xFFE6E4E8),
    surfaceTint = Color.Transparent,
)

private val PieDarkColors = darkColorScheme(
    primary = Color(0xFFF2AD96),
    onPrimary = Color(0xFF392018),
    primaryContainer = Color(0xFF432D2B),
    onPrimaryContainer = Color(0xFFF8DAD0),
    secondary = Color(0xFFF2AD96),
    onSecondary = Color(0xFF392018),
    secondaryContainer = Color(0xFF432D2B),
    onSecondaryContainer = Color(0xFFF8DAD0),
    background = Color(0xFF171518),
    onBackground = Color(0xFFF4ECE8),
    surface = Color(0xFF242124),
    onSurface = Color(0xFFF4ECE8),
    surfaceVariant = Color(0xFF332D33),
    onSurfaceVariant = Color(0xFFBFB2BA),
    outline = Color(0xFF8F818A),
    outlineVariant = Color(0xFF433C43),
    tertiary = Color(0xFF9BC5A7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF121013),
    surfaceContainerLow = Color(0xFF201D20),
    surfaceContainer = Color(0xFF242124),
    surfaceContainerHigh = Color(0xFF2C272C),
    surfaceContainerHighest = Color(0xFF332D33),
    surfaceTint = Color.Transparent,
)

@Composable
fun PieTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) PieDarkColors else PieLightColors, typography = PieTypography, content = content)
}
