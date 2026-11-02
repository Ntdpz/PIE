package com.ntdpz.pie.core.data.diary

import com.ntdpz.pie.core.domain.diary.DiaryEntry
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.io.File
import com.ntdpz.pie.core.data.media.MediaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RoomDiaryRepository(private val dao: DiaryDao, private val media: MediaDao, private val mediaRoot: File) : DiaryRepository {
    override fun observeEntry(date: LocalDate) =
        dao.observeEntry(date.toString()).map { it?.toDomain() }

    override fun observeSummaries() =
        dao.observeSummaries().map { entries -> entries.map(DiarySummaryRow::toDomain) }

    override suspend fun readEntry(date: LocalDate) =
        dao.readEntry(date.toString())?.toDomain()

    override suspend fun saveEntry(entry: DiaryEntry) = dao.upsert(entry.toEntity())
    override suspend fun deleteEntry(date: LocalDate) = withContext(Dispatchers.IO) {
        val owned = media.readForDate(date.toString())
        dao.delete(date.toString())
        owned.forEach { File(mediaRoot, it.fileName).delete() }
    }
}
