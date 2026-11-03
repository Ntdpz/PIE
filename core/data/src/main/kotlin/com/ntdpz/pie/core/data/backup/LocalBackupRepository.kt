package com.ntdpz.pie.core.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ntdpz.pie.core.data.diary.PieDatabase
import com.ntdpz.pie.core.domain.backup.BackupRepository
import com.ntdpz.pie.core.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class LocalBackupRepository(
    private val context: Context,
    private val database: PieDatabase,
    private val settings: SettingsRepository,
) : BackupRepository {
    private val mediaRoot = File(context.filesDir, "diary-media").apply { mkdirs() }

    override suspend fun canRestore() = database.diaryDao().count() == 0

    override suspend fun export(uri: String, password: CharArray) = withContext(Dispatchers.IO) {
        require(password.size >= 8)
        val zipFile = File.createTempFile("pie-export-", ".zip", context.cacheDir)
        try {
            val snapshot = database.withTransaction {
                BackupManifest(database.diaryDao().readAll(), database.mediaDao().readAll(), settings.settings.first().reminder)
            }
            ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                BackupManifestIO.write(zip, snapshot)
                zip.closeEntry()
                snapshot.media.forEach {
                    zip.putNextEntry(ZipEntry("media/${it.id}"))
                    File(mediaRoot, it.fileName).inputStream().use { input -> copy(input, zip, context.cacheDir) }
                    zip.closeEntry()
                }
            }
            val output = context.contentResolver.openOutputStream(Uri.parse(uri), "wt") ?: throw IOException("Cannot open backup destination")
            output.buffered().use { target ->
                zipFile.inputStream().buffered().use { BackupCipher.encrypt(it, target, password) }
            }
            settings.recordBackupSuccess(System.currentTimeMillis())
        } finally { zipFile.delete() }
    }

    override suspend fun restore(uri: String, password: CharArray) = withContext(Dispatchers.IO) {
        check(canRestore()) { "Restore requires an empty app" }
        val zipFile = File.createTempFile("pie-restore-", ".zip", context.cacheDir)
        val staging = File(context.filesDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        val moved = mutableListOf<File>()
        var committed = false
        val previous = settings.settings.first().reminder
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(uri)) ?: throw IOException("Cannot open backup")
            input.buffered().use { source ->
                zipFile.outputStream().buffered().use { BackupCipher.decrypt(source, it, password) }
            }
            var manifest: BackupManifest? = null
            val names = mutableSetOf<String>()
            ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(names.add(entry.name) && !entry.isDirectory)
                    if (entry.name == "manifest.json") manifest = BackupManifestIO.read(zip)
                    else {
                        require(entry.name.startsWith("media/"))
                        val id = entry.name.removePrefix("media/")
                        require(UUID.fromString(id).toString() == id)
                        File(staging, id).outputStream().buffered().use { copy(zip, it, staging) }
                    }
                    zip.closeEntry()
                }
            }
            val parsed = requireNotNull(manifest)
            require(names == parsed.media.map { "media/${it.id}" }.toSet() + "manifest.json")
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                settings.saveReminder(parsed.reminder.copy(enabled = false))
                database.withTransaction {
                    check(database.diaryDao().count() == 0)
                    parsed.media.forEach {
                        val target = File(mediaRoot, it.fileName)
                        check(!target.exists() || target.delete()) { "Cannot replace an unreferenced media file" }
                        check(File(staging, it.id).renameTo(target))
                        moved += target
                    }
                    parsed.entries.forEach { database.diaryDao().upsert(it) }
                    parsed.media.forEach { database.mediaDao().insert(it) }
                }
                committed = true
            }
        } finally {
            zipFile.delete()
            staging.deleteRecursively()
            if (!committed) withContext(NonCancellable) {
                moved.forEach(File::delete)
                settings.saveReminder(previous)
            }
        }
    }

    private suspend fun copy(input: InputStream, output: OutputStream, storage: File) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            if (storage.usableSpace < count + 1_048_576L) throw IOException("Insufficient storage")
            output.write(buffer, 0, count)
        }
    }
}
