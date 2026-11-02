package com.ntdpz.pie.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SessionState(val settings: AppSettings = AppSettings(), val locked: Boolean = true, val error: Boolean = false)

class SessionViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SessionState())
    val state = mutable.asStateFlow()
    private var observation: Job? = null

    init { retry() }

    fun retry() {
        observation?.cancel()
        mutable.update { it.copy(error = false) }
        observation = viewModelScope.launch {
            repository.settings.catch {
                mutable.update { it.copy(error = true) }
            }.collect { settings ->
                mutable.update { it.copy(settings = settings, locked = settings.pinEnabled && it.locked, error = false) }
            }
        }
    }

    fun lock() { mutable.update { if (it.settings.pinEnabled) it.copy(locked = true) else it } }
    fun unlock() { mutable.update { it.copy(locked = false) } }
}
