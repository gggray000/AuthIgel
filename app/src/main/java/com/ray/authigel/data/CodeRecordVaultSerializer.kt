package com.ray.authigel.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.ray.authigel.vault.CodeRecordVault
import java.io.InputStream
import java.io.OutputStream

object CodeRecordVaultSerializer : Serializer<CodeRecordVault> {
    override val defaultValue: CodeRecordVault = CodeRecordVault.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CodeRecordVault {
        try {
            return CodeRecordVault.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: CodeRecordVault,
        output: OutputStream) = t.writeTo(output)
}

val Context.codeRecordVaultDataStore: DataStore<CodeRecordVault> by dataStore(
    fileName = "settings.pb",
    serializer = CodeRecordVaultSerializer
)
