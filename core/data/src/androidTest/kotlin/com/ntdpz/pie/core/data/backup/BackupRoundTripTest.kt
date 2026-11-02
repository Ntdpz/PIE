package com.ntdpz.pie.core.data.backup

import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.diary.*
import com.ntdpz.pie.core.data.media.*
import com.ntdpz.pie.core.domain.settings.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class BackupRoundTripTest {
    @Test
    fun encryptedBackupRestoresTextMediaAndDisabledReminders() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PieDatabase::class.java).build()
        val settings = TestSettings()
        val backups = LocalBackupRepository(context, database, settings)
        val archive = File.createTempFile("round-trip-", ".pie", context.cacheDir)
        val id = UUID.randomUUID().toString()
        val root = File(context.filesDir, "diary-media").apply { mkdirs() }
        val name = MediaFileNames.forId(id, "image/png")
        val ownedFile = File(root, name)
        val bytes = "fixture image bytes".toByteArray()
        val password = "backup-password".toCharArray()
        try {
            database.diaryDao().upsert(DiaryEntity("2026-11-02", "ข้อความ 🙂 #test", 123L))
            ownedFile.writeBytes(bytes)
            database.mediaDao().insert(MediaEntity(id, "2026-11-02", name, "image/png", 123L))
            val uri = Uri.fromFile(archive).toString()
            backups.export(uri, password)
            assertFalse(archive.readBytes().contentEquals(bytes))
            assertFalse(backups.canRestore())
            assertTrue(runCatching { backups.restore(uri, password) }.isFailure)
            assertEquals("ข้อความ 🙂 #test", database.diaryDao().readEntry("2026-11-02")?.text)
            database.diaryDao().delete("2026-11-02")
            ownedFile.delete()
            assertTrue(backups.canRestore())
            assertTrue(runCatching { backups.restore(uri, "wrong-password".toCharArray()) }.isFailure)
            assertEquals(0, database.diaryDao().count())
            backups.restore(uri, password)
            assertEquals("ข้อความ 🙂 #test", database.diaryDao().readEntry("2026-11-02")?.text)
            assertEquals(id, database.mediaDao().readAll().single().id)
            assertArrayEquals(bytes, ownedFile.readBytes())
            assertFalse(settings.settings.first().reminder.enabled)
        } finally {
            password.fill('\u0000')
            archive.delete()
            ownedFile.delete()
            database.close()
        }
    }
}

private class TestSettings : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings(loaded = true, reminder = ReminderSettings(enabled = true)))
    override suspend fun saveReminder(reminder: ReminderSettings) { settings.value = settings.value.copy(reminder = reminder) }
    override suspend fun setPin(pin: CharArray, currentPin: CharArray?) = Unit
    override suspend fun verifyPin(pin: CharArray) = false
    override suspend fun disablePin(pin: CharArray) = false
}
