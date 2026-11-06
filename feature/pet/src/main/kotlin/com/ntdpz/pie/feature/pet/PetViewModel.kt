package com.ntdpz.pie.feature.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.pet.PetBehavior
import com.ntdpz.pie.core.domain.pet.PetPose
import com.ntdpz.pie.core.domain.pet.PetPosition
import com.ntdpz.pie.core.domain.pet.PetPreferences
import com.ntdpz.pie.core.domain.pet.PetRepository
import com.ntdpz.pie.core.domain.pet.PetSaveCelebration
import com.ntdpz.pie.core.domain.settings.ReminderMessages
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PetUiState(
    val preferences: PetPreferences = PetPreferences(),
    val loaded: Boolean = false,
    val pose: PetPose = PetPose.Idle,
    val dailyMessage: String = "",
    val error: String? = null,
)

class PetViewModel(
    private val repository: PetRepository,
    private val saves: StateFlow<Map<LocalDate, SaveState>>,
    private val clock: Clock,
    private val nowMillis: () -> Long,
    writeScope: CoroutineScope,
) : ViewModel() {
    private sealed interface Change {
        data class Enabled(val value: Boolean) : Change
        data class ReducedMotion(val value: Boolean) : Change
        data class Position(val value: PetPosition) : Change
    }

    private val mutable = MutableStateFlow(PetUiState())
    val state = mutable.asStateFlow()
    private val changes = Channel<Change>(Channel.UNLIMITED)
    private val celebration = PetSaveCelebration()
    private var loadJob: Job? = null
    private var greetedDate: LocalDate? = null
    private var active = false
    private var lastInteraction = nowMillis()
    private var reactionAt = lastInteraction
    private var reaction: PetPose? = null

    init {
        reload()
        writeScope.launch {
            for (change in changes) {
                try {
                    when (change) {
                        is Change.Enabled -> repository.setEnabled(change.value)
                        is Change.ReducedMotion -> repository.setReducedMotion(change.value)
                        is Change.Position -> repository.setPosition(change.value)
                    }
                    mutable.update { it.copy(error = null) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutable.update { it.copy(error = "บันทึกการตั้งค่า PIE ไม่สำเร็จ ลองอีกครั้ง") }
                }
            }
        }
        viewModelScope.launch {
            saves.collect {
                if (celebration.observe(it) && active) react(PetPose.Saved)
            }
        }
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                repository.preferences.collect { preferences ->
                    mutable.update { it.copy(preferences = preferences, loaded = true, error = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(loaded = false, error = "เปิดการตั้งค่า PIE ไม่สำเร็จ") }
            }
        }
    }

    fun resume() {
        active = true
        lastInteraction = nowMillis()
        val date = LocalDate.now(clock)
        mutable.update { it.copy(dailyMessage = ReminderMessages.defaultForDay(date)) }
        if (greetedDate != date) {
            greetedDate = date
            react(PetPose.Greeting)
        }
    }

    fun pause() {
        active = false
        reaction = null
        celebration.dismiss()
        mutable.update { it.copy(pose = PetPose.Idle) }
    }

    fun tick() {
        if (!active) return
        val date = LocalDate.now(clock)
        if (greetedDate != date) {
            greetedDate = date
            mutable.update { it.copy(dailyMessage = ReminderMessages.defaultForDay(date)) }
            react(PetPose.Greeting)
        }
        val pose = PetBehavior.pose(nowMillis(), lastInteraction, reaction, reactionAt)
        mutable.update { it.copy(pose = pose) }
    }

    fun tap() = react(PetPose.Tap)

    fun beginEditing(date: LocalDate) = celebration.begin(date, saves.value)

    fun mediaCommitted(date: LocalDate) = celebration.mediaCommitted(date)

    fun finishEditing(date: LocalDate) {
        if (celebration.finish(date, saves.value) && active) react(PetPose.Saved)
    }

    fun setEnabled(enabled: Boolean) { changes.trySend(Change.Enabled(enabled)) }

    fun setReducedMotion(enabled: Boolean) { changes.trySend(Change.ReducedMotion(enabled)) }

    fun setPosition(position: PetPosition) { changes.trySend(Change.Position(position)) }

    private fun react(pose: PetPose) {
        lastInteraction = nowMillis()
        reactionAt = lastInteraction
        reaction = pose
        mutable.update { it.copy(pose = pose) }
    }

    override fun onCleared() {
        changes.close()
        super.onCleared()
    }
}
