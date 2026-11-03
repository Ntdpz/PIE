package com.ntdpz.pie.core.domain.media

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DiaryMediaRepository {
    fun observeAllMedia(): Flow<Map<LocalDate, List<DiaryMedia>>>
    fun observeMedia(date: LocalDate): Flow<List<DiaryMedia>>
    suspend fun importMedia(date: LocalDate, uri: String)
    suspend fun deleteMedia(id: String)
}
