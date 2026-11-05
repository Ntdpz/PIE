package com.ntdpz.pie.core.data.backup

import com.ntdpz.pie.core.data.diary.*
import com.ntdpz.pie.core.domain.diary.*
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupFormattingTest {
    @Test fun oldArchiveDefaultsToPlainText() {
        val json = """{"version":1,"entries":[{"date":"2026-11-05","text":"old note","updatedAt":1}],"media":[]}"""
        val entry = BackupManifestIO.read(ByteArrayInputStream(json.toByteArray())).entries.single().toDomain()
        assertEquals("old note", entry.text)
        assertTrue(entry.formatting.isEmpty())
    }

    @Test fun formatChangesHaveDistinctReceiptsWhilePlaintextKeepsLegacyHash() {
        val text = "hello"
        val styles = DiaryFormattingJson.encode(text, listOf(TextFormatRange(0, 5, TextFormat.Bold)))
        assertNotEquals(textDigest("2026-11-05", text), textDigest("2026-11-05", text, styles))
        assertEquals(textDigest("2026-11-05", text), textDigest("2026-11-05", text, "[]"))
    }

    @Test fun manifestRoundTripKeepsStylesAndInvalidOffsetsAreRejected() {
        val original = DiaryEntity("2026-11-05", "hello", 1, """[{"start":0,"end":5,"format":"Bold"}]""")
        val output = ByteArrayOutputStream()
        BackupManifestIO.write(output, BackupManifest(listOf(original), emptyList(), ReminderSettings()))
        val read = BackupManifestIO.read(ByteArrayInputStream(output.toByteArray())).entries.single()
        assertEquals(original, read)
        val invalidOutput = ByteArrayOutputStream()
        BackupManifestIO.write(
            invalidOutput,
            BackupManifest(listOf(original.copy(formatting = """[{"start":0,"end":50,"format":"Bold"}]""")), emptyList(), ReminderSettings()),
        )
        assertTrue(runCatching { BackupManifestIO.read(ByteArrayInputStream(invalidOutput.toByteArray())) }.isFailure)
    }
}
