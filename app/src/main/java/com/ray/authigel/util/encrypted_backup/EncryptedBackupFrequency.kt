package com.ray.authigel.util.encrypted_backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

sealed class EncryptedBackupFrequency {

    object Never : EncryptedBackupFrequency()
    object Once : EncryptedBackupFrequency()
    data class Periodic(val days: Int) : EncryptedBackupFrequency()

    companion object {
        val KEY_BACKUP_MODE = stringPreferencesKey("backup_mode")
        val KEY_PERIOD = intPreferencesKey("period_days")

        fun fromPrefs(prefs: Preferences): EncryptedBackupFrequency =
            when (prefs[KEY_BACKUP_MODE]) {
                "never" -> Never
                "once" -> Once
                "periodic" ->
                    Periodic(prefs[KEY_PERIOD] ?: 7)
                else -> Never
            }
    }

    fun writeToPrefs(prefs: MutablePreferences) {
        when (this) {
            Never -> {
                prefs[KEY_BACKUP_MODE] = "never"
                prefs.remove(KEY_PERIOD)
            }
            Once -> {
                prefs[KEY_BACKUP_MODE] = "once"
                prefs.remove(KEY_PERIOD)
            }
            is Periodic -> {
                prefs[KEY_BACKUP_MODE] = "periodic"
                prefs[KEY_PERIOD] = days
            }
        }
    }
}