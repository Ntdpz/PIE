package com.ntdpz.pie.feature.editor

import com.ntdpz.pie.core.domain.diary.SaveState
import java.time.LocalDate

data class EditorUiState(
    val date: LocalDate,
    val text: String = "",
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
)
