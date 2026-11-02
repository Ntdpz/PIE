package com.ntdpz.pie.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.R
import kotlin.math.PI
import kotlin.math.sin

/** Shown only while real work is pending; never delays the completed state. */
@Composable
fun PieLoader(label: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "PIE loading")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "Loading wave",
    )
    val color = MaterialTheme.colorScheme.primary
    val semantics = modifier.semantics(mergeDescendants = true) {
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        liveRegion = LiveRegionMode.Polite
    }
    val dots: @Composable () -> Unit = {
        Canvas(Modifier.size(width = 40.dp, height = 20.dp)) {
            repeat(3) { index ->
                val lift = (sin((phase.value * 2 * PI - index * 0.7)).toFloat() + 1f) / 2f
                drawCircle(
                    color.copy(alpha = 0.35f + lift * 0.65f),
                    radius = 3.dp.toPx(),
                    center = Offset((8 + index * 12).dp.toPx(), 13.dp.toPx() - lift * 6.dp.toPx()),
                )
            }
        }
    }
    if (compact) {
        Row(semantics, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dots()
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(semantics, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painterResource(R.drawable.pie_mark), contentDescription = null,
                modifier = Modifier.size(56.dp).graphicsLayer {
                    val breath = sin(phase.value * 2 * PI).toFloat()
                    translationY = -breath * 2.dp.toPx()
                    scaleX = 1f + breath * 0.025f
                    scaleY = scaleX
                },
            )
            dots()
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
