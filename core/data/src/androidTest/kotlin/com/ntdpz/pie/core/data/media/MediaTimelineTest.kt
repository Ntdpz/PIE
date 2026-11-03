package com.ntdpz.pie.core.data.media

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.backup.DatabaseMutationGate
import com.ntdpz.pie.core.data.diary.DiaryEntity
import com.ntdpz.pie.core.data.diary.PieDatabase
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MediaTimelineTest {
    @Test fun allMediaGroupsByDateAndUsesStableOrderWhenTimestampsMatch() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.cacheDir, "timeline-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) { override fun getFilesDir() = directory }
        val db = Room.inMemoryDatabaseBuilder(context, PieDatabase::class.java).build()
        val repository = LocalMediaRepository(context, db, DatabaseMutationGate(db, directory))
        val date = LocalDate.of(2026, 11, 3)
        val first = "00000000-0000-0000-0000-000000000001"
        val second = "00000000-0000-0000-0000-000000000002"
        try {
            db.diaryDao().upsert(DiaryEntity(date.toString(), "fixture", 1))
            for (id in listOf(second, first)) {
                val name = MediaFileNames.forId(id, "image/png")
                File(directory, "diary-media/$name").writeText("fixture")
                db.mediaDao().insert(MediaEntity(id, date.toString(), name, "image/png", 1))
            }
            assertEquals(listOf(first, second), repository.observeAllMedia().first().getValue(date).map { it.id })
            repository.deleteMedia(first)
            assertEquals(listOf(second), repository.observeAllMedia().first().getValue(date).map { it.id })
            db.diaryDao().delete(date.toString())
            assertTrue(repository.observeAllMedia().first().isEmpty())
        } finally { db.close(); directory.deleteRecursively() }
    }
}
