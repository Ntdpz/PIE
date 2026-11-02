package com.ntdpz.pie.core.data.settings

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ntdpz.pie.core.domain.settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.pieSettings by preferencesDataStore("pie_settings")

class LocalSettingsRepository(context: Context) : SettingsRepository {
    private val store = context.applicationContext.pieSettings
    private val pinMutex = Mutex()
    private val enabled = booleanPreferencesKey("reminder_enabled")
    private val days = stringSetPreferencesKey("reminder_days")
    private val times = stringSetPreferencesKey("reminder_times")
    private val custom = booleanPreferencesKey("reminder_custom")
    private val message = stringPreferencesKey("reminder_message")
    private val salt = stringPreferencesKey("pin_salt")
    private val hash = stringPreferencesKey("pin_hash")
    private val failures = intPreferencesKey("pin_failures")
    private val lockedUntil = longPreferencesKey("pin_locked_until")

    override val settings = store.data.map { p ->
        AppSettings(
            loaded = true,
            pinEnabled = p[hash] != null,
            reminder = ReminderSettings(
                p[enabled] ?: false,
                p[days]?.mapNotNull(String::toIntOrNull)?.toSet() ?: (1..7).toSet(),
                p[times]?.mapNotNull(String::toIntOrNull)?.toSet() ?: setOf(1260),
                p[custom] ?: false,
                p[message].orEmpty(),
            ),
        )
    }

    override suspend fun saveReminder(reminder: ReminderSettings) {
        require(reminder.days.all { it in 1..7 } && reminder.times.all { it in 0..1439 })
        require(!reminder.enabled || (reminder.days.isNotEmpty() && reminder.times.isNotEmpty()))
        store.edit {
            it[enabled] = reminder.enabled
            it[days] = reminder.days.map(Int::toString).toSet()
            it[times] = reminder.times.map(Int::toString).toSet()
            it[custom] = reminder.customMode
            it[message] = reminder.message
        }
    }

    override suspend fun setPin(pin: CharArray, currentPin: CharArray?) = withContext(Dispatchers.IO) {
        pinMutex.withLock {
            require(pin.size == 6 && pin.all(Char::isDigit))
            if (store.data.first()[hash] != null) require(currentPin != null && verifyUnlocked(currentPin))
            val randomSalt = ByteArray(16).also(SecureRandom()::nextBytes)
            val derived = derive(pin, randomSalt)
            store.edit {
                it[salt] = Base64.encodeToString(randomSalt, Base64.NO_WRAP)
                it[hash] = Base64.encodeToString(derived, Base64.NO_WRAP)
                it[failures] = 0
                it[lockedUntil] = 0L
            }
            derived.fill(0)
        }
    }

    override suspend fun verifyPin(pin: CharArray) = withContext(Dispatchers.IO) {
        pinMutex.withLock { verifyUnlocked(pin) }
    }

    override suspend fun disablePin(pin: CharArray) = withContext(Dispatchers.IO) {
        pinMutex.withLock {
            if (!verifyUnlocked(pin)) return@withLock false
            store.edit { it.remove(hash); it.remove(salt); it.remove(failures); it.remove(lockedUntil) }
            true
        }
    }

    private suspend fun verifyUnlocked(pin: CharArray): Boolean {
        val p = store.data.first()
        val remaining = (p[lockedUntil] ?: 0L) - System.currentTimeMillis()
        if (remaining > 0) throw PinRateLimited((remaining + 999) / 1000)
        val storedHash = p[hash] ?: return true
        val actual = derive(pin, Base64.decode(requireNotNull(p[salt]), Base64.NO_WRAP))
        val valid = MessageDigest.isEqual(actual, Base64.decode(storedHash, Base64.NO_WRAP))
        actual.fill(0)
        store.edit {
            val count = if (valid) 0 else (p[failures] ?: 0) + 1
            it[failures] = count
            it[lockedUntil] = if (!valid && count >= 5) System.currentTimeMillis() + 30_000L else 0L
        }
        return valid
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
