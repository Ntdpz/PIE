package com.ntdpz.pie.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.domain.media.DiaryMedia
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun MemoryGallery(
    state: HistoryUiState,
    imageLoader: ImageLoader,
    onRetry: () -> Unit,
    onCalendar: () -> Unit,
    onOpen: (LocalDate) -> Unit,
    onWrite: () -> Unit,
) {
    val columns = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2
    val months = remember(state.memories) { state.memories.groupBy { YearMonth.from(it.summary.date) } }
    val grid = rememberLazyGridState()
    var preview by remember { mutableStateOf<DiaryMedia?>(null) }
    var held by remember { mutableStateOf(false) }
    val videoKeys = remember(state.memories) {
        state.memories.flatMap { day -> day.media.filter { it.isVideo }.map { it.id } }.toSet()
    }
    val playingTile by remember(grid, videoKeys) {
        derivedStateOf {
            val layout = grid.layoutInfo
            layout.visibleItemsInfo.firstOrNull { item ->
                item.key in videoKeys &&
                    item.offset.y >= layout.viewportStartOffset &&
                    item.offset.y + item.size.height <= layout.viewportEndOffset
            }?.key
        }
    }
    BackHandler(preview != null) { preview = null }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { preview = null }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = grid,
            modifier = Modifier.fillMaxSize().testTag("memory-gallery"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = preview == null,
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    FilledIconButton(
                        onClick = onCalendar,
                        modifier = Modifier.align(Alignment.End).size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { PieIcon(PieGlyph.Calendar, "เลือกวันจากปฏิทิน", color = MaterialTheme.colorScheme.primary) }
                    Text(
                        "ความทรงจำ",
                        Modifier.fillMaxWidth().padding(bottom = 16.dp).semantics { heading() },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    HorizontalDivider()
                }
            }
            if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) { PieLoader("กำลังเปิดความทรงจำ…", compact = true) }
            if (state.loadFailed) item(span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = onRetry) { Text("เปิดรายการไม่สำเร็จ · ลองอีกครั้ง") }
            }
            if (!state.loading && !state.loadFailed && state.memories.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 64.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    PieIcon(PieGlyph.Book, color = MaterialTheme.colorScheme.primary, size = 48.dp)
                    Text("เรื่องเล่าจะค่อย ๆ อยู่ตรงนี้", style = MaterialTheme.typography.titleLarge)
                    PiePrimaryButton("เขียนวันนี้", onWrite, icon = PieGlyph.Write)
                }
            }
            months.forEach { (month, days) ->
                item(key = month.toString(), span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        PieDates.month(month),
                        Modifier.padding(top = 8.dp).semantics { heading() },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                days.forEach { day ->
                    val date = day.summary.date
                    val dateLabel = PieDates.shortEntry(date)
                    if (day.media.isEmpty()) {
                        item(key = date.toString(), span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                onClick = { onOpen(date) },
                                modifier = Modifier.testTag("memory-tile-$date-"),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PieIcon(PieGlyph.Book, size = 18.dp)
                                    Text("$dateLabel · ${day.summary.preview.ifBlank { "เรื่องเล่า" }}", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    PieIcon(PieGlyph.Next, size = 16.dp)
                                }
                            }
                        }
                    }
                    items(day.media, key = { it.id }) { media ->
                        val open = { onOpen(date) }
                        val label = if (media.isVideo) "วิดีโอ $dateLabel" else "รูปภาพ $dateLabel"
                        Surface(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                .testTag("memory-tile-$date-${media.id}")
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = label
                                    onClick("เปิดบันทึก") { open(); true }
                                    onLongClick("ดูสื่อเต็ม") { held = false; preview = media; true }
                                }
                                .pointerInput(media.id, date) {
                                    detectTapGestures(
                                        onTap = { open() },
                                        onLongPress = { held = true; preview = media },
                                        onPress = {
                                            try { tryAwaitRelease() } finally {
                                                if (held && preview?.id == media.id) preview = null
                                            }
                                        },
                                    )
                                },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Box {
                                if (media.isVideo) {
                                    MemoryVideo(media.uri, imageLoader, preview == null && playingTile == media.id, Modifier.fillMaxSize(), preview = true)
                                    Text(
                                        "วิดีโอ",
                                        Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                            .background(Color(0xDB302B28), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                } else {
                                    AsyncImage(media.uri, null, imageLoader, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                        }
                    }
                }
            }
        }
        Crossfade(targetState = preview, label = "Full media preview") { selected ->
            if (selected != null) {
                Box(
                    Modifier.fillMaxSize().testTag("memory-hold-preview")
                        .background(Color(0xFF1D1B19))
                        .pointerInput(Unit) { detectTapGestures(onTap = { preview = null }) },
                ) {
                    val mediaModifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 72.dp)
                    if (selected.isVideo) {
                        MemoryVideo(selected.uri, imageLoader, preview == selected, mediaModifier, preview = true, cropPreview = false)
                    } else {
                        AsyncImage(selected.uri, "ภาพเต็ม", imageLoader, mediaModifier, contentScale = ContentScale.Fit)
                    }
                    Text(
                        if (held) "ปล่อยนิ้วเพื่อกลับไปที่กริด" else "แตะเพื่อกลับไปที่กริด",
                        Modifier.align(Alignment.BottomCenter).padding(24.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
