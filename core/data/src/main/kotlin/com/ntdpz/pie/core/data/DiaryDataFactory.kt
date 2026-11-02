package com.ntdpz.pie.core.data

import android.content.Context
import androidx.room.Room
import com.ntdpz.pie.core.data.diary.PieDatabase
import com.ntdpz.pie.core.data.diary.RoomDiaryRepository
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import com.ntdpz.pie.core.data.media.LocalMediaRepository
import com.ntdpz.pie.core.data.diary.DatabaseMigrations
import com.ntdpz.pie.core.domain.backup.BackupRepository
import com.ntdpz.pie.core.domain.settings.SettingsRepository
import com.ntdpz.pie.core.data.backup.LocalBackupRepository

data class DiaryData(val repository: DiaryRepository, val media: DiaryMediaRepository, val backup: BackupRepository)

object DiaryDataFactory {
    fun create(context: Context, settings: SettingsRepository): DiaryData {
        val database = Room.databaseBuilder(
            context.applicationContext,
            PieDatabase::class.java,
            "pie.db",
        ).addMigrations(DatabaseMigrations.From1To2).build()
        return DiaryData(
            RoomDiaryRepository(database.diaryDao(), database.mediaDao(), java.io.File(context.filesDir, "diary-media")),
            LocalMediaRepository(context.applicationContext, database),
            LocalBackupRepository(context.applicationContext, database, settings),
        )
    }
}
