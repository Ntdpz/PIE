package com.ntdpz.pie.navigation

import com.ntdpz.pie.core.designsystem.component.PieGlyph

internal enum class PieDestination(val route: String, val label: String, val glyph: PieGlyph) {
    Home("home", "หน้าแรก", PieGlyph.Home),
    History("history", "ย้อนวัน", PieGlyph.Calendar),
    Settings("settings", "ตั้งค่า", PieGlyph.Settings),
}
