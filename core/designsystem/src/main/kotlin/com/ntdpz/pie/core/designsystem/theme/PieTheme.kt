package com.ntdpz.pie.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PieColors = lightColorScheme(
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
    onSurfaceVariant = Color(0xFF756E76),
    outlineVariant = Color(0xFFE6E4E8),
)

@Composable
fun PieTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PieColors, content = content)
}
