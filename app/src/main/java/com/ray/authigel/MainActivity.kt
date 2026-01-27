package com.ray.authigel

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.ray.authigel.data.VaultDI
import com.ray.authigel.ui.homescreen.HomeScreen
import com.ray.authigel.ui.theme.AuthIgelTheme
import com.ray.authigel.ui.theme.ThemeMode

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        VaultDI.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }

            AuthIgelTheme(themeMode = themeMode) {
                HomeScreen(
                    themeMode = themeMode,
                    onSetThemeMode = { themeMode = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    AuthIgelTheme {
        HomeScreen(
            themeMode = ThemeMode.SYSTEM,
            onSetThemeMode = {}
        )
    }
}