package com.ntdpz.pie.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable
fun HomeRoute(viewModel: HomeViewModel, onOpenDiary: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }
    HomeScreen(state = state, onWrite = { onOpenDiary(state.date) })
}
