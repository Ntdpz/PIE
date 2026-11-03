package com.ntdpz.pie.core.data.media

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MediaDao {
    @Query("SELECT * FROM diary_media WHERE date = :date ORDER BY createdAtEpochMillis, id")
    fun observe(date: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM diary_media ORDER BY date DESC, createdAtEpochMillis, id")
    fun observeAll(): Flow<List<MediaEntity>>

    @Insert
    suspend fun insert(media: MediaEntity)

    @Query("SELECT * FROM diary_media WHERE id = :id")
    suspend fun read(id: String): MediaEntity?

    @Query("DELETE FROM diary_media WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM diary_media ORDER BY date, createdAtEpochMillis")
    suspend fun readAll(): List<MediaEntity>

    @Query("SELECT * FROM diary_media WHERE date = :date")
    suspend fun readForDate(date: String): List<MediaEntity>
}
