package com.ntdpz.pie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.navigation.PieApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PieApplication).container
        setContent {
            PieTheme {
                PieApp(container)
            }
        }
    }
}
