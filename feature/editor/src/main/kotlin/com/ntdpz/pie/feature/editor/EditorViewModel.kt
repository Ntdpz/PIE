package com.ntdpz.pie.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
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
    private val mediaRepository: DiaryMediaRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState(date))
    val state = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            mediaRepository.observeMedia(date).collect { media ->
                mutableState.update { it.copy(media = media) }
            }
        }
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
        if (mutableState.value.loading || mutableState.value.loadFailed || mutableState.value.deleting) return
        mutableState.update { it.copy(text = text) }
        writer.submit(date, text)
    }

    fun retrySave() {
        writer.submit(date, mutableState.value.text)
    }

    fun importMedia(uris: List<String>) {
        if (uris.isEmpty() || mutableState.value.importing) return
        viewModelScope.launch {
            mutableState.update { it.copy(importing = true, message = null) }
            try {
                uris.forEach { mediaRepository.importMedia(date, it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(message = "เพิ่มสื่อไม่สำเร็จ โปรดตรวจพื้นที่ว่างแล้วลองใหม่") }
            } finally {
                mutableState.update { it.copy(importing = false) }
            }
        }
    }

    fun deleteMedia(id: String) {
        viewModelScope.launch {
            try { mediaRepository.deleteMedia(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(message = "ลบสื่อไม่สำเร็จ") } }
        }
    }

    fun deleteEntry() {
        if (mutableState.value.importing || mutableState.value.deleting) return
        mutableState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            val result = writer.delete(date)
            mutableState.update {
                it.copy(deleting = false, deleted = result.isSuccess,
                    message = if (result.isFailure) "ลบบันทึกไม่สำเร็จ ลองใหม่อีกครั้ง" else null)
            }
        }
    }
}
