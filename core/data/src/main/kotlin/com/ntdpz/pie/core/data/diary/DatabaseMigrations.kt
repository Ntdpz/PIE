package com.ntdpz.pie.core.data.diary

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object DatabaseMigrations {
    val From2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS import_history (id INTEGER NOT NULL, completedAtMillis INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE IF NOT EXISTS import_receipts (date TEXT NOT NULL, sourceHash TEXT NOT NULL, PRIMARY KEY(date, sourceHash), FOREIGN KEY(date) REFERENCES diary_entries(date) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_receipts_date ON import_receipts(date)")
        }
    }
    val From1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS diary_media (" +
                    "id TEXT NOT NULL, date TEXT NOT NULL, fileName TEXT NOT NULL, " +
                    "mimeType TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), " +
                    "FOREIGN KEY(date) REFERENCES diary_entries(date) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_media_date ON diary_media(date)")
        }
    }
}
