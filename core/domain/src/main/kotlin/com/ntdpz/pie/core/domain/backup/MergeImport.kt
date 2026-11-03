package com.ntdpz.pie.core.domain.backup

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class ImportPreview(
    val token: String,
    val addedDays: Int,
    val skippedDays: Int,
    val conflicts: List<DiaryConflict>,
    val addedMedia: Int,
    val skippedMedia: Int,
    val filledDays: Int = 0,
)

data class ImportResult(val addedDays: Int, val changedDays: Int, val skippedDays: Int, val addedMedia: Int, val skippedMedia: Int)

interface MergeImportRepository {
    val lastImportAtMillis: Flow<Long?>
    suspend fun prepareImport(uri: String, password: CharArray): ImportPreview
    suspend fun commitImport(token: String, choices: Map<LocalDate, MergeChoice>): ImportResult
    suspend fun discardImport(token: String)
}

class ImportPreviewChanged : Exception()
