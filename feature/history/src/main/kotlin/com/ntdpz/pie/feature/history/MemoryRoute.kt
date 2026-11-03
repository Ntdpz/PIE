package com.ntdpz.pie.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryRoute(viewModel: MemoryViewModel, onBack: () -> Unit, onEdit: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loader = rememberMemoryImageLoader()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var zooming by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { expanded = false; zooming = false }
    if (state.loading || state.failed || state.days.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับ") }
            when {
                state.loading -> PieLoader("กำลังเปิดความทรงจำ…")
                state.failed -> PiePrimaryButton("เปิดไม่สำเร็จ · ลองอีกครั้ง", viewModel::retry)
                else -> Text("ยังไม่มีความทรงจำ")
            }
        }
        return
    }
    val latestDays by rememberUpdatedState(state.days)
    val pager = rememberPagerState(initialPage = state.days.indexOfFirst { it.summary.date == viewModel.initialDate }.coerceAtLeast(0)) { state.days.size }
    LaunchedEffect(pager) {
        snapshotFlow { latestDays.getOrNull(pager.settledPage)?.summary?.date }.filterNotNull().distinctUntilChanged().collect {
            expanded = latestDays.firstOrNull { day -> day.summary.date == it }?.media?.isEmpty() == true; zooming = false; viewModel.selectDate(it)
        }
    }
    LaunchedEffect(expanded) { if (expanded) zooming = false }
    BackHandler(expanded) { expanded = false }
    VerticalPager(
        state = pager, key = { state.days[it].summary.date.toString() },
        userScrollEnabled = !expanded && !zooming, modifier = Modifier.fillMaxSize().testTag("memory-day-pager"),
    ) { index ->
        val day = state.days[index]
        val active = index == pager.settledPage
        val text = if (day.summary.date == state.date) state.text else day.summary.preview
        val photos = rememberPagerState { day.media.size }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(Modifier.weight(1f).fillMaxWidth().testTag("memory-media-${day.summary.date}").background(Color(0xFF1D1B19))) {
                if (day.media.isNotEmpty()) HorizontalPager(
                    state = photos, key = { day.media[it].id }, userScrollEnabled = !zooming,
                    modifier = Modifier.fillMaxSize().testTag("memory-photos-${day.summary.date}"),
                ) { photo ->
                    MemoryAttachment(day.media[photo], loader, active && photo == photos.settledPage && !expanded, { if (active && photo == photos.settledPage) zooming = it }, Modifier.fillMaxSize())
                } else PieIcon(PieGlyph.Book, color = MaterialTheme.colorScheme.primary, size = 72.dp, modifier = Modifier.align(Alignment.Center))
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(24.dp)) { IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับสู่ความทรงจำ") } }
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) { Text(PieDates.entry(day.summary.date), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
                    Surface(shape = RoundedCornerShape(24.dp)) { IconButton(onClick = { onEdit(day.summary.date) }) { PieIcon(PieGlyph.Write, "แก้บันทึก") } }
                }
                if (day.media.isNotEmpty()) Surface(Modifier.align(Alignment.BottomCenter).padding(12.dp), shape = RoundedCornerShape(20.dp)) {
                    Text("${photos.currentPage + 1} / ${day.media.size}", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "อ่านเรื่องเล่าทั้งหมด") { expanded = true },
            ) {
                Column(Modifier.pointerInput(day.summary.date) { detectVerticalDragGestures { change, amount ->
                    if (amount < 0) { change.consume(); expanded = true }
                } }.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(PieDates.shortEntry(day.summary.date), style = MaterialTheme.typography.titleLarge)
                    if (text.isNotBlank()) Text(text, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(if (text.isBlank()) "ยังไม่มีข้อความ · แตะเพื่อเปิด" else "อ่านต่อ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (expanded) {
        val day = state.days.getOrNull(pager.settledPage) ?: return
        val text = if (day.summary.date == state.date) state.text else day.summary.preview
        ModalBottomSheet(onDismissRequest = { expanded = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(PieDates.entry(day.summary.date), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { expanded = false }) { PieIcon(PieGlyph.Close, "ปิดเรื่องเล่า") }
                }
                Text(text.ifBlank { "วันนี้มีภาพให้เก็บไว้" }, Modifier.testTag("memory-story"), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { expanded = false; onEdit(day.summary.date) }) { PieIcon(PieGlyph.Write); Text("แก้บันทึก", Modifier.padding(start = 8.dp)) }
            }
        }
    }
}
