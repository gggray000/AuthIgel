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

    fun add(record: CodeRecord) = viewModelScope.launch {
        repo.add(record)
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.removeById(id)
    }
}