package com.ntdpz.pie.feature.editor

import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import java.time.LocalDate
import com.ntdpz.pie.core.domain.media.DiaryMedia

data class EditorUiState(
    val date: LocalDate,
    val text: String = "",
    val formatting: List<TextFormatRange> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
    val media: List<DiaryMedia> = emptyList(),
    val importing: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val message: String? = null,
)
