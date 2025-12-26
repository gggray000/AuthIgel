package com.ray.authigel.ui.homescreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun AutoBackupDialog(
    initialEnabled: Boolean,
    initialPeriodDays: Int,
    initialUri: Uri?,
    hasExistingPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (enabled: Boolean, periodDays: Int, uri: Uri?, password: CharArray?) -> Unit
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var periodDays by remember { mutableStateOf(initialPeriodDays) }
    var selectedUri by remember { mutableStateOf(initialUri) }
    var hasExistingPassword by remember { mutableStateOf(hasExistingPassword) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val periodOptions = listOf(1, 3, 7, 14, 30)
    var periodMenuExpanded by remember { mutableStateOf(false) }
    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
        }
    }
    var isUpdatingPassword by remember {
        mutableStateOf(!hasExistingPassword)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Auto Backup Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,

                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable auto backup", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Automatically export encrypted backup to the chosen location.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Export location", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = selectedUri?.toString() ?: "No location chosen",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { treePickerLauncher.launch(null) },
                        enabled = enabled
                    ) {
                        Text("Choose folder")
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Backup period", fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(
                        expanded = periodMenuExpanded,
                        onExpandedChange = { periodMenuExpanded = !periodMenuExpanded }
                    ) {
                        val fillMaxWidth = Modifier.fillMaxWidth()
                        OutlinedTextField(
                            value = "$periodDays day(s)",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                                    enabled = enabled
                                ),
                            enabled = enabled,
                            label = { Text("How often to backup") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodMenuExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = periodMenuExpanded,
                            onDismissRequest = { periodMenuExpanded = false }
                        ) {
                            periodOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("$option day(s)") },
                                    onClick = {
                                        periodDays = option
                                        periodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if(enabled && hasExistingPassword && !isUpdatingPassword) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Notice: Existing password detected.",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if(enabled && hasExistingPassword) {
                        Button(
                            onClick = { isUpdatingPassword = !isUpdatingPassword },
                            enabled = enabled
                        ) {
                            if(!isUpdatingPassword){
                                Text("Reset password")
                            } else {
                                Text("Cancel resetting password")
                            }
                        }
                }

                if (enabled && isUpdatingPassword) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "IMPORTANT: Please make sure to save password safely, this is the only way to decrypt the auto-backup file.",
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
            val canConfirm = if((enabled && !hasExistingPassword )|| (enabled && isUpdatingPassword)) {
                selectedUri != null && password != "" && confirmPassword != "" && confirmPassword == password
            } else {
                true
            }
            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        enabled,
                        periodDays,
                        selectedUri,
                        if (enabled) password.toCharArray() else null
                    )
                    password = ""
                    confirmPassword = ""
                }
            ) {
                Text("Save Preference")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}