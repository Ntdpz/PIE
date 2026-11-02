package com.ntdpz.pie.core.domain.diary

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DiaryRepository {
    fun observeEntry(date: LocalDate): Flow<DiaryEntry?>
    fun observeSummaries(): Flow<List<DiarySummary>>
    suspend fun readEntry(date: LocalDate): DiaryEntry?
    suspend fun saveEntry(entry: DiaryEntry)
}
