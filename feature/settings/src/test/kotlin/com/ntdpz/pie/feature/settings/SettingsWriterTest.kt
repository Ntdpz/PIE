package com.ntdpz.pie.feature.settings

import androidx.lifecycle.ViewModelStore
import com.ntdpz.pie.core.domain.backup.BackupRepository
import com.ntdpz.pie.core.domain.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWriterTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun main() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }

    @Test
    fun rapidEditsPersistTheLatestMessageInOrder() = runTest(dispatcher) {
        val repository = TestSettingsRepository()
        val vm = SettingsViewModel(repository, NoBackup(), backgroundScope)
        val store = ViewModelStore().apply { put("settings", vm) }
        try {
            runCurrent()
            repeat(50) { vm.saveReminder(ReminderSettings(message = "draft-$it", customMode = true)) }
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals("draft-49", repository.settings.value.reminder.message)
            assertEquals((0..49).map { "draft-$it" }, repository.saved)
            assertTrue(vm.state.value.saved)
        } finally { store.clear() }
    }

    @Test
    fun leavingScreenDrainsAlreadyAcceptedChanges() = runTest(dispatcher) {
        val repository = TestSettingsRepository()
        val vm = SettingsViewModel(repository, NoBackup(), backgroundScope)
        val store = ViewModelStore().apply { put("settings", vm) }
        runCurrent()
        vm.saveReminder(ReminderSettings(message = "last edit"))
        store.clear()
        advanceTimeBy(100)
        runCurrent()
        assertEquals("last edit", repository.settings.value.reminder.message)
    }

    @Test
    fun themeChoicesPersistInOrderAfterLeavingScreen() = runTest(dispatcher) {
        val repository = TestSettingsRepository()
        val vm = SettingsViewModel(repository, NoBackup(), backgroundScope)
        val store = ViewModelStore().apply { put("settings", vm) }
        runCurrent()
        vm.saveTheme(ThemeMode.Dark)
        vm.saveTheme(ThemeMode.Light)
        vm.saveTheme(ThemeMode.Dark)
        vm.saveReminder(ReminderSettings(message = "keep reminder"))
        store.clear()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.Dark), repository.themes)
        assertEquals(ThemeMode.Dark, repository.settings.value.themeMode)
        assertEquals("keep reminder", repository.settings.value.reminder.message)
    }
}

private class TestSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings(loaded = true))
    val saved = mutableListOf<String>()
    val themes = mutableListOf<ThemeMode>()
    override suspend fun saveReminder(reminder: ReminderSettings) {
        delay(5)
        saved += reminder.message
        settings.value = settings.value.copy(reminder = reminder)
    }
    override suspend fun setPin(pin: CharArray, currentPin: CharArray?) = Unit
    override suspend fun saveTheme(mode: ThemeMode) {
        delay(5)
        themes += mode
        settings.value = settings.value.copy(themeMode = mode)
    }
    override suspend fun verifyPin(pin: CharArray) = false
    override suspend fun disablePin(pin: CharArray) = false
}

private class NoBackup : BackupRepository {
    override suspend fun export(uri: String, password: CharArray) = Unit
    override suspend fun restore(uri: String, password: CharArray) = Unit
    override suspend fun canRestore() = false
}
