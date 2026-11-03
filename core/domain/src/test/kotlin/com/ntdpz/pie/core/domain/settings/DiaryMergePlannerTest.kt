package com.ntdpz.pie.core.domain.settings

import com.ntdpz.pie.core.domain.backup.DiaryMergePlanner
import com.ntdpz.pie.core.domain.backup.MergeChoice
import com.ntdpz.pie.core.domain.diary.DiaryEntry
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DiaryMergePlannerTest {
    private val date = LocalDate.of(2026, 11, 3)
    private val now = Instant.parse("2026-11-03T00:00:00Z")
    private fun entry(text: String, day: LocalDate = date) = DiaryEntry(day, text, now)

    @Test fun distinctDatesAreAddedAndIdenticalTextIsSkipped() {
        val existing = entry("same")
        val added = entry("new", date.plusDays(1))
        val plan = DiaryMergePlanner.preview(listOf(existing), listOf(existing.copy(updatedAt = now.minusSeconds(50)), added))
        assertEquals(listOf(added), plan.additions)
        assertEquals(setOf(date), plan.unchangedDates)
        assertTrue(plan.conflicts.isEmpty())
    }

    @Test fun differentTextRequiresExplicitResolutionRegardlessOfClock() {
        val local = entry("ของเดิม")
        val incoming = entry("จากไฟล์").copy(updatedAt = now.plusSeconds(100))
        val plan = DiaryMergePlanner.preview(listOf(local), listOf(incoming))
        assertTrue(plan.additions.isEmpty())
        val conflict = plan.conflicts.single()
        assertEquals(local, DiaryMergePlanner.resolve(conflict, MergeChoice.KeepDevice, now))
        assertEquals(incoming.text, DiaryMergePlanner.resolve(conflict, MergeChoice.UseBackup, now).text)
        val both = DiaryMergePlanner.resolve(conflict, MergeChoice.KeepBoth, now)
        assertTrue(both.text.startsWith(local.text))
        assertTrue(both.text.endsWith(incoming.text))
        assertEquals(date, both.date)
    }

    @Test fun blankIncomingNeverErasesDeviceText() {
        val plan = DiaryMergePlanner.preview(listOf(entry("keep")), listOf(entry("")))
        assertTrue(plan.additions.isEmpty())
        assertTrue(plan.conflicts.isEmpty())
        assertEquals(setOf(date), plan.unchangedDates)
    }

    @Test fun emptyDeviceTextCanAcceptBackupText() {
        assertEquals(listOf(entry("restore")), DiaryMergePlanner.preview(listOf(entry("")), listOf(entry("restore"))).additions)
    }

    @Test fun duplicateDatesAreRejectedInsteadOfSilentlyDiscarded() {
        assertThrows(IllegalArgumentException::class.java) {
            DiaryMergePlanner.preview(emptyList(), listOf(entry("first"), entry("second")))
        }
    }
}
