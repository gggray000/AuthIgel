package com.ray.authigel.ui.homescreen

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ray.authigel.data.BackupPasswordKeystore
import com.ray.authigel.data.CodeRecordVaultViewModel
import com.ray.authigel.ui.theme.HedgehogBrown
import com.ray.authigel.util.CodeRecordExporter
import com.ray.authigel.util.CodeRecordImporter
import com.ray.authigel.util.OtpGenerator
import com.ray.authigel.util.encrypted_backup.AutoBackupScheduler
import com.ray.authigel.util.encrypted_backup.EncryptedBackupFrequency
import com.ray.authigel.util.encrypted_backup.EncryptedBackupPreferences
import com.ray.authigel.util.rememberQrCodeScanner
import com.ray.authigel.vault.CodeRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val vm: CodeRecordVaultViewModel = viewModel()
    val records by vm.records.collectAsState()
    var codes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var topMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exporter = remember { CodeRecordExporter() }
    val backupBytes = remember(records) { exporter.buildBackupBytes(records) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val result = exporter.writeToUri(context, uri, backupBytes)
            scope.launch {
                if (result) {
                    snackbarHostState.showSnackbar("Successfully wrote ${records.size} record(s) to $uri")
                } else {
                    snackbarHostState.showSnackbar("Failed to export backup.")
                }
            }
        }
    }
    val hasExistingPassword by vm.hasPassword.collectAsState()
    var showEncryptedBackupDialog by remember { mutableStateOf(false) }
    val importer = remember { CodeRecordImporter() }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) {uri ->
        if (uri != null) {
            var inputStream: InputStream? = null;
            try{
                inputStream = context.contentResolver.openInputStream(uri)
            } catch (_: FileNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar("Cannot Open File.") }
                return@rememberLauncherForActivityResult
            }
            if (inputStream != null) {
                val lines = importer.getLines(inputStream);
                val totalCounts = lines.size;
                var successfulCounts = 0;
                val newRecords = importer.convertToRecords(lines);
                newRecords.forEach {
                    vm.add(it)
                    successfulCounts += 1
                }
                scope.launch { snackbarHostState.showSnackbar("Successfully imported $successfulCounts out of $totalCounts records.") }
            }
        }
    }
    val qrScanner = rememberQrCodeScanner(
        onResult = { qrText ->
            val newRecords = importer.convertToRecords(listOf(qrText))
            newRecords.forEach { vm.add(it) }
            scope.launch { snackbarHostState.showSnackbar("QR code imported") }
        },
        onError = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    )
    var showRestoreDialog by remember { mutableStateOf(false) }
    val lastBackupFileUri = EncryptedBackupPreferences.loadLastBackupFileUri(context)
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedRestoreUri = uri
        }
    }
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState
    ) { from, to ->
        vm.move(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    var recordPendingDeletion by remember {mutableStateOf<CodeRecord?>(null)}

    LaunchedEffect(records) {
        codes = refreshCodes(records)
    }

    Scaffold(
        topBar = {
            Column (
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = { Text("AuthIgel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = HedgehogBrown) },
                    actions = {
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = HedgehogBrown)
                        }

                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export plain text records") },
                                onClick = {
                                    topMenuExpanded = false
                                    val timestamp = LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                                    exportLauncher.launch("export_$timestamp.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Encrypted Backup settings") },
                                onClick = {
                                    topMenuExpanded = false
                                    showEncryptedBackupDialog = true
                                }
                            )
                        }
                    }
                )

                TotpCountdown(
                    periodSec = 30,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
                    onCycleComplete = { codes = refreshCodes(records) }
                )
                 } },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    containerColor = HedgehogBrown,
                    contentColor = Color.White,
                    onClick = { fabMenuExpanded = true }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Scan QR-Code") },
                        onClick = {
                            fabMenuExpanded = false
                            qrScanner.startScan()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add record manually") },
                        onClick = {
                            fabMenuExpanded = false
                            showAddDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import TXT file") },
                        onClick = {
                            fabMenuExpanded = false
                            importLauncher.launch(arrayOf("text/plain"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Restore encrypted backup") },
                        onClick = {
                            fabMenuExpanded = false
                            showRestoreDialog = true
                        }
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            if (records.isEmpty()) {
                EmptyState(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.id }) { record ->

                        ReorderableItem(reorderState, key = record.id) { isDragging ->
                            val interactionSource = remember { MutableInteractionSource() }
                            ReorderableCodeRecordItem(
                                record = record,
                                code = codes[record.id] ?: "------",
                                scope = this,
                                interactionSource = interactionSource,
                                isDragging = isDragging,
                                hapticFeedback = hapticFeedback,
                                onDelete = { recordPendingDeletion = record }
                            )
                        }

                    }
                }
            }
        }
    }
    if (recordPendingDeletion != null) {
        val issuer = recordPendingDeletion!!.issuer
        AlertDialog(
            onDismissRequest = {
                recordPendingDeletion = null
            },
            title = {
                Text("Confirmation")
            },
            text = {
                Text(
                    "Are you sure to delete record from $issuer?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {

                        vm.delete(recordPendingDeletion!!.id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Record from $issuer has been deleted")
                        }
                        recordPendingDeletion = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        recordPendingDeletion = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showAddDialog) {
        AddRecordDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { issuer, holder, secret, url ->
                val record = CodeRecord.newBuilder()
                    .setId(System.currentTimeMillis().toString())
                    .setIssuer(issuer)
                    .setHolder(holder)
                    .setSecret(secret)         // Base32 secret (unchanged)
                    .setRawUrl(url ?: "")
                    .setAddedAt(System.currentTimeMillis())
                    .build()
                vm.add(record)
                showAddDialog = false
                scope.launch { snackbarHostState.showSnackbar("Record added") }
            }
        )
    }
    if (showEncryptedBackupDialog) {
        val (backupFrequency, backupFolderUri) = EncryptedBackupPreferences.load(context)
        EncryptedBackupDialog(
            encryptedBackupFrequency = backupFrequency,
            initialUri = backupFolderUri,
            hasExistingPassword = hasExistingPassword,
            onDismiss = { showEncryptedBackupDialog = false },
            onConfirm = { newOptions, uri, password ->
                showEncryptedBackupDialog = false
                scope.launch {
                    EncryptedBackupPreferences.save(context, newOptions, uri)
                    if (backupFrequency != EncryptedBackupFrequency.Never && uri != null) {
                        if (password != null && password.isNotEmpty()) {
                            val passwordCopy = password.copyOf()
                            val encrypted = BackupPasswordKeystore.encrypt(passwordCopy)
                            vm.storeEncryptedBackupPassword(encrypted)
                            password.fill('\u0000')
                        }
                        when (newOptions) {
                            EncryptedBackupFrequency.Never -> {
                                AutoBackupScheduler.cancelPeriodicBackup(context)
                                snackbarHostState.showSnackbar("Encrypted backup disabled.")
                            }

                            EncryptedBackupFrequency.Once -> {
                                AutoBackupScheduler.cancelPeriodicBackup(context)
                                AutoBackupScheduler.runImmediateBackup(context)
                                snackbarHostState.showSnackbar("Backup saved.")
                            }

                            is EncryptedBackupFrequency.Periodic -> {
                                AutoBackupScheduler.runImmediateBackup(context)
                                AutoBackupScheduler.schedulePeriodicBackup(
                                    context = context,
                                    periodDays = newOptions.days
                                )
                                snackbarHostState.showSnackbar("Auto backup enabled (every ${newOptions.days} day(s)).")
                            }
                        }
                    }
                }
            }
        )
    }
    if (showRestoreDialog) {
        RestoreBackupDialog(
            onDismiss = {
                showRestoreDialog = false
                selectedRestoreUri = null
            },
            hasLastBackupUri = lastBackupFileUri != null,
            onUseLastBackup = {
                selectedRestoreUri = lastBackupFileUri
            },
            onChooseAnotherPosition = {
                restoreFilePicker.launch(arrayOf("*/*"))
            },
            selectedUri = selectedRestoreUri,
            onConfirm = { uri, password ->
                showRestoreDialog = false
                val passwordCopy = password.copyOf()
                scope.launch {
                    try {
                        context.contentResolver.openInputStream(uri)?.use {
                            when (val dec =
                                importer.decryptEncryptedBackup(it, password)) {

                                is CodeRecordImporter.DecryptResult.Success -> {

                                    val encrypted = BackupPasswordKeystore.encrypt(passwordCopy)
                                    vm.storeEncryptedBackupPassword(encrypted)

                                    dec.lines
                                        .let(importer::convertToRecords)
                                        .forEach(vm::add)

                                    snackbarHostState.showSnackbar("Backup restored successfully")
                                }
                                is CodeRecordImporter.DecryptResult.WrongPassword ->
                                    snackbarHostState.showSnackbar("Wrong password")

                                is CodeRecordImporter.DecryptResult.InvalidFormat ->
                                    snackbarHostState.showSnackbar(
                                        "Invalid backup: ${dec.reason}"
                                    )
                            }
                        }
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Restore failed")
                    } finally {
                        passwordCopy.fill('\u0000')
                    }
                }
            },
            hasExistingPassword = hasExistingPassword,
            vm = vm,
            scope = scope
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No Records yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Tap the + button to add your first code.")
    }
}

private fun refreshCodes(records: List<CodeRecord>): Map<String, String> {
    return records.associate { record ->
        val code = OtpGenerator.generateTOTP(
            record.secret
        )
        record.id to code
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun TotpCountdown(
    periodSec: Int,
    modifier: Modifier = Modifier,
    onCycleComplete: () -> Unit = {}
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastCycle by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            val currentCycle = ((nowMs / 1000L).toInt()) / periodSec
            if (lastCycle != -1 && currentCycle != lastCycle) {
                onCycleComplete()
            }
            lastCycle = currentCycle
            delay(100)
        }
    }

    val elapsed = ((nowMs / 1000L).toInt()) % periodSec
    val progress = 1f - (elapsed.toFloat() / periodSec.toFloat())

    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = HedgehogBrown,
        trackColor = Color.LightGray
    )
}