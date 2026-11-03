package com.ntdpz.pie.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.domain.settings.ThemeMode

@Composable
internal fun AppearancePicker(mode: ThemeMode, enabled: Boolean, onSelect: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("หน้าตาแอป", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System).forEach { option ->
                Surface(
                    onClick = { onSelect(option) }, enabled = enabled,
                    modifier = Modifier.weight(1f).semantics { selected = mode == option },
                    shape = RoundedCornerShape(18.dp),
                    color = if (mode == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PieIcon(when(option) { ThemeMode.Light -> PieGlyph.Sun; ThemeMode.Dark -> PieGlyph.Moon; ThemeMode.System -> PieGlyph.Device }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when(option) { ThemeMode.Light -> "สว่าง"; ThemeMode.Dark -> "มืด"; ThemeMode.System -> "ตามเครื่อง" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
