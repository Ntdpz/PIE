package com.ntdpz.pie

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.reminder.ReminderReceiver
import com.ntdpz.pie.core.designsystem.R
import com.ntdpz.pie.core.domain.pet.PetReaction
import com.ntdpz.pie.core.domain.settings.ReminderMessages
import com.ntdpz.pie.core.domain.settings.ReminderPresentations
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationMascotTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val container get() = (context.applicationContext as PieApplication).container
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private var previous: ReminderSettings? = null

    @Before
    fun prepare() {
        check(context.packageName.endsWith(".devicetest"))
        previous = runBlocking { container.settingsRepository.settings.first().reminder }
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        manager.cancel(42)
    }

    @After
    fun restore() {
        manager.cancel(42)
        context.stopService(Intent(context, PiePetOverlayService::class.java))
        previous?.let { settings ->
            runBlocking { container.settingsRepository.saveReminder(settings) }
            container.reminders.schedule(settings)
        }
    }

    @Test
    fun reminderShowsRealArtworkAndStableCopyWithCalmCustomFallback() {
        val date = LocalDate.of(2026, 11, 6)
        val settings = ReminderSettings(enabled = true)
        assertTrue(container.reminders.notifyNow(settings, date))
        val first = reminder()
        assertEquals(R.drawable.ic_notification_pet_head, first.smallIcon.resId)
        assertNotNull(first.getLargeIcon())
        assertEquals(ReminderMessages.forDay(date, settings), first.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(ReminderPresentations.forDay(date, settings).reaction.name, first.extras.getString("pie.mascot.pose"))
        val firstBitmap = (first.getLargeIcon().loadDrawable(context) as BitmapDrawable).bitmap
        assertTrue(container.reminders.notifyNow(settings, date))
        val repeated = reminder()
        assertTrue(firstBitmap.sameAs((repeated.getLargeIcon().loadDrawable(context) as BitmapDrawable).bitmap))
        assertEquals(first.extras.getString("pie.mascot.pose"), repeated.extras.getString("pie.mascot.pose"))
        assertTrue(container.reminders.notifyNow(settings.copy(customMode = true, message = "ดีใจมาก!"), date))
        compose.waitUntil(5_000) {
            manager.activeNotifications.firstOrNull { it.id == 42 }?.notification?.extras?.getString("pie.mascot.pose") == "Nod"
        }
        assertEquals(Notification.VISIBILITY_PRIVATE, reminder().visibility)
    }

    @Test
    fun historicalAlarmComponentStillDeliversMascotNotification() {
        assertEquals("com.ntdpz.pie.core.data.reminder.ReminderReceiver", ComponentName(context, ReminderReceiver::class.java).className)
        val time = ZonedDateTime.now().plusMinutes(10)
        val settings = ReminderSettings(enabled = true, times = setOf(time.hour * 60 + time.minute))
        runBlocking { container.settingsRepository.saveReminder(settings) }
        container.reminders.schedule(settings)
        val alarm = PendingIntent.getBroadcast(
            context, 42, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        assertNotNull(alarm)
        alarm.send()
        compose.waitUntil(10_000) { manager.activeNotifications.any { it.id == 42 } }
        assertEquals(R.drawable.ic_notification_pet_head, reminder().smallIcon.resId)
        assertNotNull(reminder().getLargeIcon())
    }

    @Test
    fun serviceUsesCroppedHeadAndStopActionAndAllPoseAssetsDecode() {
        PetReaction.entries.forEach { reaction ->
            val bitmap = container.petNotificationImages.forReaction(reaction)
            assertEquals(256, bitmap.width)
            assertEquals(256, bitmap.height)
        }
        compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
        compose.waitUntil(10_000) { manager.activeNotifications.any { it.id == 731 } }
        val notification = manager.activeNotifications.first { it.id == 731 }.notification
        assertEquals(R.drawable.ic_notification_pet_head, notification.smallIcon.resId)
        val bitmap = (notification.getLargeIcon().loadDrawable(context) as BitmapDrawable).bitmap
        val expected = container.petNotificationImages.head()
        val resized = Bitmap.createScaledBitmap(expected, bitmap.width, bitmap.height, true)
        assertTrue("Head differs after system resize to ${bitmap.width}x${bitmap.height}", resized.sameAs(bitmap))
        assertEquals("หยุด PIE", notification.actions.single().title.toString())
        assertTrue(container.reminders.notifyNow(ReminderSettings(enabled = true)))
        notification.actions.single().actionIntent.send()
        compose.waitUntil(10_000) { manager.activeNotifications.none { it.id == 731 } }
    }

    private fun reminder(): Notification {
        compose.waitUntil(5_000) { manager.activeNotifications.any { it.id == 42 } }
        return manager.activeNotifications.first { it.id == 42 }.notification
    }

}
