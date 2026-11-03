package com.ntdpz.pie.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import java.time.LocalDate

@Composable
internal fun MemoryGallery(
    state: HistoryUiState,
    imageLoader: ImageLoader,
    onRetry: () -> Unit,
    onCalendar: () -> Unit,
    onOpen: (LocalDate) -> Unit,
    onWrite: () -> Unit,
) {
    val grid = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), state = grid,
        modifier = Modifier.fillMaxSize().testTag("memory-gallery"), contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ความทรงจำ", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                IconButton(onClick = onCalendar) { PieIcon(PieGlyph.Calendar, "เลือกวันจากปฏิทิน", color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) { PieLoader("กำลังเปิดความทรงจำ…", compact = true) }
        if (state.loadFailed) item(span = { GridItemSpan(maxLineSpan) }) { TextButton(onClick = onRetry) { Text("เปิดรายการไม่สำเร็จ · ลองอีกครั้ง") } }
        if (!state.loading && !state.loadFailed && state.memories.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 64.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                PieIcon(PieGlyph.Book, color = MaterialTheme.colorScheme.primary, size = 48.dp)
                Text("เรื่องเล่าจะค่อย ๆ อยู่ตรงนี้", style = MaterialTheme.typography.titleLarge)
                PiePrimaryButton("เขียนวันนี้", onWrite, icon = PieGlyph.Write)
            }
        }
        itemsIndexed(state.memories, key = { _, day -> day.summary.date.toString() }, span = { index, _ -> GridItemSpan(if (index == 0) maxLineSpan else 1) }) { index, day ->
            Column(
                Modifier.fillMaxWidth().clickable(onClickLabel = "เปิดความทรงจำ ${PieDates.entry(day.summary.date)}") { onOpen(day.summary.date) },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxWidth().height(if (index == 0) 260.dp else 180.dp)) {
                        val cover = day.media.firstOrNull()
                        if (cover != null) {
                            AsyncImage(cover.uri, "ภาพความทรงจำ ${PieDates.entry(day.summary.date)}", imageLoader = imageLoader, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Surface(Modifier.align(Alignment.TopEnd).padding(10.dp), shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (cover.isVideo) PieIcon(PieGlyph.Play, "มีวิดีโอ", size = 16.dp)
                                    Text("${day.media.size}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PieIcon(PieGlyph.Book, color = MaterialTheme.colorScheme.primary)
                                Text(day.summary.preview, style = MaterialTheme.typography.bodyMedium, maxLines = if (index == 0) 5 else 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Text(PieDates.shortEntry(day.summary.date), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
