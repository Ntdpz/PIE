package com.ntdpz.pie.core.domain.diary

import java.time.Instant
import java.time.LocalDate

data class DiarySummary(
    val date: LocalDate,
    val preview: String,
    val updatedAt: Instant,
)
