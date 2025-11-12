package com.ray.authigel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.google.crypto.tink.Aead
import com.ray.authigel.vault.CodeRecordVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object CodeRecordVaultDataStore {
    fun create(context: Context, aead: Aead): DataStore<CodeRecordVault> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return DataStoreFactory.create(
            serializer = CodeRecordVaultSerializer(aead),
            scope = scope,
            produceFile = { context.dataStoreFile("codevault.pb") } // can keep older files if needed
        )
    }
}