package com.ray.authigel.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class QrCodeScanner internal constructor(
    private val context: Context,
    private val launcher: ActivityResultLauncher<Intent>,
    private val onError: (String) -> Unit
) {

    fun startScan() {
        try {
            val intent = Intent(context, MlKitQrScannerActivity::class.java)
            launcher.launch(intent)
        } catch (e: Exception) {
            onError("Failed to start QR scanner: ${e.message}")
        }
    }
}

@Composable
fun rememberQrCodeScanner(
    onResult: (String) -> Unit,
    onError: (String) -> Unit = {}
): QrCodeScanner {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringExtra("qr_text")
            if (!text.isNullOrEmpty()) {
                onResult(text)
            } else {
                onError("No QR content returned.")
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            onError("QR scanner failed with resultCode=${result.resultCode}.")
        }
    }

    return remember(context, launcher, onError) {
        QrCodeScanner(context, launcher, onError)
    }
}