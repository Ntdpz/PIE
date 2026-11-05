package com.ntdpz.pie.core.data.backup

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class BackupArchiveReader(private val context: Context) {
    suspend fun read(uri: String, password: CharArray, directory: File): BackupManifest {
        val archive = File(directory, "archive.zip")
        try {
            val source = context.contentResolver.openInputStream(Uri.parse(uri)) ?: throw IOException("Cannot open backup")
            source.buffered().use { input -> archive.outputStream().buffered().use { BackupCipher.decrypt(input, it, password) } }
            val names = mutableSetOf<String>()
            var manifest: BackupManifest? = null
            ZipInputStream(archive.inputStream().buffered()).use { zip ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory && names.add(entry.name) && names.size <= 100_000)
                    if (entry.name == "manifest.json") {
                        val file = File(directory, "manifest.json")
                        file.outputStream().buffered().use { copyChecked(zip, it, directory, 16L * 1024 * 1024) }
                        file.inputStream().use { manifest = BackupManifestIO.read(it) }
                    } else {
                        require(entry.name.startsWith("media/"))
                        val id = entry.name.removePrefix("media/")
                        require(UUID.fromString(id).toString() == id)
                        File(directory, id).outputStream().buffered().use { copyChecked(zip, it, directory) }
                    }
                    zip.closeEntry()
                }
            }
            val parsed = requireNotNull(manifest)
            require(names == parsed.media.map { "media/${it.id}" }.toSet() + "manifest.json")
            return parsed
        } finally { archive.delete() }
    }
}

internal suspend fun copyChecked(input: InputStream, output: OutputStream, storage: File, limit: Long = Long.MAX_VALUE) {
    val buffer = ByteArray(64 * 1024)
    var copied = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) return
        copied += count
        require(copied <= limit)
        if (storage.usableSpace < count + 1_048_576L) throw IOException("Insufficient storage")
        output.write(buffer, 0, count)
    }
}

internal suspend fun fileDigest(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun textDigest(date: String, text: String, formatting: String = "[]"): String = MessageDigest.getInstance("SHA-256")
    .digest((date + "\u0000" + text + if (formatting == "[]") "" else "\u0000" + formatting).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
