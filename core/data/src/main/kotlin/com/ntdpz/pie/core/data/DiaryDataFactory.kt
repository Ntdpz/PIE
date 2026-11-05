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
import com.ntdpz.pie.core.data.backup.LocalMergeImportRepository
import com.ntdpz.pie.core.data.backup.DatabaseMutationGate
import com.ntdpz.pie.core.domain.backup.MergeImportRepository

data class DiaryData(val repository: DiaryRepository, val media: DiaryMediaRepository, val backup: BackupRepository, val merge: MergeImportRepository)

object DiaryDataFactory {
    fun create(context: Context, settings: SettingsRepository): DiaryData {
        val database = Room.databaseBuilder(
            context.applicationContext,
            PieDatabase::class.java,
            "pie.db",
        ).addMigrations(DatabaseMigrations.From1To2, DatabaseMigrations.From2To3, DatabaseMigrations.From3To4).build()
        val gate = DatabaseMutationGate(database, context.filesDir)
        return DiaryData(
            RoomDiaryRepository(database.diaryDao(), database.mediaDao(), java.io.File(context.filesDir, "diary-media"), gate),
            LocalMediaRepository(context.applicationContext, database, gate),
            LocalBackupRepository(context.applicationContext, database, settings, gate),
            LocalMergeImportRepository(context.applicationContext, database, gate),
        )
    }
}
