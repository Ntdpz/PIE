package com.ntdpz.pie.core.domain.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveReminder(reminder: ReminderSettings)
    suspend fun saveTheme(mode: ThemeMode)
    suspend fun setPin(pin: CharArray, currentPin: CharArray? = null)
    suspend fun verifyPin(pin: CharArray): Boolean
    suspend fun disablePin(pin: CharArray): Boolean
}

class PinRateLimited(val seconds: Long) : Exception()
