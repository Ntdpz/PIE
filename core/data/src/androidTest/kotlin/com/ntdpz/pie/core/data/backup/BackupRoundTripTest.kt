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
    @Test fun encryptionAcceptsThaiAndEmojiPasswords() {
        for (value in listOf("รหัสผ่านของฉัน", "backup🙂🔐password", "12345678")) {
            val encrypted = java.io.ByteArrayOutputStream()
            BackupCipher.encrypt("fixture".byteInputStream(), encrypted, value.toCharArray())
            val restored = java.io.ByteArrayOutputStream()
            BackupCipher.decrypt(encrypted.toByteArray().inputStream(), restored, value.toCharArray())
            assertEquals("fixture", restored.toString("UTF-8"))
        }
    }

    @Test
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    fun exportsAndPreviews384MiBThroughDocumentProviderWithoutBufferingWholeArchive() = runTest(timeout = kotlin.time.Duration.parse("5m")) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PieDatabase::class.java).build()
        val settings = TestSettings()
        val gate = DatabaseMutationGate(database, context.filesDir)
        val backups = LocalBackupRepository(context, database, settings, gate)
        val imports = LocalMergeImportRepository(context, database, gate)
        val id = UUID.randomUUID().toString()
        val root = File(context.filesDir, "diary-media").apply { mkdirs() }
        val name = MediaFileNames.forId(id, "video/mp4")
        val owned = File(root, name)
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "PIE-streaming-test-$id.pie")
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
        }
        val destination = requireNotNull(context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            val random = java.util.Random(42)
            val buffer = ByteArray(64 * 1024)
            owned.outputStream().buffered().use { output ->
                repeat(384 * 16) { random.nextBytes(buffer); output.write(buffer) }
            }
            database.diaryDao().upsert(DiaryEntity("2026-11-03", "large backup fixture", 123L))
            database.mediaDao().insert(MediaEntity(id, "2026-11-03", name, "video/mp4", 123L))
            backups.export(destination.toString(), "fixture-password".toCharArray())
            assertNotNull(settings.settings.first().lastBackupAtMillis)
            context.contentResolver.openFileDescriptor(destination, "r")!!.use {
                assertTrue(it.statSize > 384L * 1024 * 1024)
            }
            val preview = imports.prepareImport(destination.toString(), "fixture-password".toCharArray())
            assertEquals(0, preview.addedDays)
            assertEquals(0, preview.addedMedia)
            assertEquals(1, preview.skippedMedia)
            imports.discardImport(preview.token)
        } finally {
            context.contentResolver.delete(destination, null, null)
            owned.delete()
            database.close()
        }
    }
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
            val exportedAt = settings.settings.first().lastBackupAtMillis
            assertNotNull(exportedAt)
            assertTrue(runCatching { backups.export(Uri.fromFile(context.cacheDir).toString(), password) }.isFailure)
            assertEquals(exportedAt, settings.settings.first().lastBackupAtMillis)
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
            assertEquals(exportedAt, settings.settings.first().lastBackupAtMillis)
        } finally {
            password.fill('\u0000')
            archive.delete()
            ownedFile.delete()
            database.close()
        }
    }
}

private class TestSettings : SettingsRepository {
    override suspend fun recordBackupSuccess(atMillis: Long) { settings.value = settings.value.copy(lastBackupAtMillis = atMillis) }
    override val settings = MutableStateFlow(AppSettings(loaded = true, reminder = ReminderSettings(enabled = true)))
    override suspend fun saveReminder(reminder: ReminderSettings) { settings.value = settings.value.copy(reminder = reminder) }
    override suspend fun setPin(pin: CharArray, currentPin: CharArray?) = Unit
    override suspend fun saveTheme(mode: com.ntdpz.pie.core.domain.settings.ThemeMode) { settings.value = settings.value.copy(themeMode = mode) }
    override suspend fun verifyPin(pin: CharArray) = false
    override suspend fun disablePin(pin: CharArray) = false
}
