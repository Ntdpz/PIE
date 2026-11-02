package com.ntdpz.pie.core.domain.diary

sealed interface SaveState {
    data object Idle : SaveState
    data class Saving(val revision: Long) : SaveState
    data class Saved(val revision: Long) : SaveState
    data class Failed(val revision: Long) : SaveState
}
