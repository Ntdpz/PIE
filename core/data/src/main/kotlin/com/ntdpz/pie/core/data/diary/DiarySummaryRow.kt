package com.ntdpz.pie.core.data.diary

import com.ntdpz.pie.core.domain.diary.DiarySummary
import java.time.Instant
import java.time.LocalDate

internal data class DiarySummaryRow(
    val date: String,
    val preview: String,
    val updatedAtEpochMillis: Long,
)

internal fun DiarySummaryRow.toDomain() = DiarySummary(
    date = LocalDate.parse(date),
    preview = preview,
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)
