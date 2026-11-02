package com.ntdpz.pie.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.motion.pieReveal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, versionName: String, onAction: (SettingsAction) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshRestoreAvailability()
        onPauseOrDispose { }
    }
    SettingsScreen(state, versionName, onAction)
}

@Composable
fun SettingsScreen(state: SettingsUiState, versionName: String, onAction: (SettingsAction) -> Unit) {
    var pinOptions by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { pinOptions = false }
    val reminder = state.settings.reminder
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("ตั้งค่า", modifier = Modifier.pieReveal(), style = MaterialTheme.typography.headlineLarge)
        Text("ความเป็นส่วนตัว และบันทึกของคุณ", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.pieReveal(1)) {
            SectionLabel("ความเป็นส่วนตัว")
            SettingRow(PieGlyph.Lock, "ล็อกด้วย PIN", if (state.settings.pinEnabled) "เปิดอยู่ · เปลี่ยนหรือปิด PIN" else "ตั้ง PIN 6 หลักเพื่อเปิดแอป") {
                if (state.settings.pinEnabled) pinOptions = true else onAction(SettingsAction.SetupPin)
            }
            val summary = if (reminder.enabled) {
                (if (reminder.days.size == 7) "ทุกวัน" else "${reminder.days.size} วัน/สัปดาห์") +
                    " · " + reminder.times.sorted().joinToString(", ") { "%02d:%02d".format(it / 60, it % 60) }
            } else "ปิดอยู่ · ตั้งวันและเวลาได้"
            SettingRow(PieGlyph.Bell, "แจ้งเตือนให้เขียน", summary) { onAction(SettingsAction.Reminders) }
        }
        Column(Modifier.pieReveal(2)) {
            SectionLabel("บันทึกของคุณ")
            SettingRow(PieGlyph.Download, "ส่งออกไฟล์สำรอง", "ตั้งรหัสผ่านไฟล์สำรองก่อนส่งออก") { onAction(SettingsAction.ExportBackup) }
            if (state.canRestore) SettingRow(PieGlyph.Archive, "นำเข้าไฟล์สำรอง", "สำหรับแอปที่ยังไม่มีข้อมูล") { onAction(SettingsAction.RestoreBackup) }
        }
        Surface(modifier = Modifier.pieReveal(3), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onBackground, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PieIcon(PieGlyph.Storage, color = MaterialTheme.colorScheme.primary)
                Text("เรื่องของคุณ อยู่กับคุณ", style = MaterialTheme.typography.titleMedium)
                Text("เก็บไว้บนเครื่องนี้ ยังไม่ซิงก์ข้ามเครื่อง\nส่งออกไฟล์สำรองเพื่อเก็บหรือย้ายข้อมูล", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Text("PIE · V$versionName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (pinOptions) AlertDialog(
        onDismissRequest = { pinOptions = false },
        title = { Text("ล็อกด้วย PIN") },
        text = {
            Column {
                TextButton(onClick = { pinOptions = false; onAction(SettingsAction.ChangePin) }) { Text("เปลี่ยน PIN") }
                TextButton(onClick = { pinOptions = false; onAction(SettingsAction.DisablePin) }) { Text("ปิด PIN") }
            }
        },
        confirmButton = { TextButton(onClick = { pinOptions = false }) { Text("ยกเลิก") } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingRow(glyph: PieGlyph, title: String, detail: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            PieIcon(glyph, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PieIcon(PieGlyph.Next, color = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
