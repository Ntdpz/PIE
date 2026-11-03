package com.ntdpz.pie.core.data.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import com.ntdpz.pie.core.domain.backup.BackupReadException
import com.ntdpz.pie.core.domain.backup.BackupReadFailure

class BackupCipherTest {
    private val password = "a different backup password".toCharArray()

    @Test
    fun largePayloadRoundTripsWithFreshNonce() {
        val original = ByteArray(200_000) { (it % 251).toByte() }
        val first = encrypt(original)
        val second = encrypt(original)
        assertFalse(first.contentEquals(second))
        val restored = ByteArrayOutputStream()
        BackupCipher.decrypt(ByteArrayInputStream(first), restored, password)
        assertArrayEquals(original, restored.toByteArray())
    }

    @Test
    fun wrongPasswordIsRejected() {
        assertThrows(BackupReadException::class.java) {
            BackupCipher.decrypt(ByteArrayInputStream(encrypt("private note".toByteArray())), ByteArrayOutputStream(), "wrong password".toCharArray())
        }
    }

    @Test
    fun modifiedPayloadIsRejected() {
        val encrypted = encrypt("private note".toByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(BackupReadException::class.java) {
            BackupCipher.decrypt(ByteArrayInputStream(encrypted), ByteArrayOutputStream(), password)
        }
    }

    @Test fun interruptedHeaderOnlyFileIsReportedAsIncomplete() {
        val file = "PIEBKP01".toByteArray() + ByteArray(28)
        val error = assertThrows(BackupReadException::class.java) {
            BackupCipher.decrypt(file.inputStream(), ByteArrayOutputStream(), password)
        }
        assertEquals(BackupReadFailure.Incomplete, error.reason)
    }

    @Test fun truncatedAndReorderedSegmentsAreRejected() {
        val encrypted = encrypt(ByteArray(3 * 1024 * 1024) { (it % 251).toByte() })
        assertThrows(BackupReadException::class.java) {
            BackupCipher.decrypt(encrypted.copyOf(encrypted.size - 1).inputStream(), ByteArrayOutputStream(), password)
        }
        val firstSegment = encrypted.copyOfRange(24, 24 + 1024 * 1024)
        System.arraycopy(encrypted, 24 + 1024 * 1024, encrypted, 24, 1024 * 1024)
        System.arraycopy(firstSegment, 0, encrypted, 24 + 1024 * 1024, firstSegment.size)
        assertThrows(BackupReadException::class.java) {
            BackupCipher.decrypt(encrypted.inputStream(), ByteArrayOutputStream(), password)
        }
    }

    @Test fun validLegacyBackupStillDecrypts() {
        val header = "PIEBKP01".toByteArray() + ByteArray(28) { (it + 1).toByte() }
        val spec = javax.crypto.spec.PBEKeySpec(password, header.copyOfRange(8, 24), 210_000, 256)
        val keyBytes = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(keyBytes, "AES"), javax.crypto.spec.GCMParameterSpec(128, header.copyOfRange(24, 36)))
            updateAAD(header)
        }
        keyBytes.fill(0)
        val encrypted = header + cipher.doFinal("legacy fixture".toByteArray())
        val restored = ByteArrayOutputStream()
        BackupCipher.decrypt(encrypted.inputStream(), restored, password)
        assertEquals("legacy fixture", restored.toString("UTF-8"))
    }
    private fun encrypt(bytes: ByteArray) = ByteArrayOutputStream().also {
        BackupCipher.encrypt(ByteArrayInputStream(bytes), it, password)
    }.toByteArray()
}
