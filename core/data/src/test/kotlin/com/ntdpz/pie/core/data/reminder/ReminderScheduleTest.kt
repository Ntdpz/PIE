package com.ntdpz.pie.core.data.reminder

import com.ntdpz.pie.core.domain.settings.ReminderSettings
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleTest {
    private val settings = ReminderSettings(
        enabled = true,
        days = (1..7).toSet(),
        times = setOf(9 * 60, 21 * 60),
    )

    @Test
    fun selectsNextTimeAndMovesPastElapsedAlarm() {
        val morning = ZonedDateTime.of(2026, 11, 4, 8, 30, 0, 0, ZoneId.of("Asia/Bangkok"))
        assertEquals(morning.withHour(9).withMinute(0), nextReminderAt(settings, morning))

        val evening = morning.withHour(21).withMinute(0)
        assertEquals(evening.plusDays(1).withHour(9), nextReminderAt(settings, evening))
    }

    @Test
    fun recomputesWallClockTimeInNewZone() {
        val bangkok = ZonedDateTime.of(2026, 11, 4, 8, 30, 0, 0, ZoneId.of("Asia/Bangkok"))
        val tokyo = bangkok.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
        assertEquals(9, nextReminderAt(settings, bangkok)?.hour)
        assertEquals(21, nextReminderAt(settings, tokyo)?.hour)
        assertEquals("Asia/Tokyo", nextReminderAt(settings, tokyo)?.zone?.id)
    }

    @Test
    fun skipsUnselectedDays() {
        val wednesday = ZonedDateTime.of(2026, 11, 4, 22, 0, 0, 0, ZoneId.of("Asia/Bangkok"))
        val mondayOnly = settings.copy(days = setOf(1))
        val next = nextReminderAt(mondayOnly, wednesday)
        assertEquals(2026, next?.year)
        assertEquals(11, next?.monthValue)
        assertEquals(9, next?.dayOfMonth)
        assertEquals(9, next?.hour)
    }
}
