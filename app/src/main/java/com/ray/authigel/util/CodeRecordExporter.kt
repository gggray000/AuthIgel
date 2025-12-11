package com.ray.authigel.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.ray.authigel.vault.CodeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CodeRecordExporter {

    fun buildBackupBytes(records: List<CodeRecord>): ByteArray {
        val backupStrings = records.map { it.rawUrl }
        return backupStrings.joinToString(separator = "\n").toByteArray()
    }
    fun writeToUri(context: Context, uri: Uri, byteArray: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(byteArray)
        }
    }
}