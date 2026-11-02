package com.ntdpz.pie.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.designsystem.motion.pieReveal
import com.ntdpz.pie.core.designsystem.motion.pieSheen
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(state: HomeUiState, onWrite: () -> Unit) {
    val hasEntry = !state.entry?.text.isNullOrBlank() || state.hasMedia
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val invitationHeight = if (hasEntry) (maxHeight - 420.dp).coerceAtLeast(280.dp) else 430.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
                start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth().pieReveal(), verticalAlignment = Alignment.CenterVertically) {
                PieBrand()
                Spacer(Modifier.weight(1f))
                Text(
                    PieDates.home(state.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.5f),
                )
            }
            Column(Modifier.pieReveal(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("วันนี้เป็นยังไงบ้าง?", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "จะบ่น จะเล่า หรือแค่อยากพักใจ\nตรงนี้เขียนได้ทุกอย่างเลย",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.loadFailed) Text("เปิดบันทึกไม่สำเร็จ ลองเปิดหน้าของวันนี้อีกครั้ง")
            if (hasEntry) TodayPreview(state)
            WritingInvitation(hasEntry, invitationHeight, !state.loading, onWrite)
        }
    }
}

@Composable
private fun TodayPreview(state: HomeUiState) {
    Surface(modifier = Modifier.pieReveal(2), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PieIcon(PieGlyph.Book, color = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("หน้าของวันนี้", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(
                    state.entry?.updatedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(state.entry?.text?.takeIf { it.isNotBlank() } ?: "ภาพความทรงจำของวันนี้", maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PieIcon(PieGlyph.Check, color = Color(0xFF466D59), size = 16.dp)
                Text("บันทึกแล้ว", style = MaterialTheme.typography.labelSmall, color = Color(0xFF466D59))
            }
        }
    }
}

@Composable
private fun WritingInvitation(
    hasEntry: Boolean,
    height: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onWrite: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().heightIn(min = height).pieReveal(2),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            Modifier.pieSheen().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PieIcon(PieGlyph.Invitation, color = MaterialTheme.colorScheme.primary, size = 32.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (hasEntry) "ยังมีเรื่องในใจอีกไหม\nเล่าต่อได้เสมอนะ" else "วันนี้มีเรื่องอะไร\nมาเล่าให้ฟังหน่อย",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    if (hasEntry) "เขียนต่อในหน้าของวันนี้ได้เลย" else "ไม่ต้องเรียบเรียงให้ดี\nแค่เริ่มจากสิ่งที่อยู่ในใจ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            PiePrimaryButton(
                label = if (hasEntry) "เขียนต่อวันนี้" else "เริ่มเขียนวันนี้",
                onClick = onWrite,
                enabled = enabled,
                icon = PieGlyph.Write,
            )
        }
    }
}
