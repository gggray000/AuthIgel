package com.ray.authigel.util.encrypted_backup;

import android.content.Context
import android.net.Uri

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.ray.authigel.data.BackupPasswordKeystore
import com.ray.authigel.data.VaultDI
import com.ray.authigel.util.CodeRecordExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AutoBackupManager {

    suspend fun maybeRun(context: Context) {
        val prefs = AutoBackupPreferences.load(context)
        if (!prefs.enabled) return

        if (!shouldRunToday(context)) return

        val backupUri = runBackup(context)
        if (backupUri != null) {
            AutoBackupPreferences.saveLastBackupInstant(
                context,
                Instant.now()
            )
            AutoBackupPreferences.saveLastBackupFileUri(
                context,
                backupUri
            )
            AutoBackupRetention.cleanup(context, keep = 5)
        }
    }

    private fun shouldRunToday(context: Context): Boolean {
        val last = AutoBackupPreferences.loadLastBackupInstant(context) ?: return true

        val lastDate = last
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return lastDate.isBefore(LocalDate.now())
    }

    private suspend fun runBackup(context: Context): Uri? =
        withContext(Dispatchers.IO) {

            val (enabled, folderUri) = AutoBackupPreferences.load(context)
            if (!enabled || folderUri == null) return@withContext null

            val repo = VaultDI.provideRepository(context)
            val encryptedPwBlob = repo.getEncryptedBackupPassword()
                ?: return@withContext null

            val password = BackupPasswordKeystore.decrypt(encryptedPwBlob)

            try {
                val records = repo.getAll()
                val plaintext = CodeRecordExporter().buildBackupBytes(records)
                val encrypted = BackupCrypto.encrypt(password, plaintext)

                val backupUri = createBackupFile(
                    context = context,
                    treeUri = folderUri,
                    bytes = encrypted
                )

                return@withContext backupUri
            } finally {
                password.fill('\u0000')
            }
        }

    private fun createBackupFile(
        context: Context,
        treeUri: Uri,
        bytes: ByteArray
    ): Uri? {
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri)
                ?: return null

            if (!tree.canWrite()) return null

            val backupDir = tree.findFile("AuthIgelBackups")
                ?: tree.createDirectory("AuthIgelBackups")
                ?: return null

            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

            val file = backupDir.createFile(
                "text/plain",
                "AuthIgel_AutoBackup_$timestamp.txt"
            ) ?: return null

            if (CodeRecordExporter().writeToUri(context, file.uri, bytes)) {
                file.uri
            } else {
                null
            }
        } catch (e: SecurityException) {
            Log.e("AutoBackup", "Cannot create backup file", e)
            null
        }
    }
}