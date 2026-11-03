package com.ntdpz.pie.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class HistoryViewModel(private val repository: DiaryRepository, private val clock: Clock, private val media: DiaryMediaRepository) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val mutableState = MutableStateFlow(HistoryUiState(YearMonth.from(today), today))
    val state = mutableState.asStateFlow()
    private var load: Job? = null
    init { retry() }
    fun retry() {
        load?.cancel()
        mutableState.update { it.copy(loading = true, loadFailed = false) }
        load = viewModelScope.launch {
            try {
                observeMemoryDays(repository, media).collect { days ->
                    mutableState.update { it.copy(entries = days.map { it.summary }, memories = days, loading = false) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, loadFailed = true) } }
        }
    }
    fun changeMonth(offset: Long) { mutableState.update { it.copy(month = it.month.plusMonths(offset)) } }
    fun selectDate(date: LocalDate) { mutableState.update { it.copy(selectedDate = date) } }
    fun jumpToToday() {
        val date = LocalDate.now(clock)
        mutableState.update { it.copy(month = YearMonth.from(date), selectedDate = date, today = date) }
    }
}
