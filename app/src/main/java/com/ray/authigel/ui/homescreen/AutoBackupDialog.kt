package com.ray.authigel.ui.homescreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupDialog(
    initialEnabled: Boolean,
    initialPeriodDays: Int,
    initialUri: Uri?,
    onDismiss: () -> Unit,
    onConfirm: (enabled: Boolean, periodDays: Int, uri: Uri?) -> Unit
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var periodDays by remember { mutableStateOf(initialPeriodDays) }
    var selectedUri by remember { mutableStateOf(initialUri) }

    // Period options: adjust as you like
    val periodOptions = listOf(1, 3, 7, 14, 30)
    var periodMenuExpanded by remember { mutableStateOf(false) }

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
                text = "Auto Backup Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable auto backup", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Automatically export backups to the selected location.",
                            style = MaterialTheme.typography.bodySmall
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
                        text = selectedUri?.toString() ?: "No location selected",
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
                        OutlinedTextField(
                            value = "$periodDays day(s)",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
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
            }
        },
        confirmButton = {
            val canConfirm = if(enabled){
                selectedUri !== null && periodDays !== null
            } else {
                true
            }
            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(enabled, periodDays, selectedUri)
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