package com.ntdpz.pie.core.data.diary

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object DatabaseMigrations {
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
