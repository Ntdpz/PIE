package com.ntdpz.pie.feature.settings.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PinPurpose { Unlock, Setup, Change, Disable }
enum class PinStage { Verify, New, Confirm }
data class PinUiState(
    val purpose: PinPurpose,
    val stage: PinStage,
    val digits: Int = 0,
    val busy: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false,
)

class PinViewModel(private val repository: SettingsRepository, purpose: PinPurpose) : ViewModel() {
    private val mutable = MutableStateFlow(PinUiState(purpose, if (purpose == PinPurpose.Setup) PinStage.New else PinStage.Verify))
    val state = mutable.asStateFlow()
    private val entered = StringBuilder()
    private var oldPin: CharArray? = null
    private var firstPin: CharArray? = null
    private var verification: Job? = null
    private var generation = 0

    fun restartUnlock() {
        check(state.value.purpose == PinPurpose.Unlock)
        generation++
        verification?.cancel()
        entered.clear()
        mutable.value = PinUiState(PinPurpose.Unlock, PinStage.Verify)
    }

    fun digit(digit: Int) {
        if (state.value.busy || entered.length >= 6) return
        entered.append(digit)
        mutable.update { it.copy(digits = entered.length, error = null) }
        if (entered.length == 6) submit()
    }

    fun backspace() {
        if (state.value.busy || entered.isEmpty()) return
        entered.deleteCharAt(entered.lastIndex)
        mutable.update { it.copy(digits = entered.length, error = null) }
    }

    private fun submit() {
        val pin = entered.toString().toCharArray()
        mutable.update { it.copy(busy = true) }
        val currentGeneration = generation
        verification = viewModelScope.launch {
            try {
                when (state.value.stage) {
                    PinStage.Verify -> verify(pin)
                    PinStage.New -> { firstPin?.fill('\u0000'); firstPin = pin.copyOf(); mutable.update { it.copy(stage = PinStage.Confirm) } }
                    PinStage.Confirm -> {
                        if (pin.contentEquals(firstPin)) {
                            repository.setPin(pin, oldPin)
                            mutable.update { it.copy(completed = true) }
                        } else {
                            firstPin?.fill('\u0000'); firstPin = null
                            mutable.update { it.copy(stage = PinStage.New, error = "PIN ไม่ตรงกัน ลองตั้งใหม่อีกครั้ง") }
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (limited: PinRateLimited) { mutable.update { it.copy(error = "ลองใหม่ใน ${limited.seconds} วินาที") } }
            catch (_: Exception) { mutable.update { it.copy(error = "ทำรายการไม่สำเร็จ ลองอีกครั้ง") } }
            finally {
                pin.fill('\u0000')
                if (currentGeneration == generation) {
                    entered.clear()
                    mutable.update { it.copy(digits = 0, busy = false) }
                }
            }
        }
    }

    private suspend fun verify(pin: CharArray) {
        val valid = if (state.value.purpose == PinPurpose.Disable) repository.disablePin(pin) else repository.verifyPin(pin)
        if (!valid) { mutable.update { it.copy(error = "PIN ไม่ถูกต้อง ลองอีกครั้ง") }; return }
        if (state.value.purpose == PinPurpose.Change) {
            oldPin?.fill('\u0000'); oldPin = pin.copyOf()
            mutable.update { it.copy(stage = PinStage.New) }
        } else mutable.update { it.copy(completed = true) }
    }

    override fun onCleared() { oldPin?.fill('\u0000'); firstPin?.fill('\u0000'); entered.clear() }
}
