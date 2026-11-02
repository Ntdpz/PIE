package com.ntdpz.pie.core.data.diary

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE date = :date")
    fun observeEntry(date: String): Flow<DiaryEntity?>

    @Query(
        "SELECT date, substr(text, 1, 160) AS preview, updatedAtEpochMillis, " +
            "EXISTS(SELECT 1 FROM diary_media WHERE diary_media.date = diary_entries.date) AS hasMedia " +
            "FROM diary_entries WHERE length(trim(text)) > 0 OR " +
            "EXISTS(SELECT 1 FROM diary_media WHERE diary_media.date = diary_entries.date) ORDER BY date DESC",
    )
    fun observeSummaries(): Flow<List<DiarySummaryRow>>

    @Query("SELECT * FROM diary_entries WHERE date = :date")
    suspend fun readEntry(date: String): DiaryEntity?

    @Upsert
    suspend fun upsert(entry: DiaryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureEntry(entry: DiaryEntity)

    @Query("DELETE FROM diary_entries WHERE date = :date")
    suspend fun delete(date: String)

    @Query("SELECT * FROM diary_entries ORDER BY date")
    suspend fun readAll(): List<DiaryEntity>

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun count(): Int
}
