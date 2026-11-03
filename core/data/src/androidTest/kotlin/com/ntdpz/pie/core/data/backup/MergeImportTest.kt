package com.ntdpz.pie.core.data.backup

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.diary.*
import com.ntdpz.pie.core.data.media.*
import com.ntdpz.pie.core.domain.backup.*
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class MergeImportTest {
    private val date = LocalDate.of(2026, 11, 3)
    private fun note(text: String, day: LocalDate = date) = DiaryEntity(day.toString(), text, 100L)
    private fun media(day: LocalDate = date, id: String = UUID.randomUUID().toString(), mime: String = "image/png") =
        MediaEntity(id, day.toString(), MediaFileNames.forId(id, mime), mime, 100L)

    @Test fun keepBothAndMediaDedupAreIdempotentAcrossRepositoryRecreation() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("local"))
            val owned = media()
            f.addMedia(owned, "same bytes")
            val duplicate = media()
            val collision = media(id = owned.id, mime = "video/mp4")
            val newDay = date.plusDays(1)
            val newPhoto = media(newDay)
            val archive = f.archive(listOf(note("incoming"), note("new day", newDay)), listOf(duplicate to "same bytes", collision to "different video", newPhoto to "new bytes"))
            val preview = f.prepare(archive)
            assertEquals(1, preview.addedDays)
            assertEquals(1, preview.conflicts.size)
            assertEquals(2, preview.addedMedia)
            assertEquals(1, preview.skippedMedia)
            val result = f.imports.commitImport(preview.token, mapOf(date to MergeChoice.KeepBoth))
            assertEquals(1, result.changedDays)
            assertEquals(2, f.db.diaryDao().count())
            assertEquals(3, f.db.mediaDao().readAll().size)
            val merged = f.db.diaryDao().readEntry(date.toString())!!.text
            assertTrue(merged.startsWith("local"))
            assertTrue(merged.endsWith("incoming"))
            assertEquals("same bytes", File(f.root, owned.fileName).readText())
            assertNotNull(f.imports.lastImportAtMillis.first())
            // Same plaintext in another encrypted file still has the same imported content identity.
            f.recreateImports()
            val again = f.prepare(archive)
            assertTrue(again.conflicts.isEmpty())
            assertEquals(0, again.addedMedia)
            val repeated = f.imports.commitImport(again.token, emptyMap())
            assertEquals(0, repeated.changedDays)
            assertEquals(merged, f.db.diaryDao().readEntry(date.toString())!!.text)
            assertEquals(3, f.db.mediaDao().readAll().size)
        }
    }

    @Test fun keepingDeviceCanBeReconsideredAndReplacingDoesNotDeleteMedia() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("device"))
            val owned = media()
            f.addMedia(owned, "keep photo")
            val archive = f.archive(listOf(note("backup")))
            val first = f.prepare(archive)
            f.imports.commitImport(first.token, mapOf(date to MergeChoice.KeepDevice))
            assertEquals("device", f.db.diaryDao().readEntry(date.toString())!!.text)
            val second = f.prepare(archive)
            assertEquals(1, second.conflicts.size)
            f.imports.commitImport(second.token, mapOf(date to MergeChoice.UseBackup))
            assertEquals("backup", f.db.diaryDao().readEntry(date.toString())!!.text)
            assertEquals("keep photo", File(f.root, owned.fileName).readText())
            assertEquals(owned, f.db.mediaDao().read(owned.id))
        }
    }

    @Test fun stalePreviewCannotOverwriteNewerText() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("old"))
            val preview = f.prepare(f.archive(listOf(note("backup"))))
            f.db.diaryDao().upsert(note("edited after preview"))
            assertTrue(runCatching { f.imports.commitImport(preview.token, mapOf(date to MergeChoice.UseBackup)) }.exceptionOrNull() is ImportPreviewChanged)
            assertEquals("edited after preview", f.db.diaryDao().readEntry(date.toString())!!.text)
            assertNull(f.imports.lastImportAtMillis.first())
            assertEquals(0, f.sessionDirectories().size)
        }
    }

    @Test fun wrongPasswordAndCorruptArchiveLeaveDeviceUnchangedAndCleanStaging() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("keep"))
            val archive = f.archive(listOf(note("backup")))
            val password = "wrong-password".toCharArray()
            assertTrue(runCatching { f.imports.prepareImport(Uri.fromFile(archive).toString(), password) }.isFailure)
            assertTrue(password.all { it == '\u0000' })
            archive.appendBytes(byteArrayOf(42))
            assertTrue(runCatching { f.prepare(archive) }.isFailure)
            assertEquals("keep", f.db.diaryDao().readEntry(date.toString())!!.text)
            assertNull(f.imports.lastImportAtMillis.first())
            assertEquals(0, f.sessionDirectories().size)
        }
    }

    @Test fun databaseFailureRollsBackTextAndPromotedFiles() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("local"))
            val archive = f.archive(listOf(note("backup")), listOf(media() to "photo"))
            val preview = f.prepare(archive)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_import BEFORE INSERT ON import_receipts BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            assertTrue(runCatching { f.imports.commitImport(preview.token, mapOf(date to MergeChoice.KeepBoth)) }.isFailure)
            assertEquals("local", f.db.diaryDao().readEntry(date.toString())!!.text)
            assertTrue(f.db.mediaDao().readAll().isEmpty())
            assertTrue(f.root.listFiles().isNullOrEmpty())
            assertEquals(0, f.sessionDirectories().size)
            assertNull(f.imports.lastImportAtMillis.first())
        }
    }

    @Test fun recoveryRemovesOnlyUnreferencedPromotedFilesAndPreservesCommittedMedia() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("keep"))
            val committed = media()
            val orphan = media()
            f.addMedia(committed, "committed bytes")
            File(f.root, orphan.fileName).writeText("orphan bytes")
            val interrupted = File(f.context.filesDir, "merge-sessions/${UUID.randomUUID()}").apply { mkdirs() }
            File(interrupted, "promoted.txt").writeText(committed.fileName + "\n" + orphan.fileName)
            File(interrupted, "manifest.json").writeText("private staged text")
            DatabaseMutationGate(f.db, f.context.filesDir).exclusive { }
            assertEquals("committed bytes", File(f.root, committed.fileName).readText())
            assertFalse(File(f.root, orphan.fileName).exists())
            assertFalse(interrupted.exists())
        }
    }

    @Test fun sharedGateSerializesDiaryWritesAndCancelledCommitDoesNotMutate() = runBlocking {
        Fixture().useSuspending { f ->
            val preview = f.prepare(f.archive(listOf(note("backup"))))
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val holder = launch { f.gate.exclusive { entered.complete(Unit); release.await() } }
            entered.await()
            val pending = launch { f.imports.commitImport(preview.token, emptyMap()) }
            pending.cancelAndJoin()
            val repo = RoomDiaryRepository(f.db.diaryDao(), f.db.mediaDao(), f.root, f.gate)
            val write = async { repo.saveEntry(note("user text").toDomain()) }
            assertFalse(write.isCompleted)
            release.complete(Unit)
            holder.join()
            withTimeout(10_000) { write.await() }
            assertEquals("user text", f.db.diaryDao().readEntry(date.toString())!!.text)
            f.imports.discardImport(preview.token)
        }
    }

    @Test fun deletingAnImportedDayAllowsIntentionalRestoreAgain() = runBlocking {
        Fixture().useSuspending { f ->
            val archive = f.archive(listOf(note("backup")))
            var preview = f.prepare(archive)
            f.imports.commitImport(preview.token, emptyMap())
            f.db.diaryDao().delete(date.toString())
            preview = f.prepare(archive)
            assertEquals(1, preview.addedDays)
            f.imports.commitImport(preview.token, emptyMap())
            assertEquals("backup", f.db.diaryDao().readEntry(date.toString())!!.text)
        }
    }

    @Test fun pathTraversalInArchiveIsRejectedBeforeAnyDatabaseMutation() = runBlocking {
        Fixture().useSuspending { f ->
            f.db.diaryDao().upsert(note("keep"))
            val archive = f.archive(listOf(note("backup")), extra = listOf("media/../../escape" to "bad"))
            assertTrue(runCatching { f.prepare(archive) }.isFailure)
            assertEquals("keep", f.db.diaryDao().readEntry(date.toString())!!.text)
            assertFalse(File(f.context.filesDir, "escape").exists())
            assertEquals(0, f.sessionDirectories().size)
        }
    }

    @Test fun insufficientSpaceStopsCopyBeforeWritingOutput() = runBlocking {
        Fixture().useSuspending { f ->
            val storage = object : File(f.context.cacheDir, "low-space") {
                override fun getUsableSpace(): Long = 0L
            }
            val output = java.io.ByteArrayOutputStream()
            val error = runCatching { copyChecked("fixture".byteInputStream(), output, storage) }.exceptionOrNull()
            assertTrue(error is java.io.IOException)
            assertEquals(0, output.size())
        }
    }

    private class Fixture {
        private val base = InstrumentationRegistry.getInstrumentation().targetContext
        private val directory = File(base.cacheDir, "merge-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = File(directory, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(directory, "cache").apply { mkdirs() }
        }
        val db = Room.inMemoryDatabaseBuilder(context, PieDatabase::class.java).build()
        val root = File(context.filesDir, "diary-media").apply { mkdirs() }
        var gate = DatabaseMutationGate(db, context.filesDir)
        var imports = LocalMergeImportRepository(context, db, gate)
        fun recreateImports() { gate = DatabaseMutationGate(db, context.filesDir); imports = LocalMergeImportRepository(context, db, gate) }
        suspend fun addMedia(item: MediaEntity, text: String) { File(root, item.fileName).writeText(text); db.mediaDao().insert(item) }
        suspend fun prepare(file: File) = imports.prepareImport(Uri.fromFile(file).toString(), "test-backup-password".toCharArray())
        fun sessionDirectories() = File(context.filesDir, "merge-sessions").listFiles().orEmpty()
        fun archive(entries: List<DiaryEntity>, media: List<Pair<MediaEntity, String>> = emptyList(), extra: List<Pair<String, String>> = emptyList()): File {
            val zipFile = File.createTempFile("test-source", ".zip", context.cacheDir)
            val archive = File.createTempFile("test-backup", ".pie", context.cacheDir)
            ZipOutputStream(zipFile.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                BackupManifestIO.write(zip, BackupManifest(entries, media.map { it.first }, ReminderSettings(enabled = true)))
                zip.closeEntry()
                media.forEach { (item, text) -> zip.putNextEntry(ZipEntry("media/${item.id}")); zip.write(text.toByteArray()); zip.closeEntry() }
                extra.forEach { (name, text) -> zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
            }
            zipFile.inputStream().use { input -> archive.outputStream().use { output -> BackupCipher.encrypt(input, output, "test-backup-password".toCharArray()) } }
            zipFile.delete()
            return archive
        }
        suspend fun useSuspending(block: suspend (Fixture) -> Unit) {
            try { block(this) } finally { db.close(); directory.deleteRecursively() }
        }
    }
}
