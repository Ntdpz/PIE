package com.ntdpz.pie.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.backup.BackupRepository
import com.ntdpz.pie.core.domain.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val canRestore: Boolean = false,
    val saved: Boolean = true,
    val message: String? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backups: BackupRepository,
    private val writeScope: CoroutineScope,
) : ViewModel() {
    private val mutable = MutableStateFlow(SettingsUiState())
    val state = mutable.asStateFlow()
    private val pending = AtomicInteger()
    private val writes = Channel<ReminderSettings>(Channel.UNLIMITED)
    private val themeWrites = Channel<ThemeMode>(Channel.UNLIMITED)

    init {
        writeScope.launch {
            for (mode in themeWrites) {
                try { repository.saveTheme(mode) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutable.update { it.copy(message = "เปลี่ยนรูปแบบแอปไม่สำเร็จ ลองอีกครั้ง") } }
            }
        }
        writeScope.launch {
            for (reminder in writes) {
                try { repository.saveReminder(reminder) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutable.update { it.copy(message = "บันทึกการตั้งค่าไม่สำเร็จ") } }
                finally {
                    if (pending.decrementAndGet() == 0) mutable.update { it.copy(saved = it.message == null) }
                }
            }
        }
        viewModelScope.launch {
            repository.settings.catch {
                mutable.update { it.copy(message = "เปิดการตั้งค่าไม่สำเร็จ") }
            }.collect { settings ->
                mutable.update {
                    it.copy(settings = if (pending.get() == 0) settings else settings.copy(reminder = it.settings.reminder))
                }
            }
        }
        refreshRestoreAvailability()
    }

    fun refreshRestoreAvailability() {
        viewModelScope.launch {
            try { mutable.update { it.copy(canRestore = backups.canRestore()) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(canRestore = false) } }
        }
    }

    fun saveReminder(reminder: ReminderSettings) {
        pending.incrementAndGet()
        mutable.update { it.copy(settings = it.settings.copy(reminder = reminder), saved = false, message = null) }
        check(writes.trySend(reminder).isSuccess) { "Settings writer is closed" }
    }

    fun showMessage(message: String) { mutable.update { it.copy(message = message) } }

    fun saveTheme(mode: ThemeMode) {
        check(themeWrites.trySend(mode).isSuccess) { "Theme writer is closed" }
    }

    override fun onCleared() { writes.close(); themeWrites.close() }
}
