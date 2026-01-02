package com.ray.authigel.ui.homescreen

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var showExistingPasswordDetected by remember { mutableStateOf(true) }
    var showExistingBackupDetected by remember { mutableStateOf(true) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore backup", fontWeight = FontWeight.Bold) },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                if (hasLastBackupUri && showExistingBackupDetected) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Text(
                                text = "Existing backup location detected.",
                                modifier = Modifier
                                    .padding(8.dp)
                                    .padding(end = 24.dp),
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { showExistingBackupDetected = false },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
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
                    if(showExistingPasswordDetected){
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box {
                                Text(
                                    text = "Existing peassword detected.",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .padding(end = 24.dp),
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { showExistingPasswordDetected = false },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close"
                                    )
                                }
                            }
                        }
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