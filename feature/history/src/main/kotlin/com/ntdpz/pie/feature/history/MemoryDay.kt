package com.ntdpz.pie.feature.history

import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.diary.DiarySummary
import com.ntdpz.pie.core.domain.media.DiaryMedia
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import kotlinx.coroutines.flow.combine

data class MemoryDay(val summary: DiarySummary, val media: List<DiaryMedia>)

internal fun observeMemoryDays(diary: DiaryRepository, media: DiaryMediaRepository) =
    combine(diary.observeSummaries(), media.observeAllMedia()) { entries, attachments ->
        entries.sortedByDescending { it.date }.map { MemoryDay(it, attachments[it.date].orEmpty()) }
    }
