package com.ray.authigel

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.ray.authigel.ui.theme.AuthIgelTheme
import com.ray.authigel.ui.homescreen.HomeScreen
import com.ray.authigel.ui.homescreen.LockedScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuthIgelTheme {
                LockedScreen(activity = this){
                    HomeScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    AuthIgelTheme {
        HomeScreen()
    }
}