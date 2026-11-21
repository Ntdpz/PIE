package com.ntdpz.pie.core.domain.diary

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceLocalClockTest {
    @Test
    fun existingClockUsesNewDeviceZoneForNewDates() {
        val originalZone = TimeZone.getDefault()
        val bangkok = ZoneId.of("Asia/Bangkok")
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val instant = Instant.parse("2026-11-07T18:00:00Z")
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(bangkok))
            val clock = DeviceLocalClock
            val selectedDate = instant.atZone(clock.zone).toLocalDate()
            val fixedZoneClock = clock.withZone(bangkok)

            TimeZone.setDefault(TimeZone.getTimeZone(losAngeles))

            assertEquals(LocalDate.of(2026, 11, 8), selectedDate)
            assertEquals(LocalDate.of(2026, 11, 7), instant.atZone(clock.zone).toLocalDate())
            assertEquals(bangkok, fixedZoneClock.zone)
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }
}
