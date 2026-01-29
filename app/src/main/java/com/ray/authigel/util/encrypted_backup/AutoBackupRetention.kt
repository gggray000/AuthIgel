package com.ray.authigel.util.encrypted_backup

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile

object AutoBackupRetention {

    fun cleanup(context: Context, keep: Int) {
        val treeUri = AutoBackupPreferences.load(context).folderUri ?: return
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return

        val backupDir = tree.findFile("AuthIgelBackups") ?: return

        val backups = backupDir
            .listFiles()
            .filter { file ->
                file.isFile &&
                        file.name?.startsWith("AuthIgel_AutoBackup_") == true
            }
            .map { file ->
                file to file.lastModified()
            }

        Log.d("AutoBackupRetention", "found ${backups.size} backups")

        backups
            .sortedByDescending { it.second }
            .drop(keep)
            .forEach { (file, _) ->
                val deleted = file.delete()
                Log.d(
                    "AutoBackupRetention",
                    "delete ${file.name} → $deleted"
                )
            }
    }
}