package com.ntdpz.pie.core.data.backup

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ntdpz.pie.core.data.diary.DiaryEntity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "import_history")
internal data class ImportHistory(@PrimaryKey val id: Int = 0, val completedAtMillis: Long)

@Entity(
    tableName = "import_receipts", primaryKeys = ["date", "sourceHash"],
    foreignKeys = [ForeignKey(entity = DiaryEntity::class, parentColumns = ["date"], childColumns = ["date"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("date")],
)
internal data class ImportReceipt(val date: String, val sourceHash: String)

@Dao
internal interface ImportReceiptDao {
    @Query("SELECT completedAtMillis FROM import_history WHERE id = 0") fun observeLastImport(): Flow<Long?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun recordHistory(history: ImportHistory)
    @Query("SELECT * FROM import_receipts") suspend fun readAll(): List<ImportReceipt>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(receipt: ImportReceipt)
    @Query("DELETE FROM import_receipts WHERE date = :date") suspend fun deleteForDate(date: String)
}
