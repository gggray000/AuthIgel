package com.ray.authigel.ui.homescreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ray.authigel.util.encrypted_backup.EncryptedBackupFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptedBackupDialog(
    encryptedBackupFrequency: EncryptedBackupFrequency,
    initialUri: Uri?,
    hasExistingPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (options: EncryptedBackupFrequency, uri: Uri?, password: CharArray?) -> Unit
) {
    var backupOptionsState by remember {
        mutableStateOf(encryptedBackupFrequency)
    }
    val enabled = backupOptionsState != EncryptedBackupFrequency.Never
    val frequencyLabel = when (backupOptionsState) {
        EncryptedBackupFrequency.Never -> "Never"
        EncryptedBackupFrequency.Once -> "Only once"
        is EncryptedBackupFrequency.Periodic ->
            "${(backupOptionsState as EncryptedBackupFrequency.Periodic).days} day(s)"
    }
    var selectedUri by remember { mutableStateOf(initialUri) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(!hasExistingPassword) }

    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
        }
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    ExposedDropdownMenuBox(
                        expanded = periodMenuExpanded,
                        onExpandedChange = { periodMenuExpanded = !periodMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = frequencyLabel,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                                    enabled = true
                                ),
                            label = { Text("Backup frequency") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(periodMenuExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = periodMenuExpanded,
                            onDismissRequest = { periodMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Never") },
                                onClick = {
                                    backupOptionsState = EncryptedBackupFrequency.Never
                                    periodMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Only once") },
                                onClick = {
                                    backupOptionsState = EncryptedBackupFrequency.Once
                                    periodMenuExpanded = false
                                }
                            )
                            listOf(1, 3, 7, 30).forEach { days ->
                                DropdownMenuItem(
                                    text = { Text("$days day(s)") },
                                    onClick = {
                                        backupOptionsState = EncryptedBackupFrequency.Periodic(days)
                                        periodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if(enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Backup location", fontWeight = FontWeight.SemiBold)

                        Text(
                            text = selectedUri?.toString() ?: "No location chosen",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )

                        Button(
                            onClick = { treePickerLauncher.launch(null) },
                            enabled = enabled
                        ) {
                            Text("Choose folder")
                        }
                    }
                }


                if (enabled && hasExistingPassword && !isUpdatingPassword) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Notice: Existing password detected.",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold
                        )
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "IMPORTANT: Save this password safely. It is required to decrypt backups.",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
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
                        }
                    )

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
            val canConfirm = when (backupOptionsState) {
                EncryptedBackupFrequency.Never -> true
                EncryptedBackupFrequency.Once,
                is EncryptedBackupFrequency.Periodic -> {
                    selectedUri != null &&
                            (!isUpdatingPassword || (password.isNotEmpty() && confirmPassword == password))
                }
            }

            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        backupOptionsState,
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