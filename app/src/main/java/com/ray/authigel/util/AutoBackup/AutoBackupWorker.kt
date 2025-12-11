package com.ray.authigel.util.AutoBackup

import android.content.Context
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )
            val repo = VaultDI.provideRepository(context)
            val records = repo.getAll()
            val bytes = exporter.buildBackupBytes(records)
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "AuthIgel_AutoBackup_$timestamp.txt"

            val backupUri = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "text/plain",
                fileName
            )

            if (backupUri == null) {
                return Result.failure()
            }

            val ok = exporter.writeToUri(context, backupUri, bytes)

            if (ok) Result.success() else Result.failure()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}