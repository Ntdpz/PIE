package com.ntdpz.pie.feature.settings.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.domain.backup.*
import java.time.LocalDate

@Composable
internal fun ImportOverview(preview: ImportPreview) {
    Text("พบเรื่องเล่าของคุณ", style = MaterialTheme.typography.headlineLarge)
    ImportCounts(preview.addedDays, "วันใหม่", preview.conflicts.size, "วันที่ต้องเลือก")
    HorizontalDivider()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PieIcon(PieGlyph.Media, color = MaterialTheme.colorScheme.primary)
        Text("รูปและวิดีโอใหม่ ${preview.addedMedia} รายการ")
    }
    ImportDetails(listOf(
        "วันใหม่" to "${preview.addedDays} วัน", "เติมวันที่ยังว่าง" to "${preview.filledDays} วัน",
        "ข้อความที่ต้องเลือก" to "${preview.conflicts.size} วัน", "ข้ามข้อความเดิม" to "${preview.skippedDays} วัน",
        "เพิ่มสื่อ" to "${preview.addedMedia} รายการ", "ข้ามสื่อซ้ำ" to "${preview.skippedMedia} รายการ",
    ))
    Text("ยังไม่ได้นำเข้า", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
}

@Composable
internal fun ImportConfirmation(preview: ImportPreview, choices: Map<LocalDate, MergeChoice>) {
    Text("พร้อมรวมบันทึกแล้ว", style = MaterialTheme.typography.headlineLarge)
    PieIcon(PieGlyph.Book, color = MaterialTheme.colorScheme.primary, size = 48.dp)
    ImportRows(listOf(
        "วันใหม่" to "${preview.addedDays} วัน",
        "เก็บทั้งสองข้อความ" to "${choices.values.count { it == MergeChoice.KeepBoth }} วัน",
        "แทนข้อความเดิม" to "${choices.values.count { it == MergeChoice.UseBackup }} วัน",
    ))
    Text("รูปและวิดีโอเดิมยังอยู่", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ImportDetails(listOf(
        "แก้ไขข้อความ" to "${preview.filledDays + choices.values.count { it != MergeChoice.KeepDevice }} วัน",
        "เก็บเดิม / ข้าม" to "${preview.skippedDays + choices.values.count { it == MergeChoice.KeepDevice }} วัน",
        "เพิ่มสื่อ" to "${preview.addedMedia} รายการ", "ข้ามสื่อซ้ำ" to "${preview.skippedMedia} รายการ",
    ))
}

@Composable
internal fun ImportSuccessScreen(result: ImportResult, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Spacer(Modifier.height(24.dp))
            PieSuccessMark()
            Text("อยู่ด้วยกันแล้ว", style = MaterialTheme.typography.headlineLarge)
            Text("นำเข้าบันทึกสำเร็จ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ImportCounts(result.addedDays, "วันใหม่", result.addedMedia, "รูปและวิดีโอ")
            ImportDetails(listOf("แก้ไขข้อความ" to "${result.changedDays} วัน", "ข้ามข้อความเดิม" to "${result.skippedDays} วัน", "ข้ามสื่อซ้ำ" to "${result.skippedMedia} รายการ"))
            PiePrimaryButton("เรียบร้อย", onBack)
        }
    }
}

@Composable
private fun ImportCounts(first: Int, firstLabel: String, second: Int, secondLabel: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf(first to firstLabel, second to secondLabel).forEach { (count, label) ->
            Column(Modifier.weight(1f)) {
                Text(count.toString(), style = MaterialTheme.typography.displayLarge)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ImportRows(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, Modifier.weight(1f))
                Text(value, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDetails(rows: List<Pair<String, String>>) {
    var open by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { open = false }
    TextButton(onClick = { open = true }) { Text("รายละเอียด"); PieIcon(PieGlyph.Next, size = 16.dp) }
    if (open) ModalBottomSheet(onDismissRequest = { open = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("รายละเอียด", style = MaterialTheme.typography.headlineSmall)
            ImportRows(rows)
            Text("PIN ธีม และการแจ้งเตือนใช้ค่าของเครื่องนี้", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
