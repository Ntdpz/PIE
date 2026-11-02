package com.ntdpz.pie.core.data.diary

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DiaryEntity::class], version = 1, exportSchema = true)
internal abstract class PieDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
}
