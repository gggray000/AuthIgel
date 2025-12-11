package com.ray.authigel.util

import android.content.Context
import android.net.Uri
import com.ray.authigel.vault.CodeRecord

class CodeRecordExporter {

    fun buildBackupBytes(records: List<CodeRecord>): ByteArray {
        val backupStrings = records.map { it.rawUrl }
        return backupStrings.joinToString(separator = "\n").toByteArray()
    }
    fun writeToUri(context: Context, uri: Uri, byteArray: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(byteArray)
            } ?: return false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}