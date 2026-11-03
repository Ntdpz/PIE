package com.ntdpz.pie.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PieLightColors = lightColorScheme(
    primary = Color(0xFFA64E3C),
    onPrimary = Color(0xFFFFFCF7),
    primaryContainer = Color(0xFFF0DDD1),
    onPrimaryContainer = Color(0xFF652919),
    secondary = Color(0xFFA64E3C),
    onSecondary = Color(0xFFFFFCF7),
    secondaryContainer = Color(0xFFF0DDD1),
    onSecondaryContainer = Color(0xFF652919),
    background = Color(0xFFF7F3EC),
    onBackground = Color(0xFF302B28),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF302B28),
    onSurfaceVariant = Color(0xFF746B62),
    outlineVariant = Color(0xFFE4DCD1),
    tertiary = Color(0xFF52664E),
    surfaceVariant = Color(0xFFEDE7DD),
    outline = Color(0xFF85796C),
    error = Color(0xFFA93632),
    onError = Color(0xFFFFFCF7),
    errorContainer = Color(0xFFF6DED8),
    onErrorContainer = Color(0xFF652723),
    surfaceContainerLowest = Color(0xFFFFFCF7),
    surfaceContainer = Color(0xFFFFFCF7),
    surfaceContainerLow = Color(0xFFF7F3EC),
    surfaceContainerHigh = Color(0xFFEDE7DD),
    surfaceContainerHighest = Color(0xFFE4DCD1),
    surfaceTint = Color.Transparent,
)

private val PieDarkColors = darkColorScheme(
    primary = Color(0xFFE7AD93),
    onPrimary = Color(0xFF302B28),
    primaryContainer = Color(0xFF49352B),
    onPrimaryContainer = Color(0xFFF0DDD1),
    secondary = Color(0xFFE7AD93),
    onSecondary = Color(0xFF302B28),
    secondaryContainer = Color(0xFF49352B),
    onSecondaryContainer = Color(0xFFF0DDD1),
    background = Color(0xFF1D1B19),
    onBackground = Color(0xFFF1EADF),
    surface = Color(0xFF292622),
    onSurface = Color(0xFFF1EADF),
    surfaceVariant = Color(0xFF37312B),
    onSurfaceVariant = Color(0xFFB8AD9D),
    outline = Color(0xFF968979),
    outlineVariant = Color(0xFF484037),
    tertiary = Color(0xFFB3C3A3),
    error = Color(0xFFE8998F),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF49302B),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF181613),
    surfaceContainerLow = Color(0xFF231F1B),
    surfaceContainer = Color(0xFF292622),
    surfaceContainerHigh = Color(0xFF312B25),
    surfaceContainerHighest = Color(0xFF37312B),
    surfaceTint = Color.Transparent,
)

@Composable
fun PieTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) PieDarkColors else PieLightColors, typography = PieTypography, content = content)
}
