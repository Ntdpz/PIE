package com.ntdpz.pie.feature.history

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.ntdpz.pie.core.designsystem.component.PieLoader
import com.ntdpz.pie.core.designsystem.motion.PieMotion

@Composable
internal fun MemoryMediaCarousel(
    day: MemoryDay,
    loader: ImageLoader,
    viewport: Rect,
    zooming: Boolean,
    onZoom: (Boolean) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val pager = rememberPagerState { day.media.size }
    var ratios by remember(day.media) { mutableStateOf<Map<String, Float?>>(emptyMap()) }
    var retry by remember { mutableIntStateOf(0) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val current = day.media.getOrNull(pager.currentPage) ?: return
    LaunchedEffect(day.media, pager.currentPage, retry) {
        val indices = listOf(pager.currentPage, pager.currentPage + 1, pager.currentPage - 1)
        for (index in indices) {
            val item = day.media.getOrNull(index) ?: continue
            if (item.id !in ratios) ratios = ratios + (item.id to readMemoryAspectRatio(context, item))
        }
    }
    val visible = bounds.bottom > viewport.top && bounds.top < viewport.bottom && bounds.height > 0f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val ratio = ratios[current.id]
        val height by animateDpAsState(
            targetValue = if (ratio != null) maxWidth / ratio else 96.dp,
            animationSpec = tween(PieMotion.GentleMillis, easing = PieMotion.Emphasized),
            label = "Memory media height",
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(height).testTag("memory-media-${day.summary.date}")
                    .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)
                    .onGloballyPositioned { bounds = it.boundsInRoot() },
            ) {
                HorizontalPager(
                    state = pager, key = { day.media[it].id }, userScrollEnabled = !zooming,
                    modifier = Modifier.fillMaxSize().testTag("memory-photos-${day.summary.date}"),
                ) { index ->
                    val item = day.media[index]
                    when {
                        ratios[item.id] != null -> MemoryAttachment(
                            item, loader, visible && index == pager.settledPage,
                            { if (index == pager.settledPage) onZoom(it) }, Modifier.fillMaxSize(),
                        )
                        item.id in ratios -> TextButton(onClick = { ratios = ratios - item.id; retry++ }) { Text("เปิดสื่อไม่สำเร็จ · ลองอีกครั้ง") }
                        else -> PieLoader("กำลังเปิดสื่อ…", compact = true)
                    }
                }
            }
            if (day.media.size > 1) Text(
                "${pager.currentPage + 1} / ${day.media.size}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
