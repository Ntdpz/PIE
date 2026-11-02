package com.ntdpz.pie.feature.home

import com.ntdpz.pie.core.domain.diary.DiaryEntry
import java.time.LocalDate

data class HomeUiState(
    val date: LocalDate,
    val entry: DiaryEntry? = null,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val hasMedia: Boolean = false,
)
