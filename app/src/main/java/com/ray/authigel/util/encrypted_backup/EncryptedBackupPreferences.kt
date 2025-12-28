package com.ray.authigel.util.encrypted_backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.encryptedBackupDataStore by preferencesDataStore("encrypted_backup_settings")

object EncryptedBackupPreferences {

    private val KEY_URI = stringPreferencesKey("folder_uri")
    private val KEY_LAST_BACKUP_FILE_URI =
        stringPreferencesKey("last_backup_file_uri")

    fun load(context: Context): Pair<EncryptedBackupFrequency, Uri?> =
        runBlocking {
            val prefs = context.encryptedBackupDataStore.data.first()
            val options = EncryptedBackupFrequency.fromPrefs(prefs)
            val folderUri = prefs[KEY_URI]?.toUri()

            options to folderUri
        }

    suspend fun save(
        context: Context,
        options: EncryptedBackupFrequency,
        folderUri: Uri?
    ) {
        context.encryptedBackupDataStore.edit { prefs ->
            options.writeToPrefs(prefs)

            if (folderUri != null) {
                prefs[KEY_URI] = folderUri.toString()
            } else {
                prefs.remove(KEY_URI)
            }
        }
    }

    suspend fun saveLastBackupFileUri(
        context: Context,
        fileUri: Uri
    ) {
        context.encryptedBackupDataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_FILE_URI] = fileUri.toString()
        }
    }

    fun loadLastBackupFileUri(context: Context): Uri? = runBlocking {
        val prefs = context.encryptedBackupDataStore.data.first()
        val uriString = prefs[KEY_LAST_BACKUP_FILE_URI]
        uriString?.toUri()
    }

    suspend fun clearLastBackupFileUri(context: Context) {
        context.encryptedBackupDataStore.edit { prefs ->
            prefs.remove(KEY_LAST_BACKUP_FILE_URI)
        }
    }
}