package com.ntdpz.pie.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class HistoryViewModel(repository: DiaryRepository, private val clock: Clock) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val mutableState = MutableStateFlow(HistoryUiState(YearMonth.from(today), today))
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.observeSummaries().collect { entries ->
                    mutableState.update { it.copy(entries = entries, loading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun changeMonth(offset: Long) {
        mutableState.update { it.copy(month = it.month.plusMonths(offset)) }
    }

    fun selectDate(date: LocalDate) {
        mutableState.update { it.copy(selectedDate = date) }
    }

    fun jumpToToday() {
        val date = LocalDate.now(clock)
        mutableState.update { it.copy(month = YearMonth.from(date), selectedDate = date, today = date) }
    }
}
