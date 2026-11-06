package com.ntdpz.pie.core.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ntdpz.pie.PieApplication
import kotlinx.coroutines.launch

/** App dependency wiring. Keep the historical component name so existing alarms still resolve. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as PieApplication).container
        container.applicationScope.launch {
            try {
                container.reminderDelivery.receive(intent.action)
            } finally { pending.finish() }
        }
    }
}
