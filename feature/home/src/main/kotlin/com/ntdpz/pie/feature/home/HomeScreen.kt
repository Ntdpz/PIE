package com.ntdpz.pie.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.PieBrand
import com.ntdpz.pie.core.designsystem.component.PiePrimaryButton
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: HomeUiState, onWrite: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PieBrand()
        Text(
            state.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("th"))),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("วันนี้เป็นยังไงบ้าง?", style = MaterialTheme.typography.headlineMedium)
        Text("จะบ่น จะเล่า หรือแค่อยากพักใจ\nตรงนี้เขียนได้ทุกอย่างเลย")
        if (state.loading) CircularProgressIndicator()
        if (state.loadFailed) Text("เปิดบันทึกไม่สำเร็จ ลองเปิดหน้าของวันนี้อีกครั้ง")
        AnimatedVisibility(visible = !state.entry?.text.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("หน้าของวันนี้", style = MaterialTheme.typography.titleMedium)
                    Text(state.entry?.text.orEmpty(), maxLines = 3)
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("มีเรื่องอะไรในใจ\nมาเล่าให้ฟังหน่อย", style = MaterialTheme.typography.headlineSmall)
                Text("ไม่ต้องเรียบเรียงให้ดี แค่เริ่มจากสิ่งที่อยู่ในใจ")
                PiePrimaryButton(
                    label = if (state.entry?.text.isNullOrBlank()) "เริ่มเขียนวันนี้" else "เขียนต่อวันนี้",
                    onClick = onWrite,
                    enabled = !state.loading,
                )
            }
        }
    }
}
