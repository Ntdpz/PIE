package com.ntdpz.pie.core.data.media

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ntdpz.pie.core.data.diary.DiaryEntity

@Entity(
    tableName = "diary_media",
    foreignKeys = [ForeignKey(
        entity = DiaryEntity::class, parentColumns = ["date"], childColumns = ["date"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("date")],
)
internal data class MediaEntity(
    @PrimaryKey val id: String,
    val date: String,
    val fileName: String,
    val mimeType: String,
    val createdAtEpochMillis: Long,
)
