package com.ray.authigel.ui.homescreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptedBackupDialog(
    context: Context,
    initiallyEnabled: Boolean,
    initialUri: Uri?,
    hasExistingPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (enabled: Boolean, uri: Uri?, password: CharArray?) -> Unit
) {
    var enabled by remember { mutableStateOf(initiallyEnabled) }
    var selectedUri by remember { mutableStateOf(initialUri) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(!hasExistingPassword) }
    var showExistingPasswordDetected by remember { mutableStateOf(true) }
    var showPasswordTip by remember { mutableStateOf(true) }

    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Encrypted Backup Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable encrypted backups",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                    }

                    Text(
                        text = "Encrypted backup will be automatically exported to the chosen location when the app starts. A maximum of 5 backup files will be retained.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if(enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if(selectedUri != null) {
                            Text("Current backup location", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Backup location", fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            text = selectedUri?.toString() ?: "No location chosen",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                    )
                                }
                                treePickerLauncher.launch(intent) },
                            enabled = enabled
                        ) {
                            if(selectedUri != null) {
                                Text("Update backup location")
                            } else {
                                Text("Choose backup location")
                            }

                        }
                    }
                }


                if (enabled && hasExistingPassword && !isUpdatingPassword && showExistingPasswordDetected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Text(
                                text = "Existing password detected.",
                                modifier = Modifier
                                    .padding(8.dp)
                                    .padding(end = 24.dp)
                                    .align(Alignment.Center),
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

                if (enabled && hasExistingPassword) {
                    Button(onClick = { isUpdatingPassword = !isUpdatingPassword }) {
                        Text(
                            if (isUpdatingPassword)
                                "Cancel resetting password"
                            else
                                "Reset password"
                        )
                    }
                }

                if (enabled && isUpdatingPassword) {

                    if(showPasswordTip) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box {
                                Text(
                                    text = "IMPORTANT: Save this password safely. It is required to decrypt backups.",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .padding(end = 24.dp)
                                        .align(Alignment.Center),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError
                                )

                                IconButton(
                                    onClick = { showPasswordTip = false },
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

                    var passwordTouched by remember { mutableStateOf(false) }
                    val isPasswordValid = password.length >= 6 && password.any{ it.isLetter() } && password.any{ it.isDigit() };

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordTouched = true },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation =
                            if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector =
                                        if (showPassword)
                                            Icons.Outlined.Visibility
                                        else
                                            Icons.Outlined.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = passwordTouched && !isPasswordValid
                    )

                    if(passwordTouched && !isPasswordValid){
                        Text(
                            "Password must be at least 6 characters long, and a combination of letters and digits.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        visualTransformation =
                            if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text(
                            "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = !enabled ||
                    (selectedUri != null && (!isUpdatingPassword || (password.isNotEmpty() && confirmPassword == password)))

            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        enabled,
                        selectedUri,
                        if (enabled) password.toCharArray() else null
                    )
                }
            ) {
                Text("Save Preference")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}