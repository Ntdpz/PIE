package com.ntdpz.pie.core.data

import android.content.Context
import androidx.room.Room
import com.ntdpz.pie.core.data.diary.PieDatabase
import com.ntdpz.pie.core.data.diary.RoomDiaryRepository
import com.ntdpz.pie.core.domain.diary.DiaryRepository

object DiaryDataFactory {
    fun createRepository(context: Context): DiaryRepository {
        val database = Room.databaseBuilder(
            context.applicationContext,
            PieDatabase::class.java,
            "pie.db",
        ).build()
        return RoomDiaryRepository(database.diaryDao())
    }
}
