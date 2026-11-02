package com.ntdpz.pie.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable
fun HistoryRoute(viewModel: HistoryViewModel, onOpenDiary: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onChangeMonth = viewModel::changeMonth,
        onSelectDate = viewModel::selectDate,
        onToday = viewModel::jumpToToday,
        onOpenDiary = { onOpenDiary(state.selectedDate) },
    )
}
