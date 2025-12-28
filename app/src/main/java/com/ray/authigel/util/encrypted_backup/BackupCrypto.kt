package com.ray.authigel.util.encrypted_backup

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val MAGIC = "AUTHIGEL"
    private const val VERSION: Byte = 1
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_LEN = 16
    private const val IV_LEN = 12

    // Adjust if you want slower/faster. 150k is a good baseline.
    private const val PBKDF2_ITERS = 150_000

    fun encrypt(password: CharArray, plaintext: ByteArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }

        val key = deriveAesKey(password, salt, PBKDF2_ITERS)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // File format: MAGIC(8) | VERSION(1) | SALT(16) | ITERS(4) | IV(12) | CIPHERTEXT(n)
        return ByteArrayOutputStream().use { out ->
            out.write(MAGIC.toByteArray(Charsets.UTF_8))
            out.write(byteArrayOf(VERSION))
            out.write(salt)
            out.write(ByteBuffer.allocate(4).putInt(PBKDF2_ITERS).array())
            out.write(iv)
            out.write(ciphertext)
            out.toByteArray()
        }
    }

    private fun deriveAesKey(password: CharArray, salt: ByteArray, iters: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iters, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }
}