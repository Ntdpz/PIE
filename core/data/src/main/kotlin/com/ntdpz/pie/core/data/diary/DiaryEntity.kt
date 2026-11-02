package com.ntdpz.pie.core.data.diary

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ntdpz.pie.core.domain.diary.DiaryEntry
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "diary_entries")
internal data class DiaryEntity(
    @PrimaryKey val date: String,
    val text: String,
    val updatedAtEpochMillis: Long,
)

internal fun DiaryEntity.toDomain() = DiaryEntry(
    date = LocalDate.parse(date),
    text = text,
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

internal fun DiaryEntry.toEntity() = DiaryEntity(
    date = date.toString(),
    text = text,
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)
