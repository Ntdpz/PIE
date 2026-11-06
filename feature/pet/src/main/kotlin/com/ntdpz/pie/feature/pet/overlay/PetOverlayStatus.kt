package com.ntdpz.pie.feature.pet.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PetOverlayPhase { Off, Starting, Running, Failed }

data class PetOverlayState(val phase: PetOverlayPhase = PetOverlayPhase.Off, val error: String? = null)

/** App-scoped, explicitly injected service feedback. Never persists a stale running flag. */
class PetOverlayStatus {
    private val mutable = MutableStateFlow(PetOverlayState())
    val state = mutable.asStateFlow()

    fun starting() { mutable.value = PetOverlayState(PetOverlayPhase.Starting) }
    fun running() { mutable.value = PetOverlayState(PetOverlayPhase.Running) }
    fun failed(message: String) { mutable.value = PetOverlayState(PetOverlayPhase.Failed, message) }
    fun stopped() {
        mutable.update { if (it.phase == PetOverlayPhase.Failed) it else PetOverlayState() }
    }
}
