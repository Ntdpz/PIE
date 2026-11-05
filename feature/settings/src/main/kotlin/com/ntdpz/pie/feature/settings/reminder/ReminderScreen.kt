package com.ntdpz.pie.feature.settings.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.domain.settings.*
import com.ntdpz.pie.feature.settings.SettingsUiState
import java.time.LocalDate

@Composable
fun ReminderScreen(state: SettingsUiState, today: LocalDate, onBack: () -> Unit, onUpdate: (ReminderSettings) -> Unit, onEnable: (Boolean) -> Unit, onEditTime: (Int?) -> Unit) {
    val r = state.settings.reminder
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับ") }
            Text("เวลาของตัวเอง", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Switch(r.enabled, onEnable, enabled = state.settings.loaded)
        }
        r.times.sorted().forEachIndexed { index, time ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEditTime(time) }, Modifier.weight(1f)) {
                    Text("%02d:%02d".format(time / 60, time % 60), style = if (index == 0) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineSmall)
                }
                if (index == 0) IconButton(onClick = { onEditTime(null) }) { PieIcon(PieGlyph.Plus, "เพิ่มเวลาแจ้งเตือน") }
                if (r.times.size > 1) IconButton(onClick = { onUpdate(r.copy(times = r.times - time)) }) { PieIcon(PieGlyph.Close, "ลบเวลา", size = 18.dp) }
            }
        }
        if (r.times.isEmpty()) TextButton(onClick = { onEditTime(null) }) { Text("เพิ่มเวลาแจ้งเตือน") }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("จ", "อ", "พ", "พฤ", "ศ", "ส", "อา").forEachIndexed { index, label ->
                val day = index + 1
                val selected = day in r.days
                Surface(onClick = {
                    val days = if (selected) r.days - day else r.days + day
                    onUpdate(r.copy(days = days, enabled = r.enabled && days.isNotEmpty()))
                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(24.dp), color = if (selected) colors.primaryContainer else colors.surface) {
                    Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
        HorizontalDivider()
        Text("คำชวนของวันนี้", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Text(ReminderMessages.forDay(today, r), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("สุ่มทุกวัน", "เขียนเอง").forEachIndexed { index, label ->
                FilterChip(selected = r.customMode == (index == 1), onClick = { onUpdate(r.copy(customMode = index == 1)) }, label = { Text(label) })
            }
        }
        if (r.customMode) OutlinedTextField(r.message, { onUpdate(r.copy(message = it)) }, Modifier.fillMaxWidth(), placeholder = { Text("ข้อความของคุณ…") }, maxLines = 4, shape = RoundedCornerShape(16.dp))
        Text(state.message ?: if (state.saved) "บันทึกการตั้งค่าแล้ว" else "กำลังบันทึก…", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}
