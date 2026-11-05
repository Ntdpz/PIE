package com.ntdpz.pie.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import com.ntdpz.pie.core.domain.diary.TextFormatting
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var loadJob: Job? = null

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
        if (loadJob?.isActive == true) return
        if (!mutableState.value.loading && !mutableState.value.loadFailed) return
        mutableState.update { it.copy(loading = true, loadFailed = false) }
        loadJob = viewModelScope.launch {
            try {
                val entry = writer.pendingEntry(date) ?: repository.readEntry(date)
                mutableState.update { it.copy(text = entry?.text.orEmpty(), formatting = entry?.formatting.orEmpty(), loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun onTextChanged(text: String) {
        onDocumentChanged(text, TextFormatting.afterEdit(mutableState.value.text, text, mutableState.value.formatting, extendAtEnd = true))
    }

    fun onDocumentChanged(text: String, formatting: List<TextFormatRange>) {
        if (!canEdit()) return
        val normalized = TextFormatting.normalize(text, formatting)
        mutableState.update { it.copy(text = text, formatting = normalized) }
        writer.submit(date, text, normalized)
    }

    fun retrySave() {
        if (!canEdit() || mutableState.value.saveState !is SaveState.Failed) return
        writer.submit(date, mutableState.value.text, mutableState.value.formatting)
    }

    private fun canEdit(): Boolean = mutableState.value.let {
        !it.loading && !it.loadFailed && !it.deleting && !it.deleted
    }

    fun importMedia(uris: List<String>) {
        if (uris.isEmpty() || mutableState.value.importing || !canEdit()) return
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
        if (!canEdit() || mutableState.value.importing) return
        viewModelScope.launch {
            try { mediaRepository.deleteMedia(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(message = "ลบสื่อไม่สำเร็จ") } }
        }
    }

    fun deleteEntry() {
        if (mutableState.value.importing || !canEdit()) return
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
