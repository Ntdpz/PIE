package com.ntdpz.pie.core.data.reminder

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.ntdpz.pie.core.domain.settings.ReminderMessages
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import java.time.LocalDate
import java.time.ZonedDateTime

class AndroidReminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private fun alarmIntent() = PendingIntent.getBroadcast(
        context, 42, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(settings: ReminderSettings) {
        alarms.cancel(alarmIntent())
        if (!settings.enabled || settings.days.isEmpty() || settings.times.isEmpty()) return
        val now = ZonedDateTime.now()
        val next = (0..7).flatMap { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek.value !in settings.days) emptyList()
            else settings.times.map { date.atTime(it / 60, it % 60).atZone(now.zone) }
        }.filter { it.isAfter(now) }.minOrNull() ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), alarmIntent())
    }

    fun notifyNow(settings: ReminderSettings): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(NotificationChannel("diary", "ชวนเขียนไดอารี่", NotificationManager.IMPORTANCE_DEFAULT))
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return false
        val notification = Notification.Builder(context, "diary")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("PIE")
            .setContentText(ReminderMessages.forDay(LocalDate.now(), settings))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        manager.notify(42, notification)
        return true
    }
}
