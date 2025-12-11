package com.ray.authigel.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.ray.authigel.vault.CodeRecordVault

object VaultDI {
    @Volatile private var initialized = false

    lateinit var aead: Aead
        private set
    lateinit var dataStore: DataStore<CodeRecordVault>
        private set
    lateinit var repo: CodeRecordVaultRepository
        private set

    fun init(appContext: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            TinkConfig.register()

            val keysetMgr = AndroidKeysetManager.Builder()
                .withSharedPref(appContext, "tink_codevault_keyset", "tink_prefs")
                .withKeyTemplate(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri("android-keystore://codevault_master_key")
                .build()

            aead = keysetMgr.keysetHandle.getPrimitive(Aead::class.java)
            dataStore = CodeRecordVaultDataStore.create(appContext, aead)
            repo = CodeRecordVaultRepository(dataStore)

            initialized = true
        }
    }

    fun provideRepository(context: Context): CodeRecordVaultRepository {
        return CodeRecordVaultRepository(dataStore)
    }

}