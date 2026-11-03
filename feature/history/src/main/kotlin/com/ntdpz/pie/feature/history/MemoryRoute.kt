package com.ntdpz.pie.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

@Composable
fun MemoryRoute(viewModel: MemoryViewModel, onBack: () -> Unit, onEdit: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loader = rememberMemoryImageLoader()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับสู่ความทรงจำ") }
            Text("เรื่องเล่า", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PieLoader("กำลังเปิดความทรงจำ…") }
            state.failed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { PiePrimaryButton("เปิดไม่สำเร็จ · ลองอีกครั้ง", viewModel::retry) }
            state.days.isEmpty() -> Text("ยังไม่มีความทรงจำ", Modifier.padding(24.dp))
            else -> {
                val list = rememberLazyListState(initialFirstVisibleItemIndex = state.days.indexOfFirst { it.summary.date == viewModel.initialDate }.coerceAtLeast(0))
                var viewport by remember { mutableStateOf(Rect.Zero) }
                var zoomedDate by remember { mutableStateOf<LocalDate?>(null) }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) { zoomedDate = null }
                LaunchedEffect(list) {
                    snapshotFlow { list.layoutInfo.visibleItemsInfo.mapNotNull { item -> (item.key as? String)?.let(LocalDate::parse) }.toSet() }
                        .distinctUntilChanged().collect(viewModel::showDates)
                }
                LazyColumn(
                    state = list, userScrollEnabled = zoomedDate == null,
                    modifier = Modifier.fillMaxSize().testTag("memory-feed").onGloballyPositioned { viewport = it.boundsInRoot() },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(state.days, key = { it.summary.date.toString() }) { day ->
                        Column(Modifier.fillMaxWidth().testTag("memory-entry-${day.summary.date}"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(PieDates.entry(day.summary.date), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { onEdit(day.summary.date) }, Modifier.testTag("memory-edit-${day.summary.date}")) { PieIcon(PieGlyph.Write, "แก้บันทึก", size = 20.dp) }
                            }
                            if (day.media.isNotEmpty()) MemoryMediaCarousel(
                                day, loader, viewport, zoomedDate == day.summary.date,
                                onZoom = { zoomedDate = if (it) day.summary.date else null },
                            )
                            val entry = state.entries[day.summary.date]
                            when {
                                entry == null -> PieLoader("กำลังเปิดเรื่องเล่า…", compact = true)
                                entry.failed -> TextButton(onClick = viewModel::retry) { Text("เปิดเรื่องเล่าไม่สำเร็จ · ลองอีกครั้ง") }
                                entry.text.isNotBlank() -> Text(
                                    entry.text, Modifier.fillMaxWidth().testTag("memory-story-${day.summary.date}"),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 29.sp),
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}
