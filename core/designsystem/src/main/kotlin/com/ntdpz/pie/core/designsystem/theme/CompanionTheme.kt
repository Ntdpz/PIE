package com.ntdpz.pie.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

object CompanionColors {
    val Cream = Color(0xFFFFF4E8)
    val Caramel = Color(0xFFE6C7A6)
    val Rose = Color(0xFFC64F78)
    val Ink = Color(0xFF302A32)
    val DarkSurface = Color(0xFF3B333C)
    val DarkCaramel = Color(0xFF705449)
    val DarkRose = Color(0xFFF3A8C0)
}

/** Companion palette follows the app's resolved theme, including explicit theme overrides. */
@Composable
fun CompanionTheme(content: @Composable () -> Unit) {
    val cream = CompanionColors.Cream
    val caramel = CompanionColors.Caramel
    val rose = CompanionColors.Rose
    val ink = CompanionColors.Ink
    val dark = LocalPieDarkTheme.current
    val colors = if (dark) {
        MaterialTheme.colorScheme.copy(
            primary = CompanionColors.DarkRose, onPrimary = ink,
            primaryContainer = CompanionColors.DarkCaramel, onPrimaryContainer = cream,
            background = ink, onBackground = cream,
            surface = CompanionColors.DarkSurface, onSurface = cream, onSurfaceVariant = caramel,
            surfaceVariant = CompanionColors.DarkCaramel,
            outline = CompanionColors.DarkRose, outlineVariant = CompanionColors.DarkCaramel,
            error = cream, onError = ink,
        )
    } else {
        MaterialTheme.colorScheme.copy(
            primary = rose, onPrimary = cream,
            primaryContainer = caramel, onPrimaryContainer = ink,
            background = cream, onBackground = ink,
            surface = cream, onSurface = ink, onSurfaceVariant = ink,
            surfaceVariant = caramel, outline = rose, outlineVariant = caramel,
            error = ink, onError = cream,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
