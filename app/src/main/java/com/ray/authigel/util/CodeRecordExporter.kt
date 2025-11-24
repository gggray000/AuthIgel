package com.ray.authigel.util

import android.content.Context
import android.net.Uri

class CodeRecordExporter {
    fun writeToUri(context: Context, uri: Uri, byteArray: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(byteArray)
        }
    }

}