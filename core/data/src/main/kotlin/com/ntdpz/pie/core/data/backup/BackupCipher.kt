package com.ntdpz.pie.core.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupCipher {
    private val magic = "PIEBKP01".toByteArray(Charsets.US_ASCII)
    private const val SaltSize = 16
    private const val NonceSize = 12

    fun encrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val random = SecureRandom()
        val salt = ByteArray(SaltSize).also(random::nextBytes)
        val nonce = ByteArray(NonceSize).also(random::nextBytes)
        val header = magic + salt + nonce
        output.write(header)
        transform(input, output, password, header, Cipher.ENCRYPT_MODE)
    }

    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val header = ByteArray(magic.size + SaltSize + NonceSize)
        java.io.DataInputStream(input).readFully(header)
        require(header.copyOfRange(0, magic.size).contentEquals(magic)) { "Unsupported backup format" }
        transform(input, output, password, header, Cipher.DECRYPT_MODE)
    }

    private fun transform(input: InputStream, output: OutputStream, password: CharArray, header: ByteArray, mode: Int) {
        val salt = header.copyOfRange(magic.size, magic.size + SaltSize)
        val nonce = header.copyOfRange(magic.size + SaltSize, header.size)
        val spec = PBEKeySpec(password, salt, 210_000, 256)
        val keyBytes = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        val key = SecretKeySpec(keyBytes, "AES")
        keyBytes.fill(0)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(128, nonce))
            updateAAD(header)
        }
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            cipher.update(buffer, 0, count)?.let(output::write)
        }
        output.write(cipher.doFinal())
    }
}
