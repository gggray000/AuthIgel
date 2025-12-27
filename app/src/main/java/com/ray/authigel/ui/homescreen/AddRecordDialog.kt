package com.ray.authigel.ui.homescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ray.authigel.util.OtpSeedFactory

@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (issuer: String, holder: String, secret: String, url: String?) -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf(OtpSeedFactory.generateRandomSecret()) }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isUrlValid by remember { mutableStateOf(true) }

    fun validateUrl(u: String) {
        error = null
        runCatching {
            val parsed = u.trim().toUri()
            if (parsed.scheme != "otpauth") throw IllegalArgumentException()
            val labelRaw = parsed.path?.removePrefix("/")?.trim().orEmpty()
            val labelParts = labelRaw.split(":", limit = 2)
            val labelIssuer = parsed.getQueryParameter("issuer")?.trim()
            val labelHolder = if (labelParts.size == 2) labelParts[1].trim() else labelParts[0].trim()
            val s = parsed.getQueryParameter("secret")?.trim().orEmpty()
            if (s.isEmpty()) throw IllegalArgumentException("Missing 'secret' in URL")
            issuer = (labelIssuer ?: (if (labelParts.size == 2) labelParts[0].trim() else "")).ifBlank { issuer }
            holder = labelHolder.ifBlank { holder }
            secret = s.uppercase()
            isUrlValid = true
        }.onFailure { ex ->
            isUrlValid = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Issuer (e.g. Hedgehog Cloud)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it },
                    label = { Text("Holder (e.g., foo@bar.com)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secret,
                    onValueChange = { },
                    label = { Text("Secret (Base32)*") },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    secret = OtpSeedFactory.generateRandomSecret()
                }) {
                    Text("Generate Random Secret")
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { value ->
                        url = value
                        validateUrl(value) },
                    label = { Text("OTPAuth URL (generated or pasted)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isUrlValid,
                    supportingText = {if(!isUrlValid) { Text("Invalid OTPAuth URL.") }}
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    val canBuild = issuer.isNotBlank() && holder.isNotBlank() && secret.isNotBlank()
                    Button(
                        enabled = canBuild,
                        onClick = {
                            error = null
                            if (!canBuild) return@Button
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
                    ) { Text("Generate OTPAuth URL") }
                }

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
            val canSave = issuer.isNotBlank() && holder.isNotBlank() && secret.isNotBlank() && url.isNotBlank() && isUrlValid
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