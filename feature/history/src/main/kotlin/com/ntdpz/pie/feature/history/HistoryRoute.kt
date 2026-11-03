package com.ntdpz.pie.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRoute(viewModel: HistoryViewModel, onOpenDiary: (LocalDate) -> Unit, onOpenMemory: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var calendar by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { calendar = false }
    val loader = rememberMemoryImageLoader()
    MemoryGallery(state, loader, viewModel::retry, { calendar = true }, onOpenMemory, { onOpenDiary(state.today) })
    if (calendar) ModalBottomSheet(
        onDismissRequest = { calendar = false },
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("เลือกวัน", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { viewModel.jumpToToday() }) { Text("วันนี้") }
                IconButton(onClick = { calendar = false }) { PieIcon(PieGlyph.Close, "ปิดปฏิทิน") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.changeMonth(-1) }) { PieIcon(PieGlyph.Previous, "เดือนก่อน") }
                Text(PieDates.month(state.month), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.changeMonth(1) }) { PieIcon(PieGlyph.Next, "เดือนถัดไป") }
            }
            MonthCalendar(state.month, state.selectedDate, state.entries.map { it.date }.toSet()) { date ->
                viewModel.selectDate(date)
                calendar = false
                if (state.entries.any { it.date == date }) onOpenMemory(date) else onOpenDiary(date)
            }
            Text("• วันที่มีเรื่องเล่า", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
