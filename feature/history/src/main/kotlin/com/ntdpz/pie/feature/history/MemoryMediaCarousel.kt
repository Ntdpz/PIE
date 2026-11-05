package com.ntdpz.pie.feature.history

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
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
    heroHeight: Dp,
    pager: PagerState,
) {
    val context = LocalContext.current.applicationContext
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
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF1D1B19))) {
        val ratio = ratios[current.id]
        val height by animateDpAsState(
            targetValue = when {
                ratio == null -> 96.dp
                ratio < 1f && !current.isVideo -> heroHeight
                else -> (maxWidth / ratio).coerceAtMost(heroHeight)
            },
            animationSpec = tween(PieMotion.GentleMillis, easing = PieMotion.Emphasized),
            label = "Memory media height",
        )
        if (ratio != null && ratio >= 1f) {
            AsyncImage(
                model = current.uri,
                contentDescription = null,
                imageLoader = loader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f)))
        }
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth().height(height).testTag("memory-media-${day.summary.date}")
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
                            fillPhoto = ratios[item.id]?.let { it < 1f } == true,
                        )
                        item.id in ratios -> TextButton(onClick = { ratios = ratios - item.id; retry++ }) { Text("เปิดสื่อไม่สำเร็จ · ลองอีกครั้ง") }
                        else -> PieLoader("กำลังเปิดสื่อ…", compact = true)
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.45f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1D1B19)))),
            )
    }
}

@Composable
internal fun MemoryPhotoIndicators(pager: PagerState) {
    if (pager.pageCount <= 1) return

    val scope = rememberCoroutineScope()
    val dots = rememberLazyListState()
    LaunchedEffect(pager.currentPage) {
        if (dots.layoutInfo.visibleItemsInfo.none { it.index == pager.currentPage }) {
            dots.scrollToItem(pager.currentPage)
        }
    }
    LazyRow(
        state = dots,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(pager.pageCount) { index ->
            val selected = index == pager.currentPage
            val width by animateDpAsState(
                if (selected) 20.dp else 7.dp,
                animationSpec = tween(PieMotion.QuickMillis),
                label = "Selected photo indicator",
            )
            Box(
                Modifier.size(48.dp)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                    )
                    .semantics { contentDescription = "รูปที่ ${index + 1} จาก ${pager.pageCount}" },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier.size(width, 7.dp).background(
                        if (selected) Color(0xFFE7AD93) else Color(0xFFB8AD9D),
                        RoundedCornerShape(8.dp),
                    ),
                )
            }
        }
    }
}
