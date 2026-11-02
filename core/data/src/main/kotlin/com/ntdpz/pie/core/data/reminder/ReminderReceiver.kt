package com.ntdpz.pie.core.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ntdpz.pie.core.data.settings.LocalSettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = LocalSettingsRepository(context).settings.first().reminder
                val reminders = AndroidReminders(context)
                if (intent.action == null && settings.enabled) reminders.notifyNow(settings)
                reminders.schedule(settings)
            } finally { pending.finish() }
        }
    }
}
