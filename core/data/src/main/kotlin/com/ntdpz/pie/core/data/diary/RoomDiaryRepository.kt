package com.ntdpz.pie.core.data.diary

import com.ntdpz.pie.core.domain.diary.DiaryEntry
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate

internal class RoomDiaryRepository(private val dao: DiaryDao) : DiaryRepository {
    override fun observeEntry(date: LocalDate) =
        dao.observeEntry(date.toString()).map { it?.toDomain() }

    override fun observeSummaries() =
        dao.observeSummaries().map { entries -> entries.map(DiarySummaryRow::toDomain) }

    override suspend fun readEntry(date: LocalDate) =
        dao.readEntry(date.toString())?.toDomain()

    override suspend fun saveEntry(entry: DiaryEntry) = dao.upsert(entry.toEntity())
}
