package com.ray.authigel.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.ray.authigel.vault.CodeRecordVault
import java.io.InputStream
import java.io.OutputStream
import com.google.crypto.tink.Aead

class CodeRecordVaultSerializer(private val aead: Aead) : Serializer<CodeRecordVault> {
    override val defaultValue: CodeRecordVault = CodeRecordVault.getDefaultInstance()
    // stable AAD so ciphertext is bound to app/schema version
    private val aad: ByteArray = "com.ray.authigel.vault/v1".toByteArray()

    override suspend fun readFrom(input: InputStream): CodeRecordVault {
        val ciphertext = input.readBytes()
        if (ciphertext.isEmpty()) return defaultValue
        // Allow a one-time migration from plaintext to encrypted if you already have data
        // Try decrypt first; if it fails, try parsing as plaintext
        runCatching {
            val clear = aead.decrypt(ciphertext, aad)
            return CodeRecordVault.parseFrom(clear)
        }.onFailure { decryptErr ->
            // Fallback: plaintext (pre-encryption installs)
            runCatching {
                return CodeRecordVault.parseFrom(ciphertext)
            }.onFailure {
                when (it) {
                    is InvalidProtocolBufferException ->
                        throw CorruptionException("Cannot read CodeRecordVault (bad proto).", it)
                    else -> throw CorruptionException("Cannot read CodeRecordVault (decrypt failed).", decryptErr)
                }
            }
        }
        // Unreachable
        return defaultValue
    }

    override suspend fun writeTo(t: CodeRecordVault, output: OutputStream) {
        val clear = t.toByteArray()
        val ciphertext = aead.encrypt(clear, aad)
        output.write(ciphertext)
    }
}
