package com.ntdpz.pie.core.domain.diary

import java.time.Instant
import java.time.LocalDate

data class DiaryEntry(
    val date: LocalDate,
    val text: String,
    val updatedAt: Instant,
    val formatting: List<TextFormatRange> = emptyList(),
)
