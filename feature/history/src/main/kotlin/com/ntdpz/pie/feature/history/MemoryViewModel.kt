package com.ntdpz.pie.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class MemoryUiState(
    val days: List<MemoryDay> = emptyList(),
    val date: LocalDate,
    val text: String = "",
    val loading: Boolean = true,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(diary: DiaryRepository, media: DiaryMediaRepository, val initialDate: LocalDate) : ViewModel() {
    private val selected = MutableStateFlow(initialDate)
    private val retry = MutableStateFlow(0)
    val state = retry.flatMapLatest {
        combine(observeMemoryDays(diary, media), selected.flatMapLatest { date ->
            diary.observeEntry(date).map { date to it?.text.orEmpty() }
        }) { days, entry -> MemoryUiState(days, entry.first, entry.second, loading = false) }
            .catch { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                emit(MemoryUiState(date = selected.value, loading = false, failed = true))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState(date = initialDate))

    fun selectDate(date: LocalDate) { selected.value = date }
    fun retry() { retry.value++ }
}
