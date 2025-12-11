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

    suspend fun add(record: CodeRecord) {
        dataStore.updateData { current ->
            current.toBuilder().addTokens(record).build()
        }
    }

    suspend fun removeById(id: String) {
        dataStore.updateData { current ->
            val filtered = current.tokensList.filterNot { it.id == id }
            CodeRecordVault.newBuilder().addAllTokens(filtered).build()
        }
    }

    suspend fun replaceAll(records: List<CodeRecord>) {
        dataStore.updateData { CodeRecordVault.newBuilder().addAllTokens(records).build() }
    }

    suspend fun getAll(): List<CodeRecord> {
        return dataStore.data.first().tokensList
    }
}