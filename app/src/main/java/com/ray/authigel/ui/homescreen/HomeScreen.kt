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
data class TokenEntry(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val secret: ByteArray,
    val code: String
)

// --- ViewModel to hold the list of cards ---
class HomeViewModel : ViewModel() {
    private val _tokens = mutableStateListOf<TokenEntry>()
    val tokens: List<TokenEntry> get() = _tokens

    fun addToken(label: String, secret: ByteArray) {
        val code = OtpGenerator.generateTOTP(secret)
        _tokens.add(TokenEntry(label = label, secret = secret, code = code))
    }

    fun refreshToken(id: String) {
        val idx = _tokens.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val t = _tokens[idx]
            val newCode = OtpGenerator.generateTOTP(t.secret)
            _tokens[idx] = t.copy(code = newCode)
        }
    }

    fun deleteToken(id: String) {
        _tokens.removeAll { it.id == id }
    }
}

// --- Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // To be replaced later
    val demoSecret = remember { "12345678901234567890".toByteArray(Charsets.US_ASCII) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AuthIgel") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = HedgehogBrown,
                contentColor = Color.White,
                onClick = {
                    vm.addToken(label = "Demo Account", secret = demoSecret)
                    scope.launch { snackbarHostState.showSnackbar("Card added!") }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            if (vm.tokens.isEmpty()) {
                EmptyState(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.tokens, key = { it.id }) { token ->
                        TokenCard(
                            token = token,
                            onRefresh = {
                                vm.refreshToken(token.id)
                                scope.launch { snackbarHostState.showSnackbar("Code refreshed") }
                            },
                            onDelete = {
                                vm.deleteToken(token.id)
                                scope.launch { snackbarHostState.showSnackbar("Card removed") }
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
private fun TokenCard(
    token: TokenEntry,
    onRefresh: () -> Unit,
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
                Text(token.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(formatOtp(token.code), style = MaterialTheme.typography.headlineLarge)
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
                onCycleComplete = onRefresh
            )
        }
    }
}

private fun formatOtp(code: String): String {
    // visually group 6-digit codes like "123 456"
    return if (code.length == 6) code.chunked(3).joinToString(" ") else code
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AuthIgelTheme {
        val previewToken = TokenEntry(
            label = "GitHub",
            secret = ByteArray(0),
            code = "123456"
        )
        Surface { TokenCard(previewToken, onRefresh = {}, onDelete = {}) }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun TotpCountdown(
    periodSec: Int,
    modifier: Modifier = Modifier,
    onCycleComplete: () -> Unit = {}
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
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