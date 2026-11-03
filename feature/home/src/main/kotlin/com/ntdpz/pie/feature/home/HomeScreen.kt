package com.ntdpz.pie.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.designsystem.motion.pieReveal

@Composable
fun HomeScreen(state: HomeUiState, onWrite: () -> Unit) {
    val hasEntry = !state.entry?.text.isNullOrBlank() || state.hasMedia
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PieBrand()
            Spacer(Modifier.weight(1f))
            Text(PieDates.home(state.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.pieReveal(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (hasEntry) "กลับมาเล่าต่อได้นะ" else "วันนี้เป็นยังไงบ้าง", style = MaterialTheme.typography.headlineLarge)
            if (!hasEntry) Text("เขียนเท่าที่อยากเขียน", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
            when {
                state.loading -> PieLoader("กำลังเปิดหน้าของวันนี้…")
                state.loadFailed -> TextButton(onClick = onWrite) { Text("เปิดบันทึกไม่สำเร็จ · ลองอีกครั้ง") }
                hasEntry -> Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable(onClick = onWrite)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(PieDates.entry(state.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.entry?.text?.takeIf(String::isNotBlank) ?: "ภาพความทรงจำของวันนี้", style = MaterialTheme.typography.bodyLarge, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        Text("เก็บไว้แล้ว", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                else -> Box(Modifier.size(240.dp, 253.dp).clickable(onClickLabel = "เริ่มเขียนวันนี้", onClick = onWrite).pieReveal(1)) {
                    PieJournalPage(Modifier.fillMaxSize())
                    Text("ถึงตัวเราในวันนี้", Modifier.padding(start = 43.dp, top = 55.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        PiePrimaryButton(if (hasEntry) "เขียนต่อ" else "เริ่มเขียน", onWrite, enabled = !state.loading, icon = PieGlyph.Write)
    }
}
