package com.ntdpz.pie.feature.settings.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import com.ntdpz.pie.core.designsystem.motion.piePressScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.domain.backup.DiaryConflict
import com.ntdpz.pie.core.domain.backup.MergeChoice

@Composable
internal fun ImportConflictContent(
    conflict: DiaryConflict,
    index: Int,
    total: Int,
    selected: MergeChoice,
    enabled: Boolean,
    onChoice: (MergeChoice) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Text("วันนี้อยากเก็บแบบไหน?", style = MaterialTheme.typography.headlineLarge)
    Text("${PieDates.entry(conflict.date)} · ${index + 1} จาก $total วัน", color = colors.onSurfaceVariant)
    ImportTextCard("ในเครื่อง", conflict.device.text)
    ImportTextCard("จากไฟล์สำรอง", conflict.backup.text)
    Text("เลือกข้อความที่จะใช้ในวันนี้", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    MergeChoice.entries.forEach { choice ->
        val interactions = remember { MutableInteractionSource() }
        val background by animateColorAsState(if (selected == choice) colors.primaryContainer else colors.surface, label = "Choice color")
        val selectedScale by animateFloatAsState(if (selected == choice) 1f else 0.985f, spring(dampingRatio = 0.75f), label = "Choice emphasis")
        Surface(
            modifier = Modifier.piePressScale(interactions).graphicsLayer { scaleX = selectedScale; scaleY = selectedScale },
            color = background,
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = selected == choice, enabled = enabled,
                    role = Role.RadioButton, onClick = { onChoice(choice) },
                    interactionSource = interactions, indication = LocalIndication.current,
                ).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == choice, onClick = null, enabled = enabled)
                Text(choice.label(), Modifier.padding(start = 12.dp))
            }
        }
    }
    Text(
        when (selected) {
            MergeChoice.KeepDevice -> "ใช้ข้อความเดิมในเครื่อง รูปและวิดีโอที่ยังไม่มีจะเพิ่มให้วันเดิม"
            MergeChoice.UseBackup -> "ใช้ข้อความจากไฟล์สำรองแทนข้อความเดิม รูปและวิดีโอเดิมยังอยู่"
            MergeChoice.KeepBoth -> "ต่อข้อความจากไฟล์สำรองท้ายข้อความเดิม\nมีตัวแบ่งชัดเจน และยังอยู่ในวันเดียวกัน"
        },
        modifier = Modifier.animateContentSize(),
        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
    )
}

@Composable
private fun ImportTextCard(label: String, text: String) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun MergeChoice.label(): String = when (this) {
    MergeChoice.KeepDevice -> "เก็บข้อความในเครื่อง"
    MergeChoice.UseBackup -> "ใช้ข้อความจากไฟล์สำรอง"
    MergeChoice.KeepBoth -> "เก็บทั้งสองข้อความ"
}
