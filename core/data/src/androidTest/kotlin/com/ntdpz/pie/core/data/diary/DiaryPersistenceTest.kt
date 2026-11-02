package com.ntdpz.pie.core.data.diary

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryPersistenceTest {
    @Test
    fun upsertKeepsOnePagePerDayAndSurvivesDatabaseReopen() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, PieDatabase::class.java, name).build()
        try {
            val first = open()
            first.diaryDao().upsert(DiaryEntity("2026-11-02", "first", 1L))
            first.diaryDao().upsert(DiaryEntity("2026-11-02", "latest", 2L))
            first.close()
            val reopened = open()
            assertEquals("latest", reopened.diaryDao().readEntry("2026-11-02")?.text)
            reopened.close()
        } finally {
            context.deleteDatabase(name)
        }
    }
}
