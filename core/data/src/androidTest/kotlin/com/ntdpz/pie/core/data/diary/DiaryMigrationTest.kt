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
}
