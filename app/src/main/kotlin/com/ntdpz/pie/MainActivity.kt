package com.ntdpz.pie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.navigation.PieApp
import com.ntdpz.pie.security.PieSessionGate
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.domain.settings.AppSettings
import com.ntdpz.pie.core.domain.settings.ThemeMode
import kotlinx.coroutines.flow.catch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val container = (application as PieApplication).container
        setContent {
            val themeSettings = remember(container) {
                container.settingsRepository.settings.catch { emit(AppSettings()) }
            }
            val settings by themeSettings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            PieTheme(darkTheme = dark) {
                PieSessionGate(container) { PieApp(container) }
            }
        }
    }
}
