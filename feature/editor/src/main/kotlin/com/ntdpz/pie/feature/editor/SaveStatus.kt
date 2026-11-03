package com.ntdpz.pie.feature.editor

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.PieGlyph
import com.ntdpz.pie.core.designsystem.component.PieIcon
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import com.ntdpz.pie.core.domain.diary.SaveState

private enum class SavePhase { Idle, Saving, Saved, Failed }

@Composable
internal fun SaveStatus(state: SaveState, onRetry: () -> Unit) {
    // Revisions are intentionally excluded: typing must not restart the animation.
    val phase = when (state) {
        SaveState.Idle -> SavePhase.Idle
        is SaveState.Saving -> SavePhase.Saving
        is SaveState.Saved -> SavePhase.Saved
        is SaveState.Failed -> SavePhase.Failed
    }
    Crossfade(
        targetState = phase,
        animationSpec = tween(PieMotion.QuickMillis),
        label = "Save status",
    ) { displayed ->
        val color = when (displayed) {
            SavePhase.Failed -> MaterialTheme.colorScheme.error
            SavePhase.Saved -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Row(
            Modifier.then(if (displayed == SavePhase.Failed) Modifier.clickable(onClick = onRetry) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (displayed == SavePhase.Saved) PieIcon(PieGlyph.Check, color = color, size = 14.dp)
            Text(
                when (displayed) {
                    SavePhase.Idle -> "บันทึกอัตโนมัติ"
                    SavePhase.Saving -> "กำลังบันทึก…"
                    SavePhase.Saved -> "บันทึกแล้ว"
                    SavePhase.Failed -> "บันทึกไม่สำเร็จ · ลองอีกครั้ง"
                },
                style = MaterialTheme.typography.labelSmall, color = color,
            )
        }
    }
}
