package com.ntdpz.pie.core.data.diary

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE date = :date")
    fun observeEntry(date: String): Flow<DiaryEntity?>

    @Query(
        "SELECT date, substr(text, 1, 160) AS preview, updatedAtEpochMillis " +
            "FROM diary_entries WHERE length(trim(text)) > 0 ORDER BY date DESC",
    )
    fun observeSummaries(): Flow<List<DiarySummaryRow>>

    @Query("SELECT * FROM diary_entries WHERE date = :date")
    suspend fun readEntry(date: String): DiaryEntity?

    @Upsert
    suspend fun upsert(entry: DiaryEntity)
}
