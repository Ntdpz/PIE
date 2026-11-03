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
    val entries: Map<LocalDate, MemoryEntryState> = emptyMap(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

data class MemoryEntryState(val text: String = "", val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(diary: DiaryRepository, media: DiaryMediaRepository, val initialDate: LocalDate) : ViewModel() {
    private val visibleDates = MutableStateFlow(setOf(initialDate))
    private val retry = MutableStateFlow(0)
    val state = retry.flatMapLatest {
        combine(observeMemoryDays(diary, media), visibleDates) { days, visible -> days to visible }
            .flatMapLatest { (days, visible) ->
                // Observe complete prose only for the viewport and adjacent days.
                val dates = days.flatMapIndexed { index, day ->
                    if (day.summary.date in visible) days.subList((index - 1).coerceAtLeast(0), (index + 2).coerceAtMost(days.size)).map { it.summary.date }
                    else emptyList()
                }.distinct()
                if (dates.isEmpty()) flowOf(MemoryUiState(days = days, loading = false))
                else combine(dates.map { date ->
                    diary.observeEntry(date)
                        .map { date to MemoryEntryState(it?.text.orEmpty()) }
                        .catch { error ->
                            if (error is kotlinx.coroutines.CancellationException) throw error
                            emit(date to MemoryEntryState(failed = true))
                        }
                }) { entries -> MemoryUiState(days = days, entries = entries.toMap(), loading = false) }
            }
            .catch { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                emit(MemoryUiState(loading = false, failed = true))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState())

    fun showDates(dates: Set<LocalDate>) { if (dates.isNotEmpty()) visibleDates.value = dates }
    fun retry() { retry.value++ }
}
