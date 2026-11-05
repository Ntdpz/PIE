package com.ntdpz.pie.core.data.diary

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DiaryMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PieDatabase::class.java)

    @Test
    fun upgradeFrom3To4PreservesTextMediaAndDefaultsToPlainText() {
        helper.createDatabase("formatting-migration", 3).apply {
            execSQL("INSERT INTO diary_entries VALUES ('2026-11-03', 'ข้อความ 🙂', 7)")
            execSQL("INSERT INTO diary_media VALUES ('media', '2026-11-03', 'keep.png', 'image/png', 9)")
            execSQL("INSERT INTO import_receipts VALUES ('2026-11-03', 'receipt')")
            close()
        }
        helper.runMigrationsAndValidate("formatting-migration", 4, true, DatabaseMigrations.From3To4).apply {
            query("SELECT text, formatting FROM diary_entries").use {
                check(it.moveToFirst())
                assertEquals("ข้อความ 🙂", it.getString(0))
                assertEquals("[]", it.getString(1))
            }
            query("SELECT COUNT(*) FROM diary_media").use { check(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
            query("SELECT sourceHash FROM import_receipts").use { check(it.moveToFirst()); assertEquals("receipt", it.getString(0)) }
            close()
        }
    }

    @Test
    fun upgradeFrom1To4PreservesText() {
        helper.createDatabase("formatting-full-migration", 1).apply {
            execSQL("INSERT INTO diary_entries VALUES ('2026-11-03', 'old note', 1)")
            close()
        }
        helper.runMigrationsAndValidate(
            "formatting-full-migration", 4, true,
            DatabaseMigrations.From1To2, DatabaseMigrations.From2To3, DatabaseMigrations.From3To4,
        ).close()
    }

    @Test
    fun upgradeToMediaSchemaPreservesExistingDiary() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("INSERT INTO diary_entries VALUES ('2026-11-02', 'keep this note', 1)")
            close()
        }
        helper.runMigrationsAndValidate("migration-test", 2, true, DatabaseMigrations.From1To2).apply {
            query("SELECT text FROM diary_entries WHERE date = '2026-11-02'").use {
                check(it.moveToFirst())
                assertEquals("keep this note", it.getString(0))
            }
            close()
        }
    }

    @Test
    fun upgradeFrom2To3PreservesDiaryAndMediaAndCascadesReceipts() {
        helper.createDatabase("merge-migration", 2).apply {
            execSQL("INSERT INTO diary_entries VALUES ('2026-11-03', 'keep diary', 7)")
            execSQL("INSERT INTO diary_media VALUES ('media-id', '2026-11-03', 'keep.png', 'image/png', 9)")
            close()
        }
        helper.runMigrationsAndValidate("merge-migration", 3, true, DatabaseMigrations.From2To3).apply {
            execSQL("PRAGMA foreign_keys=ON")
            query("SELECT text FROM diary_entries").use { check(it.moveToFirst()); assertEquals("keep diary", it.getString(0)) }
            query("SELECT fileName FROM diary_media").use { check(it.moveToFirst()); assertEquals("keep.png", it.getString(0)) }
            execSQL("INSERT INTO import_receipts VALUES ('2026-11-03', 'source-hash')")
            execSQL("DELETE FROM diary_entries WHERE date = '2026-11-03'")
            query("SELECT COUNT(*) FROM import_receipts").use { check(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
            close()
        }
    }

    @Test
    fun upgradeFrom1To3PreservesText() {
        helper.createDatabase("full-migration", 1).apply {
            execSQL("INSERT INTO diary_entries VALUES ('2026-11-03', 'old note', 1)")
            close()
        }
        helper.runMigrationsAndValidate("full-migration", 3, true, DatabaseMigrations.From1To2, DatabaseMigrations.From2To3).close()
    }
}
