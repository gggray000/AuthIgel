package com.ray.authigel.util

import android.net.Uri
import com.ray.authigel.vault.CodeRecord
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URLDecoder
import java.util.UUID

class CodeRecordImporter {

    fun getLines(inputStream: InputStream): List<String> {
        return BufferedReader(InputStreamReader(inputStream)).readLines();
    }

    fun convertToRecords(otpUris: List<String>): List<CodeRecord> {
        val newRecords = mutableListOf<CodeRecord>()
        otpUris.forEach { uri ->
            val rawUri = Uri.parse(uri)
            val rawLabel = rawUri.lastPathSegment ?: ""
            val label = URLDecoder.decode(rawLabel, "utf-8")
            val (labelIssuer, labelHolder) = if (":" in label) {
                val parts = label.split(":", limit = 2)
                parts[0] to parts[1]
            } else {
                label to ""
            }
            val queryIssuer = rawUri.getQueryParameter("issuer")
            val issuer = queryIssuer ?: labelIssuer

            val holder = labelHolder
            val secret = rawUri.getQueryParameter("secret") ?: ""

            val newRecord = com.ray.authigel.vault.CodeRecord.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setHolder(holder)
                .setSecret(secret)
                .setRawUrl(rawUri.toString())

            newRecords.add(newRecord.build())
        }
        return newRecords;
    }
}