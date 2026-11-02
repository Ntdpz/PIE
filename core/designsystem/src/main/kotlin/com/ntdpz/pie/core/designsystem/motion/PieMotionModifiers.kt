package com.ntdpz.pie.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset

/** Finite entrances; Compose honors the device animator duration scale, including zero. */
@Composable
fun Modifier.pieReveal(order: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(PieMotion.RevealMillis, order.coerceIn(0, 4) * 45, PieMotion.Emphasized))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16f * density
    }
}

@Composable
fun Modifier.piePressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.72f, stiffness = 850f), label = "Press scale",
    )
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** A single quiet sweep on the invitation, with no persistent animation workload. */
@Composable
fun Modifier.pieSheen(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1000, 180, PieMotion.Emphasized)) }
    return drawWithContent {
        drawContent()
        if (progress.value > 0f && progress.value < 1f) {
            val x = -size.width + size.width * 3f * progress.value
            drawRect(Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent),
                start = Offset(x - size.width * 0.4f, 0f),
                end = Offset(x, size.height),
            ))
        }
    }
}
