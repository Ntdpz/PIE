package com.ntdpz.pie.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Clock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(repository: DiaryRepository, private val clock: Clock) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now(clock))

    val state = today.flatMapLatest { date ->
        repository.observeEntry(date)
            .map { HomeUiState(date = date, entry = it, loading = false) }
            .catch { emit(HomeUiState(date = date, loading = false, loadFailed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(today.value))

    fun refreshDate() {
        today.value = LocalDate.now(clock)
    }
}
