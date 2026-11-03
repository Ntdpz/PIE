package com.ntdpz.pie.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*

@Composable
internal fun PieBottomBar(route: String?, onSelect: (PieDestination) -> Unit) {
    val selectedIndex = PieDestination.entries.indexOfFirst { it.route == route }.coerceAtLeast(0)
    val position = animateFloatAsState(selectedIndex.toFloat(), spring(dampingRatio = 0.85f, stiffness = 500f), label = "Tab indicator")
    val pillColor = MaterialTheme.colorScheme.primaryContainer
    val haptic = LocalHapticFeedback.current
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().height(84.dp).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
                    .drawBehind {
                        val gap = 8.dp.toPx()
                        val width = (size.width - gap * 2) / 3
                        drawRoundRect(pillColor, Offset((width + gap) * position.value + (width - 52.dp.toPx()) / 2f, 0f), Size(52.dp.toPx(), 36.dp.toPx()), CornerRadius(18.dp.toPx()))
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PieDestination.entries.forEach { destination ->
                    val active = route == destination.route
                    val tint = animateColorAsState(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(PieMotion.GentleMillis), label = "Tab tint")
                    val iconScale = animateFloatAsState(if (active) 1.08f else 1f, spring(dampingRatio = 0.65f, stiffness = 700f), label = "Tab icon")
                    Surface(
                        onClick = { if (!active) { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick); onSelect(destination) } },
                        modifier = Modifier.weight(1f).fillMaxHeight().semantics { selected = active; role = Role.Tab },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                        ) {
                            PieIcon(destination.glyph, color = tint.value, modifier = Modifier.graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value })
                            Text(
                                destination.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = tint.value,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}
