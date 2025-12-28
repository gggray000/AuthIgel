package com.ray.authigel.data

import androidx.datastore.core.DataStore
import com.ray.authigel.vault.CodeRecord
import com.ray.authigel.vault.CodeRecordVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CodeRecordVaultRepository(private val dataStore: DataStore<CodeRecordVault>) {
    val records: Flow<List<CodeRecord>> =
        dataStore.data.map { it.tokensList }

    val hasBackupPasswordFlow: Flow<Boolean> =
        dataStore.data.map { vault ->
            !vault.encryptedBackupPassword.isEmpty
        }

    suspend fun add(record: CodeRecord) {
        dataStore.updateData { current ->
            current.toBuilder().addTokens(record).build()
        }
    }

    suspend fun removeById(id: String) {
        dataStore.updateData { current ->
            val filtered = current.tokensList.filterNot { it.id == id }
            current.toBuilder()
                .clearTokens()
                .addAllTokens(filtered)
                .build()
        }
    }

    suspend fun replaceAll(records: List<CodeRecord>) {
        dataStore.updateData { current ->
            current.toBuilder()
                .clearTokens()
                .addAllTokens(records)
                .build()
        }
    }

    suspend fun getAll(): List<CodeRecord> {
        return dataStore.data.first().tokensList
    }

    suspend fun storeEncryptedBackupPassword(encrypted: ByteArray) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setEncryptedBackupPassword(
                    com.google.protobuf.ByteString.copyFrom(encrypted)
                )
                .build()
        }
    }

    suspend fun getEncryptedBackupPassword(): ByteArray? {
        val vault = dataStore.data.first()
        val bs = vault.encryptedBackupPassword
        return if (bs.isEmpty) null else bs.toByteArray()
    }

    suspend fun clearEncryptedBackupPassword() {
        dataStore.updateData { current ->
            current.toBuilder()
                .clearEncryptedBackupPassword()
                .build()
        }
    }
}