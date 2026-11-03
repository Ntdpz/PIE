package com.ntdpz.pie.feature.settings.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.domain.backup.ImportPreview
import com.ntdpz.pie.core.domain.backup.ImportResult
import com.ntdpz.pie.core.domain.backup.MergeChoice
import java.time.LocalDate

@Composable
internal fun ImportOverview(preview: ImportPreview) {
    Text("ตรวจรายการก่อนนำเข้า", style = MaterialTheme.typography.headlineLarge)
    Text("เรื่องเล่าจากไฟล์สำรอง\nพร้อมกลับมาอยู่ด้วยกัน", color = MaterialTheme.colorScheme.onSurfaceVariant)
    ImportSummaryCard(listOf(
        "เพิ่มวันใหม่" to "${preview.addedDays} วัน",
        "เติมวันที่ยังว่าง" to "${preview.filledDays} วัน",
        "ข้อความต่างกัน" to "${preview.conflicts.size} วัน",
        "ข้ามข้อความเดิม" to "${preview.skippedDays} วัน",
        "เพิ่มรูปและวิดีโอ" to "${preview.addedMedia} รายการ",
        "ข้ามสื่อซ้ำ" to "${preview.skippedMedia} รายการ",
    ))
    ImportNotice("ข้อมูลเดิมยังอยู่", "วันที่ข้อความต่างกันจะเก็บข้อความในเครื่องไว้ก่อน คุณเลือกเปลี่ยนแยกแต่ละวันได้")
    Text("รูปและวิดีโอที่ยังไม่มีจะเพิ่มให้วันเดิม\nยังไม่มีการนำเข้า จนกว่าคุณจะยืนยัน", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ImportConfirmation(preview: ImportPreview, choices: Map<LocalDate, MergeChoice>) {
    Text("รวมเรื่องเล่าเข้าด้วยกัน", style = MaterialTheme.typography.headlineLarge)
    Text("ตรวจอีกนิด ก่อนเก็บทุกเรื่องไว้ใน PIE", color = MaterialTheme.colorScheme.onSurfaceVariant)
    ImportSummaryCard(listOf(
        "เพิ่มวันใหม่" to "${preview.addedDays} วัน",
        "แก้ไขข้อความ" to "${preview.filledDays + choices.values.count { it != MergeChoice.KeepDevice }} วัน",
        "เก็บข้อความเดิม / ข้าม" to "${preview.skippedDays + choices.values.count { it == MergeChoice.KeepDevice }} วัน",
        "เพิ่มรูปและวิดีโอ" to "${preview.addedMedia} รายการ",
        "ข้ามสื่อซ้ำ" to "${preview.skippedMedia} รายการ",
    ))
    if (choices.isNotEmpty()) ImportSummaryCard(MergeChoice.entries.map { it.label() to "${choices.values.count { choice -> choice == it }} วัน" })
    ImportNotice("ค่าของเครื่องนี้ยังเหมือนเดิม", "PIN ธีม และการแจ้งเตือนจะไม่เปลี่ยน\nสื่อเดิมจะไม่ถูกลบ")
    Text("ข้อความที่เคยนำเข้าแล้วจะข้าม เพื่อไม่ให้ต่อข้อความเดิมซ้ำ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ImportSuccessScreen(result: ImportResult, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            PieIcon(PieGlyph.Check, color = MaterialTheme.colorScheme.primary, size = 56.dp)
            Text("เรื่องเล่ากลับมาอยู่ด้วยกันแล้ว", style = MaterialTheme.typography.headlineLarge)
            Text("นำเข้าบันทึกเรียบร้อย\nกลับไปเปิดดูวันเก่า ๆ ได้เลย", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ImportSummaryCard(listOf(
                "เพิ่มวันใหม่" to "${result.addedDays} วัน",
                "แก้ไขข้อความ" to "${result.changedDays} วัน",
                "ข้ามข้อความเดิม" to "${result.skippedDays} วัน",
                "เพิ่มรูปและวิดีโอ" to "${result.addedMedia} รายการ",
                "ข้ามสื่อซ้ำ" to "${result.skippedMedia} รายการ",
            ))
            Text("PIN ธีม และการแจ้งเตือนยังใช้ค่าของเครื่องนี้", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            PiePrimaryButton("กลับไปตั้งค่า", onBack)
        }
    }
}

@Composable
private fun ImportSummaryCard(rows: List<Pair<String, String>>) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ImportNotice(title: String, description: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
