package com.ntdpz.pie.feature.history

import com.ntdpz.pie.core.domain.diary.DiarySummary
import java.time.LocalDate
import java.time.YearMonth

data class HistoryUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val entries: List<DiarySummary> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
)
