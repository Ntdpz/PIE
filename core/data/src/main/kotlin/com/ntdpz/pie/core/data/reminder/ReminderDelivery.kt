package com.ntdpz.pie.core.data.reminder

import com.ntdpz.pie.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/** Keep delivery/rescheduling rules beside the existing alarm boundary. */
class ReminderDelivery(
    private val settingsRepository: SettingsRepository,
    private val reminders: AndroidReminders,
) {
    suspend fun receive(action: String?) {
        val settings = settingsRepository.settings.first().reminder
        if (action == null && settings.enabled) reminders.notifyNow(settings)
        reminders.schedule(settings)
    }
}
