package com.ray.authigel.util

import androidx.core.net.toUri
import com.ray.authigel.vault.CodeRecord
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CodeRecordImporter {
    sealed class DecryptResult {
        data class Success(val lines: List<String>) : DecryptResult()
        data object WrongPassword : DecryptResult()
        data class InvalidFormat(val reason: String) : DecryptResult()
    }

    fun getLines(inputStream: InputStream): List<String> {
        return BufferedReader(InputStreamReader(inputStream)).readLines();
    }

    fun convertToRecords(otpUris: List<String>): List<CodeRecord> {
        val newRecords = mutableListOf<CodeRecord>()
        otpUris.forEach { uri ->
            val rawUri = uri.toUri()
            val rawLabel = rawUri.lastPathSegment ?: ""
            val label = URLDecoder.decode(rawLabel, "utf-8")
            val (labelIssuer, labelHolder) = if (":" in label) {
                val parts = label.split(":", limit = 2)
                parts[0] to parts[1]
            } else {
                label to ""
            }
            val queryIssuer = rawUri.getQueryParameter("issuer")
            val issuer = queryIssuer ?: labelIssuer
            val secret = rawUri.getQueryParameter("secret") ?: ""

            val newRecord = com.ray.authigel.vault.CodeRecord.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setHolder(labelHolder)
                .setSecret(secret)
                .setRawUrl(rawUri.toString())
                .setAddedAt(System.currentTimeMillis())
            newRecords.add(newRecord.build())
        }
        return newRecords;
    }

    fun decryptEncryptedBackup(
        inputStream: InputStream,
        password: CharArray
    ): DecryptResult {
        val data = try {
            inputStream.readBytes()
        } catch (e: Exception) {
            return DecryptResult.InvalidFormat("Cannot read file bytes.")
        }

        if (data.size < 8 + 1 + 16 + 4 + 12 + 1) {
            return DecryptResult.InvalidFormat("File too small.")
        }

        val buf = ByteBuffer.wrap(data)

        val magicBytes = ByteArray(8)
        buf.get(magicBytes)
        val magic = String(magicBytes, Charsets.UTF_8)
        if (magic != "AUTHIGEL") {
            return DecryptResult.InvalidFormat("Not an AuthIgel encrypted backup.")
        }

        val version = buf.get()
        if (version.toInt() != 1) {
            return DecryptResult.InvalidFormat("Unsupported version: $version")
        }

        val salt = ByteArray(16)
        buf.get(salt)

        val iterations = buf.int
        if (iterations < 10_000) {
            return DecryptResult.InvalidFormat("Invalid PBKDF2 iterations: $iterations")
        }

        val iv = ByteArray(12)
        buf.get(iv)

        val ciphertext = ByteArray(buf.remaining())
        buf.get(ciphertext)

        return try {
            val key = deriveAesKey(password, salt, iterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

            val plaintext = cipher.doFinal(ciphertext)

            val lines = BufferedReader(InputStreamReader(ByteArrayInputStream(plaintext)))
                .readLines()

            DecryptResult.Success(lines)
        } catch (e: AEADBadTagException) {
            // Most common outcome for wrong password or tampered file
            DecryptResult.WrongPassword
        } catch (e: Exception) {
            DecryptResult.InvalidFormat("Decryption failed: ${e.javaClass.simpleName}")
        } finally {
            password.fill('\u0000')
        }
    }

    private fun deriveAesKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}