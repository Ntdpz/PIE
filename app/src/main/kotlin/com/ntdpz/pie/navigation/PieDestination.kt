package com.ntdpz.pie.navigation

import com.ntdpz.pie.core.designsystem.component.PieGlyph

internal enum class PieDestination(val route: String, val label: String, val glyph: PieGlyph) {
    Home("home", "วันนี้", PieGlyph.Home),
    History("history", "ความทรงจำ", PieGlyph.Book),
    Settings("settings", "ตั้งค่า", PieGlyph.Settings),
}
