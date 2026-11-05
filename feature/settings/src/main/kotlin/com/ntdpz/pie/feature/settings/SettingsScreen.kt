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
import com.ntdpz.pie.core.domain.settings.ThemeMode

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, versionName: String, onAction: (SettingsAction) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshRestoreAvailability()
        onPauseOrDispose { }
    }
    SettingsScreen(state, versionName, onAction, viewModel::saveTheme)
}

@Composable
fun SettingsScreen(state: SettingsUiState, versionName: String, onAction: (SettingsAction) -> Unit, onThemeChange: (ThemeMode) -> Unit) {
    var pinOptions by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { pinOptions = false }
    val reminder = state.settings.reminder
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("พื้นที่ของคุณ", modifier = Modifier.pieReveal(), style = MaterialTheme.typography.headlineLarge)
        Column(Modifier.pieReveal(1), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel("เก็บเรื่องเล่า")
            Spacer(Modifier.height(8.dp))
            DriveBackupCard { onAction(SettingsAction.GoogleDrive) }
            Spacer(Modifier.height(8.dp))
            val backupTime = state.settings.lastBackupAtMillis?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", java.util.Locale.forLanguageTag("th-TH")))
            }
            SettingRow(PieGlyph.Download, "ส่งออกไฟล์", backupTime?.let { "ล่าสุด $it" } ?: "ยังไม่ได้ส่งออก") { onAction(SettingsAction.ExportBackup) }
            SettingRow(PieGlyph.Archive, "นำเข้าไฟล์", "ตรวจรายการก่อนรวม") {
                onAction(SettingsAction.RestoreBackup)
            }
            state.lastImportAtMillis?.let { timestamp ->
                val label = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", java.util.Locale.forLanguageTag("th-TH")))
                Text("นำเข้าสำเร็จล่าสุด $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.pieReveal(2), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel("ส่วนตัวและการใช้งาน")
            Spacer(Modifier.height(8.dp))
            SettingRow(PieGlyph.Lock, "รหัส PIN", if (state.settings.pinEnabled) "เปิดอยู่" else "ปิดอยู่") {
                if (state.settings.pinEnabled) pinOptions = true else onAction(SettingsAction.SetupPin)
            }
            val summary = if (reminder.enabled) {
                (if (reminder.days.size == 7) "ทุกวัน" else "${reminder.days.size} วัน/สัปดาห์") +
                    " · " + reminder.times.sorted().joinToString(", ") { "%02d:%02d".format(it / 60, it % 60) }
            } else "ปิดอยู่"
            SettingRow(PieGlyph.Bell, "แจ้งเตือน", summary) { onAction(SettingsAction.Reminders) }
        }
        AppearancePicker(state.settings.themeMode, state.settings.loaded, onThemeChange)
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Text("PIE · V$versionName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (pinOptions) PieDialog(
        onDismissRequest = { pinOptions = false },
        title = "ล็อกด้วย PIN",
        text = {
            Column {
                PinOption(PieGlyph.Lock, "เปลี่ยน PIN") { pinOptions = false; onAction(SettingsAction.ChangePin) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PinOption(PieGlyph.Close, "ปิด PIN") { pinOptions = false; onAction(SettingsAction.DisablePin) }
            }
        },
        confirmButton = { TextButton(onClick = { pinOptions = false }) { Text("ยกเลิก") } },
    )
}

@Composable
private fun PinOption(glyph: PieGlyph, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PieIcon(glyph, color = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingRow(glyph: PieGlyph, title: String, detail: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            PieIcon(glyph, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PieIcon(PieGlyph.Next, color = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
        }
    }
}

@Composable
private fun DriveBackupCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = colors.primaryContainer) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surface) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    PieIcon(PieGlyph.CloudBackup, color = colors.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Google Drive", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                    Text("สำรองข้อมูลไว้กับบัญชีของคุณ", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                    Text("เลือกที่เก็บเมื่อสำรอง", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(24.dp), color = colors.primary) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        PieIcon(PieGlyph.ArrowUpRight, color = colors.onPrimary)
                    }
                }
            }
        }
    }
}
