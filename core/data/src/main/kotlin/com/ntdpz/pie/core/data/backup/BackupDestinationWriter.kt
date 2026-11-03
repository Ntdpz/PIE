package com.ntdpz.pie.core.data.backup

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Build the entire encrypted archive locally, then verify the bytes stored by the document provider. */
internal class BackupDestinationWriter(private val context: Context) {
    suspend fun write(source: File, uri: String, password: CharArray) {
        val staged = File.createTempFile("pie-encrypted-", ".pie", context.cacheDir)
        val jobContext = currentCoroutineContext()
        try {
            source.inputStream().buffered().use { raw ->
                val cancellable = object : FilterInputStream(raw) {
                    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                        jobContext.ensureActive()
                        return super.read(bytes, offset, length)
                    }
                }
                staged.outputStream().buffered().use { rawOutput ->
                    val checked = object : FilterOutputStream(rawOutput) {
                        override fun write(bytes: ByteArray, offset: Int, length: Int) {
                            if (context.cacheDir.usableSpace < length + 1_048_576L) throw IOException("Insufficient storage")
                            out.write(bytes, offset, length)
                        }
                    }
                    BackupCipher.encrypt(cancellable, checked, password)
                }
            }
            val expected = staged.inputStream().use { digest(it) }
            val destination = Uri.parse(uri)
            val output = context.contentResolver.openOutputStream(destination, "wt") ?: throw IOException("Cannot open backup destination")
            output.buffered().use { target -> staged.inputStream().buffered().use { copyChecked(it, target, context.cacheDir) } }
            val actualHash = MessageDigest.getInstance("SHA-256")
            var verifiedBytes = 0L
            val verified = object : OutputStream() {
                override fun write(value: Int) { verifiedBytes++ }
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    jobContext.ensureActive()
                    verifiedBytes += length
                }
            }
            val input = context.contentResolver.openInputStream(destination) ?: throw IOException("Cannot verify backup destination")
            input.buffered().use { BackupCipher.decrypt(DigestInputStream(it, actualHash), verified, password) }
            check(verifiedBytes == source.length()) { "Backup plaintext verification failed" }
            val actual = actualHash.digest()
            check(MessageDigest.isEqual(expected, actual)) { "Backup destination verification failed" }
        } finally { staged.delete() }
    }

    private suspend fun digest(input: InputStream): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) return hash.digest()
            hash.update(buffer, 0, count)
        }
    }
}
