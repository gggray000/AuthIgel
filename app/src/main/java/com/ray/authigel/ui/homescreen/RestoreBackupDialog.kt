package com.ray.authigel.ui.homescreen

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ray.authigel.data.CodeRecordVaultViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RestoreBackupDialog(
    title: String,
    onDismiss: () -> Unit,
    hasLastBackupUri: Boolean,
    onUseLastBackup: () -> Unit,
    onChooseAnotherPosition: () -> Unit,
    selectedUri: Uri?,
    onConfirm: (uri: Uri, password: CharArray) -> Unit,
    hasExistingPassword: Boolean,
    vm: CodeRecordVaultViewModel,
    scope: CoroutineScope
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                if (hasLastBackupUri) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Existing backup location detected.",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(
                        onClick = onUseLastBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use latest backup file")
                    }
                }

                OutlinedButton(
                    onClick = onChooseAnotherPosition,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose another backup file")
                }

                if (selectedUri != null) {
                    Text(
                        text = "Selected file:\n$selectedUri",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (hasExistingPassword) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Existing password detected.",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                password = vm.getBackupPasswordPlaintext() ?: ""
                            }
                        }
                    ) {
                        Text("Apply existing password")
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation =
                        if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Outlined.Visibility
                                else
                                    Icons.Outlined.VisibilityOff,
                                contentDescription =
                                    if (showPassword) "Hide password"
                                    else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {
            TextButton(
                enabled = selectedUri != null && password.isNotEmpty(),
                onClick = {
                    onConfirm(selectedUri!!, password.toCharArray())
                    password = ""
                }
            ) {
                Text("Restore")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}