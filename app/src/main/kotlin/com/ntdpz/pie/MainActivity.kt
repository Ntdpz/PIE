package com.ntdpz.pie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.navigation.PieApp
import com.ntdpz.pie.security.PieSessionGate
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val container = (application as PieApplication).container
        setContent {
            PieTheme {
                PieSessionGate(container) { PieApp(container) }
            }
        }
    }
}
