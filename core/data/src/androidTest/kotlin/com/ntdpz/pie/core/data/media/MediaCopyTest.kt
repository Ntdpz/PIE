package com.ntdpz.pie.core.data.media

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.diary.PieDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID

class MediaCopyTest {
    @Test
    fun importedPhotoSurvivesDeletionOfTheSource() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "pie-test-${UUID.randomUUID()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        val database = Room.inMemoryDatabaseBuilder(context, PieDatabase::class.java).build()
        val repository = LocalMediaRepository(context, database)
        var copied: File? = null
        var sourceDeleted = false
        try {
            resolver.openOutputStream(uri)!!.use {
                Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
                    compress(Bitmap.CompressFormat.PNG, 100, it); recycle()
                }
            }
            val date = LocalDate.of(2026, 11, 2)
            repository.importMedia(date, uri.toString())
            val media = repository.observeMedia(date).first().single()
            copied = File(requireNotNull(Uri.parse(media.uri).path))
            assertTrue(copied.exists())
            resolver.delete(uri, null, null)
            sourceDeleted = true
            assertTrue(copied.exists())
            assertTrue(database.diaryDao().observeSummaries().first().single().hasMedia)
            repository.deleteMedia(media.id)
            assertFalse(copied.exists())
        } finally {
            if (!sourceDeleted) resolver.delete(uri, null, null)
            copied?.delete()
            database.close()
        }
    }
}
