package com.ntdpz.pie.core.data.backup

import com.google.crypto.tink.subtle.AesGcmHkdfStreaming
import com.ntdpz.pie.core.domain.backup.BackupReadException
import com.ntdpz.pie.core.domain.backup.BackupReadFailure
import java.io.DataInputStream
import java.io.EOFException
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** V2 uses Tink's authenticated segments; platform GCM can buffer the entire input. */
internal object BackupCipher {
    private val magic = "PIEBKP02".toByteArray(Charsets.US_ASCII)
    private val legacyMagic = "PIEBKP01".toByteArray(Charsets.US_ASCII)
    private const val SaltSize = 16

    fun encrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val salt = ByteArray(SaltSize).also(SecureRandom()::nextBytes)
        val header = magic + salt
        val cipher = streamingCipher(password, salt)
        output.write(header)
        // Closing finalizes the authenticated last segment without taking ownership of the caller's stream.
        val destination = object : FilterOutputStream(output) {
            override fun close() = flush()
            override fun write(bytes: ByteArray, offset: Int, length: Int) = out.write(bytes, offset, length)
        }
        cipher.newEncryptingStream(destination, header).use { encrypted -> input.copyTo(encrypted, 64 * 1024) }
    }

    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val source = input.buffered()
        val version = ByteArray(magic.size)
        try { DataInputStream(source).readFully(version) }
        catch (_: EOFException) { throw BackupReadException(BackupReadFailure.Incomplete) }
        when {
            version.contentEquals(legacyMagic) -> LegacyBackupCipher.decrypt(source, output, password, version)
            version.contentEquals(magic) -> {
                val salt = ByteArray(SaltSize)
                try { DataInputStream(source).readFully(salt) }
                catch (_: EOFException) { throw BackupReadException(BackupReadFailure.Incomplete) }
                source.mark(1)
                if (source.read() < 0) throw BackupReadException(BackupReadFailure.Incomplete)
                source.reset()
                val cipher = streamingCipher(password, salt)
                try { cipher.newDecryptingStream(source, version + salt).use { it.copyTo(output, 64 * 1024) } }
                catch (_: java.io.IOException) { throw BackupReadException(BackupReadFailure.AuthenticationOrDamage) }
            }
            else -> throw BackupReadException(BackupReadFailure.Unsupported)
        }
    }

    private fun streamingCipher(password: CharArray, salt: ByteArray): AesGcmHkdfStreaming {
        val spec = PBEKeySpec(password, salt, 210_000, 256)
        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        return try { AesGcmHkdfStreaming(key, "HmacSha256", 32, 1024 * 1024, 0) }
        finally { key.fill(0) }
    }
}
