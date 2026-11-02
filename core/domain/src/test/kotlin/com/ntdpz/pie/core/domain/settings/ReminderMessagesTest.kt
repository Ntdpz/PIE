package com.ntdpz.pie.core.domain.settings

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMessagesTest {
    @Test
    fun fullCycleContains240DistinctShortMessages() {
        val start = LocalDate.of(2026, 11, 2)
        val cycle = (0 until ReminderMessages.defaultCount).map { ReminderMessages.defaultForDay(start.plusDays(it.toLong())) }
        assertEquals(240, cycle.size)
        assertEquals(240, cycle.toSet().size)
        assertTrue(cycle.all { it.isNotBlank() && it.length <= 100 })
        assertEquals(cycle.first(), ReminderMessages.defaultForDay(start.plusDays(240)))
    }

    @Test
    fun everyReminderInADayUsesTheSameMessageAsHome() {
        val date = LocalDate.of(2026, 11, 2)
        val reminder = ReminderSettings(times = setOf(8 * 60, 12 * 60, 21 * 60))
        reminder.times.forEach { minute ->
            assertEquals(ReminderMessages.defaultForDay(date), ReminderMessages.forDay(date, reminder.copy(times = setOf(minute))))
        }
    }

    @Test
    fun customMessageIsPreservedOnlyWhenCustomModeIsSelected() {
        val date = LocalDate.of(2026, 11, 2)
        val custom = "วันนี้ขอพักก่อน 🙂"
        assertEquals(custom, ReminderMessages.forDay(date, ReminderSettings(customMode = true, message = custom)))
        assertEquals(ReminderMessages.defaultForDay(date), ReminderMessages.forDay(date, ReminderSettings(message = custom)))
    }

    @Test
    fun blankCustomMessageFallsBackToDailyMessage() {
        val date = LocalDate.of(2026, 11, 2)
        listOf("", "  ", "\n").forEach { blank ->
            assertEquals(ReminderMessages.defaultForDay(date), ReminderMessages.forDay(date, ReminderSettings(customMode = true, message = blank)))
        }
    }

    @Test
    fun datesBeforeEpochAndAcrossYearBoundaryAreSupported() {
        listOf(LocalDate.of(1969, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)).forEach { date ->
            val message = ReminderMessages.defaultForDay(date)
            assertTrue(message.isNotBlank())
            assertTrue(message != ReminderMessages.defaultForDay(date.plusDays(1)))
        }
    }
}
