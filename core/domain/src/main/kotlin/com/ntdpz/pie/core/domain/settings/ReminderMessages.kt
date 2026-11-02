package com.ntdpz.pie.core.domain.settings

import java.time.LocalDate

object ReminderMessages {
    val defaultCount: Int get() = ReminderPromptCatalog.prompts.size

    fun defaultForDay(date: LocalDate): String {
        val prompts = ReminderPromptCatalog.prompts
        return prompts[Math.floorMod(date.toEpochDay(), prompts.size.toLong()).toInt()]
    }

    fun forDay(date: LocalDate, reminder: ReminderSettings): String =
        if (reminder.customMode && reminder.message.isNotBlank()) reminder.message
        else defaultForDay(date)
}
