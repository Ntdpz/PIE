package com.ntdpz.pie.core.domain.diary

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Resolves the current device time zone when a caller asks for the local date. */
object DeviceLocalClock : Clock() {
    private val utc = systemUTC()

    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = utc.withZone(zone)

    override fun instant(): Instant = utc.instant()

    override fun millis(): Long = utc.millis()
}
