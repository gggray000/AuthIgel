// com/ray/authigel/data/CodeVaultViewModel.kt
package com.ray.authigel.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ray.authigel.vault.CodeRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CodeRecordVaultViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VaultDI.repo

    val records: StateFlow<List<CodeRecord>> =
        repo.records.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val hasPassword = repo.hasEncryptedBackupPassword
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun add(record: CodeRecord) = viewModelScope.launch {
        repo.add(record)
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.removeById(id)
    }

    fun storeEncryptedBackupPassword(password: ByteArray) = viewModelScope.launch {
        repo.storeEncryptedBackupPassword(password)
    }

    fun clearEncryptedBackupPassword() = viewModelScope.launch {
        repo.clearEncryptedBackupPassword()
    }

    suspend fun getEncryptedBackupPassword(): ByteArray? {
        return repo.getEncryptedBackupPassword()
    }

    suspend fun getBackupPasswordPlaintext(): String? {
        val encrypted = getEncryptedBackupPassword() ?: return null
        return BackupPasswordKeystore.decrypt(encrypted).concatToString()
    }
}