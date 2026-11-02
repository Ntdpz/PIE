package com.ntdpz.pie.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.PiePrimaryButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onChangeMonth: (Long) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onOpenDiary: () -> Unit,
) {
    val locale = Locale.forLanguageTag("th")
    val entry = state.entries.firstOrNull { it.date == state.selectedDate }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ย้อนวัน", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onToday) { Text("วันนี้") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChangeMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "เดือนก่อน")
            }
            Text(
                state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onChangeMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "เดือนถัดไป")
            }
        }
        MonthCalendar(
            month = state.month,
            selectedDate = state.selectedDate,
            entryDates = state.entries.map { it.date }.toSet(),
            onSelectDate = onSelectDate,
        )
        if (state.loading) CircularProgressIndicator()
        if (state.loadFailed) Text("เปิดรายการบันทึกไม่สำเร็จ")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(state.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)))
                Text(entry?.preview?.takeIf(String::isNotBlank) ?: "วันนี้ยังไม่มีเรื่องเล่า", maxLines = 3)
                PiePrimaryButton(
                    label = if (entry == null) "เขียนบันทึกวันนี้" else "เปิดบันทึก",
                    onClick = onOpenDiary,
                    enabled = !state.loading && !state.loadFailed,
                )
            }
        }
    }
}
