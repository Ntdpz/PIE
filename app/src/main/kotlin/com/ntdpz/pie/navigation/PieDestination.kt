package com.ntdpz.pie.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class PieDestination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "หน้าแรก", Icons.Filled.Home),
    History("history", "ย้อนวัน", Icons.Filled.DateRange),
    Settings("settings", "ตั้งค่า", Icons.Filled.Settings),
}
