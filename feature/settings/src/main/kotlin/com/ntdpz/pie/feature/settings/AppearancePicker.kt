package com.ntdpz.pie.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.domain.settings.ThemeMode

@Composable
internal fun AppearancePicker(mode: ThemeMode, enabled: Boolean, onSelect: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("รูปแบบแอป", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = mode == option, onClick = { onSelect(option) }, enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                ) {
                    Text(when (option) {
                        ThemeMode.System -> "ตามระบบ"
                        ThemeMode.Light -> "สว่าง"
                        ThemeMode.Dark -> "มืด"
                    }, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
