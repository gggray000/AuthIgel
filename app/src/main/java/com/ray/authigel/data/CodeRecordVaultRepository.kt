package com.ray.authigel.data

import android.content.Context
import com.ray.authigel.vault.CodeRecord
import com.ray.authigel.vault.CodeRecordVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CodeRecordVaultRepository(private val appContext: Context) {

    /** Stream of records (Proto -> List<CodeRecord>) */
    val records: Flow<List<CodeRecord>> =
        appContext.codeRecordVaultDataStore.data.map { vault ->
            vault.tokensList
        }

    /** Add one record */
    suspend fun add(record: CodeRecord) {
        appContext.codeRecordVaultDataStore.updateData { current ->
            current.toBuilder()
                .addTokens(record)
                .build()
        }
    }

    /** Remove by id */
    suspend fun removeById(id: String) {
        appContext.codeRecordVaultDataStore.updateData { current ->
            val filtered = current.tokensList.filterNot { it.id == id }
            CodeRecordVault.newBuilder()
                .addAllTokens(filtered)
                .build()
        }
    }

    /** Replace all (e.g., import/restore) */
    suspend fun replaceAll(records: List<CodeRecord>) {
        appContext.codeRecordVaultDataStore.updateData {
            CodeRecordVault.newBuilder().addAllTokens(records).build()
        }
    }
}