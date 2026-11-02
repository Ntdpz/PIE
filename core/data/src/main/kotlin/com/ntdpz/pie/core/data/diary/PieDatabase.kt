package com.ntdpz.pie.core.data.diary

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ntdpz.pie.core.data.media.MediaDao
import com.ntdpz.pie.core.data.media.MediaEntity

@Database(entities = [DiaryEntity::class, MediaEntity::class], version = 2, exportSchema = true)
internal abstract class PieDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun mediaDao(): MediaDao
}
