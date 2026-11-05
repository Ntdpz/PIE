package com.ntdpz.pie.core.domain.diary

import org.junit.Assert.*
import org.junit.Test

class TextFormattingTest {
    @Test fun insertThaiCompositionAndEmojiKeepsOffsets() {
        val old = "Aความสุข🙂B"
        val formats = listOf(TextFormatRange(1, old.length - 1, TextFormat.Bold))
        val new = "Aวันนี้ความสุข🙂B"
        assertEquals(listOf(TextFormatRange(1, new.length - 1, TextFormat.Bold)), TextFormatting.afterEdit(old, new, formats))
        assertEquals(emptyList<TextFormatRange>(), TextFormatting.afterEdit(old, "AB", formats))
    }

    @Test fun removingPartialBoldKeepsBothSides() {
        val ranges = listOf(TextFormatRange(0, 10, TextFormat.Bold))
        assertEquals(
            listOf(TextFormatRange(0, 3, TextFormat.Bold), TextFormatRange(7, 10, TextFormat.Bold)),
            TextFormatting.remove(ranges, 3, 7, setOf(TextFormat.Bold)),
        )
    }

    @Test fun normalizingMergesOverlappingStylesAndClipsOnlyOffsets() {
        val ranges = listOf(TextFormatRange(0, 4, TextFormat.Bold), TextFormatRange(3, 20, TextFormat.Bold))
        assertEquals(listOf(TextFormatRange(0, 6, TextFormat.Bold)), TextFormatting.normalize("abcdef", ranges))
    }

    @Test fun keepBothShiftsBackupStylesAndKeepsOriginalStyles() {
        val date = java.time.LocalDate.of(2026, 11, 5)
        val now = java.time.Instant.EPOCH
        val device = DiaryEntry(date, "one", now, listOf(TextFormatRange(0, 3, TextFormat.Bold)))
        val backup = DiaryEntry(date, "two", now, listOf(TextFormatRange(0, 3, TextFormat.Heading)))
        val merged = com.ntdpz.pie.core.domain.backup.DiaryMergePlanner.resolve(
            com.ntdpz.pie.core.domain.backup.DiaryConflict(date, device, backup),
            com.ntdpz.pie.core.domain.backup.MergeChoice.KeepBoth, now,
        )
        assertEquals(device.formatting.first(), merged.formatting.first())
        val incoming = merged.formatting.last()
        assertEquals("two", merged.text.substring(incoming.start, incoming.end))
        assertTrue(com.ntdpz.pie.core.domain.backup.DiaryMergePlanner.preview(listOf(device), listOf(device.copy(formatting = emptyList()))).conflicts.isNotEmpty())
    }
}
