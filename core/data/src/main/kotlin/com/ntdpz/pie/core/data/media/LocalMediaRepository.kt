package com.ntdpz.pie.core.data.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.ntdpz.pie.core.data.diary.DiaryEntity
import com.ntdpz.pie.core.data.diary.PieDatabase
import com.ntdpz.pie.core.domain.media.DiaryMedia
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.ntdpz.pie.core.data.backup.DatabaseMutationGate
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

internal class LocalMediaRepository(
    private val context: Context,
    private val database: PieDatabase,
    private val gate: DatabaseMutationGate = DatabaseMutationGate(database, context.filesDir),
) : DiaryMediaRepository {
    private val root = File(context.filesDir, "diary-media").apply { mkdirs() }
    private val dao = database.mediaDao()

    override fun observeAllMedia() = dao.observeAll().onStart { gate.exclusive { } }.map { rows ->
        rows.groupBy { LocalDate.parse(it.date) }.mapValues { (_, items) ->
            items.map { row -> DiaryMedia(row.id, Uri.fromFile(File(root, row.fileName)).toString(), row.mimeType.startsWith("video/")) }
        }
    }

    override fun observeMedia(date: LocalDate) = dao.observe(date.toString()).onStart { gate.exclusive { } }.map { rows ->
        rows.map { row ->
            DiaryMedia(row.id, Uri.fromFile(File(root, row.fileName)).toString(), row.mimeType.startsWith("video/"))
        }
    }

    override suspend fun importMedia(date: LocalDate, uri: String) = gate.exclusive {
        val source = Uri.parse(uri)
        val mime = context.contentResolver.getType(source).orEmpty()
        require(mime.startsWith("image/") || mime.startsWith("video/")) { "Unsupported media" }
        val id = UUID.randomUUID().toString()
        val fileName = MediaFileNames.forId(id, mime)
        val target = File(root, fileName)
        val staging = File(root, "$id.partial")
        var committed = false
        try {
            val knownSize = context.contentResolver.query(source, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
            }
            if (knownSize != null && knownSize + 1_048_576L > root.usableSpace) throw IOException("Insufficient storage")
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("Cannot open media")
            input.use { sourceStream ->
                staging.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = sourceStream.read(buffer)
                        if (count < 0) break
                        if (root.usableSpace < count + 1_048_576L) throw IOException("Insufficient storage")
                        output.write(buffer, 0, count)
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                check(staging.renameTo(target)) { "Cannot commit media file" }
                database.withTransaction {
                    database.diaryDao().ensureEntry(DiaryEntity(date.toString(), "", System.currentTimeMillis()))
                    dao.insert(MediaEntity(id, date.toString(), fileName, mime, System.currentTimeMillis()))
                }
                committed = true
            }
        } finally {
            staging.delete()
            if (!committed) target.delete()
        }
    }

    override suspend fun deleteMedia(id: String) = gate.exclusive {
        val media = dao.read(id) ?: return@exclusive
        dao.delete(id)
        File(root, media.fileName).delete()
        Unit
    }
}
