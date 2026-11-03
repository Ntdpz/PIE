package com.ntdpz.pie.feature.settings.backup

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.designsystem.motion.piePressScale
import com.ntdpz.pie.core.domain.backup.DiaryConflict
import com.ntdpz.pie.core.domain.backup.MergeChoice

@Composable
internal fun ImportConflictContent(conflict: DiaryConflict, index: Int, total: Int, selected: MergeChoice, enabled: Boolean, onChoice: (MergeChoice) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text("เก็บแบบไหนดี?", style = MaterialTheme.typography.headlineLarge)
    Text("${PieDates.entry(conflict.date)} · ${index + 1} / $total", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    ImportTextCard("ในเครื่อง", conflict.device.text)
    ImportTextCard("จากไฟล์", conflict.backup.text)
    if (LocalDensity.current.fontScale > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MergeChoice.entries.forEach { choice -> ChoiceTile(choice, selected, enabled, Modifier.fillMaxWidth(), onChoice) }
    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MergeChoice.entries.forEach { choice -> ChoiceTile(choice, selected, enabled, Modifier.weight(1f), onChoice) }
    }
    Text(when(selected) {
        MergeChoice.KeepDevice -> "ใช้ข้อความเดิมในเครื่อง"
        MergeChoice.UseBackup -> "แทนข้อความเดิมด้วยข้อความจากไฟล์"
        MergeChoice.KeepBoth -> "ต่อข้อความในวันเดียวกัน"
    }, style = MaterialTheme.typography.bodySmall, color = if (selected == MergeChoice.UseBackup) colors.error else colors.onSurfaceVariant)
}

@Composable
private fun ChoiceTile(choice: MergeChoice, selected: MergeChoice, enabled: Boolean, modifier: Modifier, onChoice: (MergeChoice) -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (choice == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, label = "Choice color")
    Surface(modifier.piePressScale(interactions), color = background, shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.selectable(choice == selected, enabled = enabled, role = Role.RadioButton, onClick = { onChoice(choice) }, interactionSource = interactions, indication = LocalIndication.current).heightIn(min = 60.dp).padding(8.dp), contentAlignment = Alignment.Center) {
            Text(choice.label(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ImportTextCard(label: String, text: String) {
    var expanded by remember(label, text) { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().animateContentSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (text.length > 120 || text.count { it == '\n' } > 2) TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "ย่อ" else "อ่านทั้งหมด") }
        }
    }
}

internal fun MergeChoice.label(): String = when(this) {
    MergeChoice.KeepDevice -> "ในเครื่อง"
    MergeChoice.UseBackup -> "จากไฟล์"
    MergeChoice.KeepBoth -> "เก็บทั้งคู่"
}
