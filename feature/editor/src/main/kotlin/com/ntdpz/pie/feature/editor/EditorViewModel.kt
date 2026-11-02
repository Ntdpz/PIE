package com.ntdpz.pie.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import com.ntdpz.pie.core.domain.diary.SaveState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class EditorViewModel(
    private val date: LocalDate,
    private val repository: DiaryRepository,
    private val writer: DiaryWriter,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState(date))
    val state = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            writer.states.collect { states ->
                mutableState.update { it.copy(saveState = states[date] ?: SaveState.Idle) }
            }
        }
    }

    fun load() {
        if (!mutableState.value.loading && !mutableState.value.loadFailed) return
        mutableState.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                val text = writer.pendingText(date) ?: repository.readEntry(date)?.text.orEmpty()
                mutableState.update { it.copy(text = text, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun onTextChanged(text: String) {
        if (mutableState.value.loading || mutableState.value.loadFailed) return
        mutableState.update { it.copy(text = text) }
        writer.submit(date, text)
    }

    fun retrySave() {
        writer.submit(date, mutableState.value.text)
    }
}
