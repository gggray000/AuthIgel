package com.ray.authigel.util.encrypted_backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant

val Context.encryptedBackupDataStore by preferencesDataStore("encrypted_backup_settings")

data class AutoBackupBasicPrefs(
    val enabled: Boolean,
    val folderUri: Uri?
)
object AutoBackupPreferences {
    private val KEY_ENABLED = booleanPreferencesKey("backup_enabled")
    private val KEY_URI = stringPreferencesKey("folder_uri")
    private val KEY_LAST_BACKUP_FILE_URI = stringPreferencesKey("last_backup_file_uri")
    private val KEY_LAST_BACKUP_INSTANT = longPreferencesKey("last_backup_instant")



    fun load(context: Context): AutoBackupBasicPrefs = runBlocking {
        val prefs = context.encryptedBackupDataStore.data.first()

        AutoBackupBasicPrefs(
            enabled = prefs[KEY_ENABLED] ?: false,
            folderUri = prefs[KEY_URI]?.toUri()
        )
    }

    suspend fun save(
        context: Context,
        enabled: Boolean,
        folderUri: Uri?
    ) {
        context.encryptedBackupDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled

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

    suspend fun saveLastBackupInstant(
        context: Context,
        instant: Instant
    ) {
        context.encryptedBackupDataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_INSTANT] = instant.toEpochMilli()
        }
    }

    fun loadLastBackupInstant(context: Context): Instant? = runBlocking {
        val prefs = context.encryptedBackupDataStore.data.first()
        prefs[KEY_LAST_BACKUP_INSTANT]?.let { Instant.ofEpochMilli(it) }
    }
}