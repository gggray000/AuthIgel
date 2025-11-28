package com.ray.authigel.ui.homescreen

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ray.authigel.util.OtpGenerator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.LinearProgressIndicator
import com.ray.authigel.ui.theme.AuthIgelTheme
import com.ray.authigel.ui.theme.HedgehogBrown
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ray.authigel.data.CodeRecordVaultViewModel
import com.ray.authigel.util.CodeRecordExporter
import com.ray.authigel.util.CodeRecordImporter
import com.ray.authigel.vault.CodeRecord
import kotlinx.coroutines.delay
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
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            exporter.writeToUri(context, uri, bytes)
        }
    }
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
                val newRecords = importer.convertToRecords(lines);
                newRecords.forEach {
                    vm.add(it)
                }
                scope.launch { snackbarHostState.showSnackbar("Import succeeded.") }
            }
        }
    }
    val qrScanner = com.ray.authigel.util.rememberQrCodeScanner(
        onResult = { qrText ->
            val newRecords = importer.convertToRecords(listOf(qrText))
            newRecords.forEach { vm.add(it) }
            scope.launch { snackbarHostState.showSnackbar("QR code imported") }
        },
        onError = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    )

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
                                text = { Text("Export Backup") },
                                onClick = {
                                    topMenuExpanded = false
                                    val backupStrings = records.map { it.rawUrl }
                                    val backupBytes = backupStrings.joinToString("\n").toByteArray()
                                    pendingExportBytes = backupBytes
                                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                                    val timestamp = LocalDateTime.now().format(formatter)
                                    exportLauncher.launch("export_$timestamp.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import TXT File") },
                                onClick = {
                                    topMenuExpanded = false
                                    importLauncher.launch(arrayOf("text/plain"))
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
                        text = { Text("Scan QR Code") },
                        onClick = {
                            fabMenuExpanded = false
                            qrScanner.startScan()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Manually Input") },
                        onClick = {
                            fabMenuExpanded = false
                            showAddDialog = true
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        val codeForRecord = codes[record.id] ?: "------"
                        CodeRecordCard(
                            record = record,
                            code = codeForRecord,
                            onDelete = {
                                vm.delete(record.id)
                                scope.launch { snackbarHostState.showSnackbar("${record.issuer} Record Removed") }
                            }
                        )
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        AddRecordDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { issuer, holder, secret, url ->
                val record = com.ray.authigel.vault.CodeRecord.newBuilder()
                    .setId(System.currentTimeMillis().toString())
                    .setIssuer(issuer)
                    .setHolder(holder)
                    .setSecret(secret)         // Base32 secret (unchanged)
                    .setRawUrl(url ?: "")
                    .build()
                vm.add(record)
                showAddDialog = false
                scope.launch { snackbarHostState.showSnackbar("Record added") }
            }
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

@Composable
private fun CodeRecordCard(
    record: CodeRecord,
    code: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(record.issuer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(record.holder, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(formatOtp(code), style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}

private fun formatOtp(code: String): String {
    // visually group 6-digit codes like "123 456"
    return if (code.length == 6) code.chunked(3).joinToString(" ") else code
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

            // Calculate which time cycle (period) we’re in
            val currentCycle = ((nowMs / 1000L).toInt()) / periodSec

            // Detect when we’ve entered a new cycle
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

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AuthIgelTheme {
        val previewCode = CodeRecord.newBuilder()
        .setId(System.currentTimeMillis().toString())
        .setIssuer("GitHub")
        .setHolder("you@example.com")
        .setSecret("JBSWY3DPEHPK3PXP")
        .setRawUrl("otpauth://totp/…")
        .build()
        Surface { CodeRecordCard(
            previewCode, "123456", onDelete = {}
        ) }
    }
}