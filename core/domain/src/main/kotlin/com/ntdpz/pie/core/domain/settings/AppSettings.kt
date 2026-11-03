package com.ntdpz.pie.core.domain.settings

enum class ThemeMode { System, Light, Dark }

data class ReminderSettings(
    val enabled: Boolean = false,
    val days: Set<Int> = (1..7).toSet(),
    val times: Set<Int> = setOf(21 * 60),
    val customMode: Boolean = false,
    val message: String = "",
)

data class AppSettings(
    val loaded: Boolean = false,
    val pinEnabled: Boolean = false,
    val reminder: ReminderSettings = ReminderSettings(),
    val themeMode: ThemeMode = ThemeMode.System,
)
