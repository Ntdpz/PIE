package com.ntdpz.pie.core.data.backup

import com.ntdpz.pie.core.domain.backup.BackupReadException
import com.ntdpz.pie.core.domain.backup.BackupReadFailure
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Reads existing V1 files only. Bound legacy provider buffering to avoid another process OOM. */
internal object LegacyBackupCipher {
    private const val MaxCiphertextBytes = 32L * 1024 * 1024

    fun decrypt(input: InputStream, output: OutputStream, password: CharArray, magic: ByteArray) {
        val rest = ByteArray(28)
        try { DataInputStream(input).readFully(rest) }
        catch (_: EOFException) { throw BackupReadException(BackupReadFailure.Incomplete) }
        val spec = PBEKeySpec(password, rest.copyOfRange(0, 16), 210_000, 256)
        val keyBytes = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        val key = SecretKeySpec(keyBytes, "AES")
        keyBytes.fill(0)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, rest.copyOfRange(16, 28)))
            updateAAD(magic + rest)
        }
        val buffer = ByteArray(64 * 1024)
        var read = 0L
        try {
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                read += count
                if (read > MaxCiphertextBytes) throw BackupReadException(BackupReadFailure.LegacyTooLarge)
                cipher.update(buffer, 0, count)?.let(output::write)
            }
            if (read < 16) throw BackupReadException(BackupReadFailure.Incomplete)
            output.write(cipher.doFinal())
        } catch (_: AEADBadTagException) { throw BackupReadException(BackupReadFailure.AuthenticationOrDamage) }
    }
}
