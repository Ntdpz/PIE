package com.ntdpz.pie.core.designsystem.format

import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.util.Locale

object PieDates {
    private fun formatter(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag("th"))
        .withChronology(ThaiBuddhistChronology.INSTANCE)

    fun home(date: LocalDate): String = date.format(formatter("EEEE d MMM yyyy")).removePrefix("วัน")
    fun entry(date: LocalDate): String = date.format(formatter("d MMMM yyyy"))
    fun shortEntry(date: LocalDate): String = date.format(formatter("EEEE d MMMM")).removePrefix("วัน")
    fun month(month: YearMonth): String = month.atDay(1).format(formatter("MMMM yyyy"))
}
