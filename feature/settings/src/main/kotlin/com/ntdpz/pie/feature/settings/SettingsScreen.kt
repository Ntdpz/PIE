package com.ntdpz.pie.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(versionName: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("ตั้งค่า", style = MaterialTheme.typography.headlineMedium)
        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("เรื่องของคุณ อยู่กับคุณ", style = MaterialTheme.typography.titleLarge)
                Text("บันทึกเก็บไว้บนเครื่องนี้")
            }
        }
        Text("PIE · $versionName", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
