package com.ntdpz.pie.core.data.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException

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
        assertThrows(AEADBadTagException::class.java) {
            BackupCipher.decrypt(ByteArrayInputStream(encrypt("private note".toByteArray())), ByteArrayOutputStream(), "wrong password".toCharArray())
        }
    }

    @Test
    fun modifiedPayloadIsRejected() {
        val encrypted = encrypt("private note".toByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) {
            BackupCipher.decrypt(ByteArrayInputStream(encrypted), ByteArrayOutputStream(), password)
        }
    }

    private fun encrypt(bytes: ByteArray) = ByteArrayOutputStream().also {
        BackupCipher.encrypt(ByteArrayInputStream(bytes), it, password)
    }.toByteArray()
}
