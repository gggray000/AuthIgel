package com.ray.authigel.util.AutoBackup


import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.autoBackupDataStore by preferencesDataStore("auto_backup_settings")

object AutoBackupPreferences {
    val KEY_ENABLED = booleanPreferencesKey("enabled")
    val KEY_PERIOD = intPreferencesKey("period_days")
    val KEY_URI = stringPreferencesKey("folder_uri")

    fun load(context: Context): Triple<Boolean, Int, Uri?> = runBlocking {
        val prefs = context.autoBackupDataStore.data.first()
        val enabled = prefs[KEY_ENABLED] ?: false
        val period = prefs[KEY_PERIOD] ?: 7
        val uriString = prefs[KEY_URI]
        val uri = uriString?.let { Uri.parse(it) }
        Triple(enabled, period, uri)
    }

    suspend fun save(context: Context, enabled: Boolean, periodDays: Int, uri: Uri?) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
            prefs[KEY_PERIOD] = periodDays
            if (uri != null) {
                prefs[KEY_URI] = uri.toString()
            } else {
                prefs.remove(KEY_URI)
            }
        }
    }
}