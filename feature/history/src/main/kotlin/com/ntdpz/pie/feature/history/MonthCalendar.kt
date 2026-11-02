package com.ntdpz.pie.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    entryDates: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = (0 until 42).map { index ->
        (index - offset + 1).takeIf { it in 1..month.lengthOfMonth() }?.let(month::atDay)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("จ", "อ", "พ", "พฤ", "ศ", "ส", "อา").forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDay(date, selectedDate, entryDates, Modifier.weight(1f), onSelectDate)
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    selectedDate: LocalDate,
    entryDates: Set<LocalDate>,
    modifier: Modifier,
    onSelect: (LocalDate) -> Unit,
) {
    val active = date != null && date == selectedDate
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (active) colors.primary else colors.background, tween(PieMotion.QuickMillis), label = "Selected day")
    val foreground by animateColorAsState(if (active) colors.onPrimary else colors.onBackground, tween(PieMotion.QuickMillis), label = "Day ink")
    Box(
        modifier = modifier.heightIn(min = 44.dp)
            .background(background, RoundedCornerShape(12.dp))
            .then(
                if (date == null) Modifier else Modifier
                    .clickable { onSelect(date) }
                    .semantics {
                        selected = active
                        contentDescription = date.toString() +
                            if (date in entryDates) " มีบันทึก" else ""
                    },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfMonth.toString(), color = foreground, style = MaterialTheme.typography.bodySmall)
                if (date in entryDates) Box(Modifier.size(4.dp).background(foreground, CircleShape))
            }
        }
    }
}
