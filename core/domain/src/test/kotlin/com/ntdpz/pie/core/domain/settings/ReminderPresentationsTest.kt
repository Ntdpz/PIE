package com.ntdpz.pie.core.domain.settings

import com.ntdpz.pie.core.domain.pet.PetReaction
import java.security.MessageDigest
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPresentationsTest {
    @Test
    fun existing240MessagesAndTheirOrderAreUnchanged() {
        val bytes = ReminderPromptCatalog.prompts.joinToString("\n").toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("db77cb984d71efd64e02c0585ec7e86ea444e4dd3b954de33f3cf55a1a78333f", digest)
    }

    @Test
    fun dailyPoseIsStableCompatibleAndAllNineActionsAppear() {
        val start = LocalDate.of(1969, 12, 1)
        val seen = mutableSetOf<PetReaction>()
        repeat(960) { day ->
            val date = start.plusDays(day.toLong())
            val result = ReminderPresentations.forDay(date, ReminderSettings())
            val prompt = ReminderPromptCatalog.entries[Math.floorMod(date.toEpochDay(), 240).toInt()]
            assertEquals(ReminderMessages.defaultForDay(date), result.message)
            assertTrue(result.reaction in ReminderPresentations.compatibleReactions(prompt.tone))
            assertEquals(result, ReminderPresentations.forDay(date, ReminderSettings(times = setOf(480, 1260))))
            assertNotEquals(result.reaction, ReminderPresentations.forDay(date.plusDays(1), ReminderSettings()).reaction)
            seen += result.reaction
        }
        assertEquals(PetReaction.entries.toSet(), seen)
    }

    @Test
    fun customTextHasCalmDefaultWithoutGuessingItsTone() {
        val date = LocalDate.of(2026, 12, 31)
        listOf("ดีใจมาก!", "วันนี้ขอพักก่อน", "อยากเขียนเอง").forEach { message ->
            assertEquals(
                ReminderPresentation(message, PetReaction.Nod),
                ReminderPresentations.forDay(date, ReminderSettings(customMode = true, message = message)),
            )
        }
        assertEquals(
            ReminderPresentations.forDay(date, ReminderSettings()),
            ReminderPresentations.forDay(date, ReminderSettings(customMode = true, message = "  ")),
        )
    }
}
