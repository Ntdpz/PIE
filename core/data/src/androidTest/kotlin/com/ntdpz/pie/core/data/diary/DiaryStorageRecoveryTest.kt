package com.ntdpz.pie.core.data.diary

import android.database.sqlite.SQLiteFullException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.domain.diary.DiaryEntry
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.diary.TextFormat
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryStorageRecoveryTest {
    @Test
    fun fullDatabaseKeepsCommittedNoteAndRetriesPendingDraft() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "diary-quota-${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, PieDatabase::class.java, databaseName)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
        try {
            val repository = RoomDiaryRepository(database.diaryDao(), database.mediaDao(), context.cacheDir)
            val clock = Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC)
            val date = LocalDate.of(2026, 11, 8)
            val previous = DiaryEntry(date, "บันทึกที่เขียนไว้แล้ว", clock.instant())
            val draft = previous.copy(
                text = "ข้อความทดสอบ ".repeat(16_384),
                formatting = listOf(TextFormatRange(0, 7, TextFormat.Bold)),
            )
            repository.saveEntry(previous)

            // Limit this fixture database alone. Leave the device's free space untouched.
            val sqlite = database.openHelper.writableDatabase
            val originalLimit = sqlite.maximumSize
            val pageCount = sqlite.query("PRAGMA page_count").use {
                check(it.moveToFirst())
                it.getLong(0)
            }
            val quota = pageCount * sqlite.pageSize
            assertEquals(quota, sqlite.setMaximumSize(quota))
            val error = runCatching { repository.saveEntry(draft) }.exceptionOrNull()
            assertTrue("Fixture must hit SQLite's full condition", error is SQLiteFullException)

            val writer = DiaryWriter(repository, backgroundScope, clock)
            writer.submit(date, draft.text, draft.formatting)
            assertTrue(runCatching { writer.awaitIdle() }.isFailure)
            assertTrue(writer.states.value[date] is SaveState.Failed)
            assertEquals(previous, repository.readEntry(date))
            assertEquals(draft, writer.pendingEntry(date))

            sqlite.setMaximumSize(originalLimit)
            val pending = requireNotNull(writer.pendingEntry(date))
            writer.submit(date, pending.text, pending.formatting)
            writer.awaitIdle()
            assertTrue(writer.states.value[date] is SaveState.Saved)
            assertEquals(draft, repository.readEntry(date))
            assertNull(writer.pendingEntry(date))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
