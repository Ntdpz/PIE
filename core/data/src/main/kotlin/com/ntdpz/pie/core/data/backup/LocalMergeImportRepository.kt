package com.ntdpz.pie.core.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.ntdpz.pie.core.data.diary.*
import com.ntdpz.pie.core.data.media.MediaEntity
import com.ntdpz.pie.core.data.media.MediaFileNames
import com.ntdpz.pie.core.domain.backup.*
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class LocalMergeImportRepository(
    private val context: Context,
    private val database: PieDatabase,
    private val gate: DatabaseMutationGate,
) : MergeImportRepository {
    private val root = File(context.filesDir, "diary-media").apply { mkdirs() }
    private val sessions = mutableMapOf<String, PreparedImport>()
    override val lastImportAtMillis = database.importReceiptDao().observeLastImport()

    override suspend fun prepareImport(uri: String, password: CharArray): ImportPreview = gate.exclusive {
        val token = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "merge-sessions/$token").apply { check(mkdirs()) }
        try {
            val manifest = BackupArchiveReader(context).read(uri, password, directory)
            val existing = database.diaryDao().readAll()
            val existingDates = existing.map { it.date }.toSet()
            val currentMedia = database.mediaDao().readAll()
            val receipts = database.importReceiptDao().readAll().toSet()
            val alreadyImported = manifest.entries
                .filter { ImportReceipt(it.date, textDigest(it.date, it.text, it.formatting)) in receipts }
                .map { LocalDate.parse(it.date) }.toSet()
            val plan = DiaryMergePlanner.preview(
                existing.map { it.toDomain() },
                manifest.entries.filter { LocalDate.parse(it.date) !in alreadyImported }.map { it.toDomain() },
            )
            val seen = mutableSetOf<String>()
            for (item in currentMedia) seen += mediaKey(item, File(root, item.fileName))
            val newMedia = manifest.media.filter { item -> seen.add(mediaKey(item, File(directory, item.id))) }
            val prepared = PreparedImport(directory, manifest, existing, currentMedia, receipts, plan, newMedia)
            sessions[token] = prepared
            ImportPreview(
                token = token,
                addedDays = manifest.entries.count { it.date !in existingDates },
                skippedDays = plan.unchangedDates.size + alreadyImported.size,
                conflicts = plan.conflicts,
                addedMedia = newMedia.size,
                skippedMedia = manifest.media.size - newMedia.size,
                filledDays = plan.additions.count { it.date.toString() in existingDates },
            )
        } catch (error: Throwable) {
            directory.deleteRecursively()
            throw error
        } finally { password.fill('\u0000') }
    }

    override suspend fun commitImport(token: String, choices: Map<LocalDate, MergeChoice>): ImportResult = gate.exclusive {
        val prepared = checkNotNull(sessions[token]) { "Import preview expired" }
        val promoted = mutableListOf<File>()
        var committed = false
        try {
            if (database.diaryDao().readAll() != prepared.existing || database.mediaDao().readAll() != prepared.existingMedia || database.importReceiptDao().readAll().toSet() != prepared.receipts) throw ImportPreviewChanged()
            require(choices.keys == prepared.plan.conflicts.map { it.date }.toSet())
            val entries = prepared.plan.additions.toMutableList()
            val accepted = entries.map { it.date }.toMutableSet()
            for (conflict in prepared.plan.conflicts) {
                val choice = choices.getValue(conflict.date)
                if (choice != MergeChoice.KeepDevice) {
                    entries += DiaryMergePlanner.resolve(conflict, choice, Instant.now())
                    accepted += conflict.date
                }
            }
            val targets = prepared.newMedia.map { item ->
                val id = UUID.randomUUID().toString()
                item.copy(id = id, fileName = MediaFileNames.forId(id, item.mimeType))
            }
            // Journal is durable before promoting any file. DB references decide recovery after a process kill.
            FileOutputStream(File(prepared.directory, "promoted.txt")).use { output ->
                output.write(targets.joinToString("\n") { it.fileName }.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                prepared.newMedia.zip(targets).forEach { (source, target) ->
                    val destination = File(root, target.fileName)
                    check(!destination.exists())
                    val staged = File(prepared.directory, source.id)
                    FileOutputStream(staged, true).use { it.fd.sync() }
                    check(staged.renameTo(destination))
                    promoted += destination
                }
                database.withTransaction {
                    // Check again inside the transaction, even for callers outside the shared gate.
                    if (database.diaryDao().readAll() != prepared.existing || database.mediaDao().readAll() != prepared.existingMedia) throw ImportPreviewChanged()
                    entries.forEach { database.diaryDao().upsert(it.toEntity()) }
                    targets.forEach { database.mediaDao().insert(it) }
                    prepared.plan.conflicts.filter { choices[it.date] == MergeChoice.UseBackup }.forEach { database.importReceiptDao().deleteForDate(it.date.toString()) }
                    prepared.manifest.entries.filter { LocalDate.parse(it.date) in accepted || LocalDate.parse(it.date) in prepared.plan.unchangedDates }.forEach {
                        database.importReceiptDao().insert(ImportReceipt(it.date, textDigest(it.date, it.text, it.formatting)))
                    }
                    database.importReceiptDao().recordHistory(ImportHistory(completedAtMillis = System.currentTimeMillis()))
                }
                committed = true
            }
            val existingDates = prepared.existing.map { it.date }.toSet()
            val newDays = prepared.manifest.entries.count { it.date !in existingDates }
            val changed = entries.count { it.date.toString() in existingDates }
            ImportResult(newDays, changed, prepared.manifest.entries.size - newDays - changed, targets.size, prepared.manifest.media.size - targets.size)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                val cleaned = committed || promoted.map { !it.exists() || it.delete() }.all { it }
                sessions.remove(token)
                if (cleaned) prepared.directory.deleteRecursively()
            }
        }
    }

    override suspend fun discardImport(token: String) = gate.exclusive {
        sessions.remove(token)?.directory?.deleteRecursively()
        Unit
    }

    private suspend fun mediaKey(item: MediaEntity, file: File) = item.date + ":" + fileDigest(file)

    private data class PreparedImport(
        val directory: File,
        val manifest: BackupManifest,
        val existing: List<DiaryEntity>,
        val existingMedia: List<MediaEntity>,
        val receipts: Set<ImportReceipt>,
        val plan: DiaryMergePlan,
        val newMedia: List<MediaEntity>,
    )
}
