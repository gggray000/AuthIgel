package com.ray.authigel.ui.homescreen

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ray.authigel.MainActivity
import com.ray.authigel.ui.theme.HedgehogBrown
import com.ray.authigel.util.BiometricAuthenticator

@Composable
fun LockedScreen(
    activity: FragmentActivity,
    homeScreen: @Composable (() -> Unit),
) {
    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val authenticator = remember(activity) { BiometricAuthenticator(activity) }

    // Run auth as soon as the screen becomes visible
    LaunchedEffect(Unit) {
        isUnlocked = false
    }

    // re-lock on process recreation only (state is saveable in memory).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isUnlocked = false
    }

    if (isUnlocked) {
        homeScreen()
    } else {
            UnLockScreen(
                onUnlockClick = {
                    authenticator.authenticate(
                        title = "Unlock AuthIgel",
                        subtitle = "Authenticate to view your 2FA tokens",
                        onSuccess = { isUnlocked = true },
                        onError = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            )
    }
}

@Composable
private fun UnLockScreen(onUnlockClick: () -> Unit) {
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
                modifier = Modifier.size(72.dp),
                tint = Color.Black,
            )
            Button(onClick = onUnlockClick, colors = ButtonDefaults.buttonColors(
                containerColor = HedgehogBrown,
                contentColor = Color.White)
            ) {
                Text("Unlock")
            }
        }
    }
}

