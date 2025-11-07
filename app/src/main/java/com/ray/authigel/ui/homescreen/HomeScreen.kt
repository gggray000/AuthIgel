package com.ray.authigel.ui.homescreen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ray.authigel.domain.OtpGenerator
import java.util.UUID
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.ray.authigel.ui.theme.AuthIgelTheme
import com.ray.authigel.ui.theme.HedgehogBrown
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay

// --- Model ---
data class CodeRecord(
    val id: String = UUID.randomUUID().toString(),
    val issuer: String,
    val holder: String,
    val secret: String,
    val rawUri: String? = null,
)

// --- ViewModel to hold the list of cards ---
class HomeViewModel : ViewModel() {
    private val _records = mutableStateListOf<CodeRecord>()
    val records: List<CodeRecord> get() = _records

    fun addToken(issuer: String, holder: String, secret: String) {
        _records.add(CodeRecord(issuer = issuer,  holder = holder, secret = secret))
    }

    fun deleteToken(id: String) {
        _records.removeAll { it.id == id }
    }
}

// --- Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // To be replaced later
    val demoSecret = remember { "12345678901234567890" }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AuthIgel") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = HedgehogBrown,
                contentColor = Color.White,
                onClick = {
                    vm.addToken(issuer = "Demo Service", holder = "Dev", secret = demoSecret)
                    scope.launch { snackbarHostState.showSnackbar("Card Added!") }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            if (vm.records.isEmpty()) {
                EmptyState(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.records, key = { it.id }) { record ->
                        CodeRecordCard(
                            record = record,
                            onDelete = {
                                vm.deleteToken(record.id)
                                scope.launch { snackbarHostState.showSnackbar("${record.issuer} Card Removed") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No tokens yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Tap the + button to add your first code.")
    }
}

@Composable
private fun CodeRecordCard(
    record: CodeRecord,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember {
    mutableStateOf(OtpGenerator.generateTOTP(record.secret.toByteArray(Charsets.US_ASCII)))
    }

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
                Text(record.issuer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(record.holder, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
                Spacer(Modifier.height(4.dp))
                Text(formatOtp(code), style = MaterialTheme.typography.headlineLarge)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TotpCountdown(
                periodSec = 15,
                onCycleComplete = { code = OtpGenerator.generateTOTP(record.secret.toByteArray(Charsets.US_ASCII)) }
            )
        }
    }
}

private fun formatOtp(code: String): String {
    // visually group 6-digit codes like "123 456"
    return if (code.length == 6) code.chunked(3).joinToString(" ") else code
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
    val remaining = periodSec - elapsed
    val progress = 1f - (elapsed.toFloat() / periodSec.toFloat())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Expires in", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp))
            CircularCountdown(
                remaining = remaining,
                progress = progress,
                size = 32.dp,
                stroke = 3.dp
            )
        }
}

@Composable
private fun CircularCountdown(
    remaining: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp,
    stroke: Dp
) {
    Box(
        modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.matchParentSize(),
            color = HedgehogBrown,
            trackColor = Color.Transparent,
            strokeWidth = stroke
        )
        Text(
            text = "${remaining}s",
            style = MaterialTheme.typography.labelSmall
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AuthIgelTheme {
        val previewToken = CodeRecord(
            issuer = "Demo Service",
            holder = "Dev",
            secret = "12345678901234567890",
        )
        Surface { CodeRecordCard(
            previewToken, onDelete = {}
        ) }
    }
}