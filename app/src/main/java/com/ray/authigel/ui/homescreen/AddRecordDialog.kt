package com.ray.authigel.ui.homescreen

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ray.authigel.util.OtpSeedFactory

@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (issuer: String, holder: String, secret: String, url: String?) -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val initialSecret = remember { OtpSeedFactory.randomSecret() }

    fun applyUrl(u: String) {
        error = null
        runCatching {
            val parsed = Uri.parse(u.trim())
            if (parsed.scheme != "otpauth") throw IllegalArgumentException("Not an otpauth:// URL")

            val labelRaw = parsed.path?.removePrefix("/")?.trim().orEmpty()
            val labelParts = labelRaw.split(":", limit = 2)
            val labelIssuer = parsed.getQueryParameter("issuer")?.trim()
            val labelHolder = if (labelParts.size == 2) labelParts[1].trim() else labelParts[0].trim()
            val s = parsed.getQueryParameter("secret")?.trim().orEmpty()
            if (s.isEmpty()) throw IllegalArgumentException("Missing 'secret' in URL")

            issuer = (labelIssuer ?: (if (labelParts.size == 2) labelParts[0].trim() else "")).ifBlank { issuer }
            holder = labelHolder.ifBlank { holder }
            secret = s.uppercase()
        }.onFailure { ex ->
            error = ex.message ?: "Invalid otpauth URL"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Issuer (e.g., GitHub)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it },
                    label = { Text("Holder (e.g., you@example.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secret.ifBlank { initialSecret },
                    onValueChange = {
                        secret = it.filter { ch -> ch.isLetterOrDigit() }.uppercase()
                    },
                    label = { Text("Secret (Base32)") },
                    singleLine = true,
                    supportingText = { Text("Use A–Z and 2–7; no spaces") },
                    trailingIcon = {
                        TextButton(onClick = { secret = OtpSeedFactory.randomSecret() }) {
                            Text("Generate")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    val canBuild = issuer.isNotBlank() && holder.isNotBlank() && secret.isNotBlank()
                    TextButton(
                        enabled = canBuild,
                        onClick = {
                            error = null
                            if (!canBuild) return@TextButton
                            runCatching {
                                url = OtpSeedFactory.buildOtpAuthUrl(
                                    issuer = issuer.trim(),
                                    holder = holder.trim(),
                                    secretBase32 = secret.trim().uppercase()
                                )
                            }.onFailure { ex ->
                                error = ex.message ?: "Failed to build URL"
                            }
                        }
                    ) { Text("Create URL") }
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (it.startsWith("otpauth://")) applyUrl(it)
                    },
                    label = { Text("OTPAuth URL (auto or paste)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            val canSave = issuer.isNotBlank() && holder.isNotBlank() && secret.isNotBlank()
            TextButton(
                enabled = canSave,
                onClick = {
                    onConfirm(
                        issuer.trim(),
                        holder.trim(),
                        secret.trim().uppercase(),
                        url.takeIf { it.isNotBlank() }
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}