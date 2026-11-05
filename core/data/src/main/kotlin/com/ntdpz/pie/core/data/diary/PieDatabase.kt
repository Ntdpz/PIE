package com.ntdpz.pie.core.data.diary

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ntdpz.pie.core.data.media.MediaDao
import com.ntdpz.pie.core.data.media.MediaEntity

@Database(entities = [DiaryEntity::class, MediaEntity::class, com.ntdpz.pie.core.data.backup.ImportReceipt::class, com.ntdpz.pie.core.data.backup.ImportHistory::class], version = 4, exportSchema = true)
internal abstract class PieDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun mediaDao(): MediaDao
    abstract fun importReceiptDao(): com.ntdpz.pie.core.data.backup.ImportReceiptDao
}
