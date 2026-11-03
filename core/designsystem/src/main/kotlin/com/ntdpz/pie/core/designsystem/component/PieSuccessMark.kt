package com.ntdpz.pie.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import kotlinx.coroutines.launch

/** Finite celebration. Both animations honor the system animator duration scale. */
@Composable
fun PieSuccessMark(modifier: Modifier = Modifier) {
    val stroke = remember { Animatable(0f) }
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 320f)) }
        stroke.animateTo(1f, tween(520, easing = PieMotion.Emphasized))
    }
    val ink = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier.size(72.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
        drawCircle(background)
        val start = Offset(size.width * 0.27f, size.height * 0.5f)
        val bend = Offset(size.width * 0.43f, size.height * 0.66f)
        val end = Offset(size.width * 0.74f, size.height * 0.34f)
        val first = (stroke.value / 0.35f).coerceIn(0f, 1f)
        val second = ((stroke.value - 0.35f) / 0.65f).coerceIn(0f, 1f)
        if (first > 0f) drawLine(ink, start, start + (bend - start) * first, 4.dp.toPx(), StrokeCap.Round)
        if (second > 0f) drawLine(ink, bend, bend + (end - bend) * second, 4.dp.toPx(), StrokeCap.Round)
    }
}
