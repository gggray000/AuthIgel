package com.ray.authigel.util.autoBackup

import android.content.Context
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ray.authigel.data.BackupPasswordKeystore
import com.ray.authigel.data.VaultDI
import com.ray.authigel.util.CodeRecordExporter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AutoBackupWorker(appContext: Context, workerParams: WorkerParameters):
CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val exporter = CodeRecordExporter()
        val (enabled, _, folderUri) = AutoBackupPreferences.load(context)
        if (!enabled) {
            return Result.success()
        }
        if (folderUri == null) {
            return Result.failure()
        }

        return try {
            val repo = VaultDI.provideRepository(context)
            val encryptedPwBlob = repo.getEncryptedBackupPassword()
                ?: return Result.failure()
            val password: CharArray = BackupPasswordKeystore.decrypt(encryptedPwBlob)
            try {
                val records = repo.getAll()
                val plaintext = exporter.buildBackupBytes(records)
                // PBKDF2 + AES-GCM
                val encryptedBackupFileBytes = BackupCrypto.encrypt(password, plaintext)
                val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
                )
                val timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                val fileName = "AuthIgel_AutoBackup_$timestamp.txt"
                val backupUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    dirUri,
                    "text/plain",
                    fileName
                ) ?: return Result.failure()

                val ok = exporter.writeToUri(context, backupUri, encryptedBackupFileBytes)
                if (ok) {
                    AutoBackupPreferences.saveLastBackupFileUri(
                        context,
                        backupUri
                    )
                    Result.success()
                } else {
                    Result.failure()
                }
            } finally {
                password.fill('\u0000')
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}