package com.ray.authigel.ui.homescreen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ray.authigel.util.BiometricAuthenticator

@Composable
fun LockedScreen(
    activity: FragmentActivity,
    homeScreen: @Composable (() -> Unit),
    timeoutMinutes: Long
) {
    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val authenticator = remember(activity) { BiometricAuthenticator(activity) }
    var lastBackgroundTimestamp by rememberSaveable { mutableLongStateOf(0L) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (lastBackgroundTimestamp != 0L) {
            val elapsedMs = System.currentTimeMillis() - lastBackgroundTimestamp
            val timeoutMs = timeoutMinutes * 60_000L

            if (elapsedMs > timeoutMs) {
                isUnlocked = false
            }
        }
    }

    if (isUnlocked) {
        homeScreen()
    } else {
            UnLockScreen(
                onUnlockClick = {
                    authenticator.authenticate(
                        title = "Unlock AuthIgel",
                        subtitle = "Authenticate to view your 2FA codes.",
                        onSuccess = { isUnlocked = true },
                        onError = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            )
    }
}

@Composable
private fun UnLockScreen(onUnlockClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onUnlockClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Unlock")
                }
            }
        }
    }
}

