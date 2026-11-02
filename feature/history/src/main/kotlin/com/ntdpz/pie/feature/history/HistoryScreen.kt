package com.ntdpz.pie.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import com.ntdpz.pie.core.designsystem.motion.pieReveal
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import java.time.LocalDate

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onChangeMonth: (Long) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onOpenDiary: () -> Unit,
) {
    val entry = state.entries.firstOrNull { it.date == state.selectedDate }
    val today = state.selectedDate == state.today
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ย้อนวัน", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            FilledTonalButton(onClick = onToday, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
                Text("วันนี้", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChangeMonth(-1) }) { PieIcon(PieGlyph.Previous, "เดือนก่อน", color = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) }
            Text(PieDates.month(state.month), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            IconButton(onClick = { onChangeMonth(1) }) { PieIcon(PieGlyph.Next, "เดือนถัดไป", color = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) }
        }
        AnimatedContent(
            targetState = state.month,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (fadeIn(tween(PieMotion.GentleMillis)) + slideInHorizontally(tween(PieMotion.GentleMillis, easing = PieMotion.Emphasized)) { it / 8 * direction }) togetherWith
                    (fadeOut(tween(PieMotion.QuickMillis)) + slideOutHorizontally(tween(PieMotion.GentleMillis)) { -it / 8 * direction })
            },
            label = "Calendar month",
        ) { month ->
            MonthCalendar(month, state.selectedDate, state.entries.map { it.date }.toSet(), onSelectDate)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Text("วันที่มีบันทึก", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.loading) PieLoader("กำลังเปิดเรื่องเล่า…", compact = true)
        if (state.loadFailed) Text("เปิดรายการบันทึกไม่สำเร็จ")
        Surface(modifier = Modifier.pieReveal(1), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(PieDates.shortEntry(state.selectedDate), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (entry?.hasMedia == true) PieIcon(PieGlyph.Media, color = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                }
                Text(
                    entry?.preview?.takeIf(String::isNotBlank) ?: if (entry?.hasMedia == true) "ภาพความทรงจำของวันนี้" else "วันนี้ยังไม่มีเรื่องเล่า",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                PiePrimaryButton(
                    if (entry == null) "เขียนบันทึกวันนี้" else if (today) "เปิดบันทึกวันนี้" else "เปิดบันทึก",
                    onOpenDiary, enabled = !state.loading && !state.loadFailed, icon = PieGlyph.Book,
                )
            }
        }
    }
}
