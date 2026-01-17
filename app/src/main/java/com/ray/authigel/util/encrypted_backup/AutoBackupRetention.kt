package com.ray.authigel.util.encrypted_backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object AutoBackupRetention {

    fun cleanup(context: Context, keep: Int) {
        val folderUri = AutoBackupPreferences.load(context).folderUri
            ?: return

        val resolver = context.contentResolver

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )

        val backups = mutableListOf<Pair<Uri, Long>>()

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(2)
                if (!name.startsWith("AuthIgel_AutoBackup_")) continue

                val docId = cursor.getString(0)
                val modified = cursor.getLong(1)

                val uri = DocumentsContract.buildDocumentUriUsingTree(
                    folderUri, docId
                )
                backups += uri to modified
            }
        }

        backups
            .sortedByDescending { it.second }
            .drop(keep)
            .forEach { (uri, _) ->
                DocumentsContract.deleteDocument(resolver, uri)
            }
    }
}