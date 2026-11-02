package com.ntdpz.pie.feature.settings.reminder

import androidx.compose.foundation.clickable
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
fun ReminderScreen(
    state: SettingsUiState,
    today: LocalDate,
    onBack: () -> Unit,
    onUpdate: (ReminderSettings) -> Unit,
    onEnable: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEditTime: (Int?) -> Unit,
) {
    val r = state.settings.reminder
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับ") }
            Text("แจ้งเตือน", style = MaterialTheme.typography.headlineSmall)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ชวนกลับมาเขียน", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(r.enabled, onEnable, enabled = state.settings.loaded)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("เตือนวันไหนบ้าง", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("จ", "อ", "พ", "พฤ", "ศ", "ส", "อา").forEachIndexed { index, label ->
                    val day = index + 1
                    val selected = day in r.days
                    Surface(
                        onClick = {
                            val next = if (selected) r.days - day else r.days + day
                            onUpdate(r.copy(days = next, enabled = r.enabled && next.isNotEmpty()))
                        },
                        modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp),
                        color = if (selected) colors.primaryContainer else colors.surface,
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) colors.primary else colors.onSurfaceVariant) }
                    }
                }
            }
            r.times.sorted().forEach { time ->
                Surface(color = colors.surface, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PieIcon(PieGlyph.Clock, color = colors.primary)
                        Text("%02d:%02d".format(time / 60, time % 60), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = { onEditTime(time) }) { Text("เปลี่ยน", style = MaterialTheme.typography.bodySmall) }
                        if (r.times.size > 1) IconButton(onClick = { onUpdate(r.copy(times = r.times - time)) }) { PieIcon(PieGlyph.Close, "ลบเวลา", size = 18.dp) }
                    }
                }
            }
            TextButton(onClick = { onEditTime(null) }, contentPadding = PaddingValues(0.dp)) {
                PieIcon(PieGlyph.Plus, color = colors.primary, size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text("เพิ่มเวลาแจ้งเตือน", style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ข้อความที่จะได้รับ", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Surface(color = colors.surface, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().height(52.dp).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ให้ PIE เลือกให้", "เขียนข้อความเอง").forEachIndexed { index, label ->
                        val selected = r.customMode == (index == 1)
                        Surface(
                            onClick = { onUpdate(r.copy(customMode = index == 1)) },
                            modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(10.dp),
                            color = if (selected) colors.primaryContainer else colors.surface,
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.bodySmall, color = if (selected) colors.primary else colors.onSurfaceVariant) }
                        }
                    }
                }
            }
            if (r.customMode) OutlinedTextField(
                r.message, { onUpdate(r.copy(message = it)) }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ข้อความของคุณ…") }, shape = RoundedCornerShape(12.dp),
            )
            Surface(color = colors.surface, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PieIcon(PieGlyph.Bell, color = colors.primary, size = 18.dp)
                        Text("PIE · ตัวอย่างแจ้งเตือน", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Text(ReminderMessages.forDay(today, r))
                }
            }
            Text("สุ่มข้อความใหม่แต่ละวัน\nหากตั้งหลายเวลา จะใช้ข้อความเดียวกันในวันนั้น", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        PiePrimaryButton("ทดสอบแจ้งเตือน", onTest, icon = PieGlyph.Ring, enabled = state.settings.loaded)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PieIcon(PieGlyph.Check, color = colors.primary, size = 16.dp)
            Text(state.message ?: if (state.saved) "บันทึกการตั้งค่าแล้ว" else "กำลังบันทึก…", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}
