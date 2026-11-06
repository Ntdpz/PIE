package com.ntdpz.pie

import android.content.ContentValues
import android.app.NotificationManager
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.settings.LocalSettingsRepository
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import com.ntdpz.pie.core.domain.settings.ReminderMessages
import java.time.ZonedDateTime
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
class PenUiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val pin = "123456"

    @Test
    fun reminderPostsNotificationWhenPermissionIsGranted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(42)
        try {
            org.junit.Assert.assertTrue((context.applicationContext as PieApplication).container.reminders.notifyNow(ReminderSettings(enabled = true)))
            compose.waitUntil(5_000) { manager.activeNotifications.any { it.id == 42 } }
        } finally {
            manager.cancel(42)
        }
    }

    @Test
    fun scheduledReminderPostsNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        val reminders = (context.applicationContext as PieApplication).container.reminders
        val settingsRepository = LocalSettingsRepository(context)
        val previousSettings = runBlocking { settingsRepository.settings.first().reminder }
        val scheduled = ZonedDateTime.now().plusMinutes(1)
        val settings = ReminderSettings(
            enabled = true,
            times = setOf(scheduled.hour * 60 + scheduled.minute),
        )
        manager.cancel(42)
        try {
            runBlocking { settingsRepository.saveReminder(settings) }
            reminders.schedule(settings)
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("dumpsys deviceidle force-idle").close()
            compose.waitUntil(120_000) { manager.activeNotifications.any { it.id == 42 } }
        } finally {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("dumpsys deviceidle unforce").close()
            runBlocking { settingsRepository.saveReminder(previousSettings) }
            reminders.schedule(previousSettings)
            manager.cancel(42)
        }
    }

    @After
    fun removeOnlyTestPreferences() = runBlocking {
        val repo = LocalSettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.disablePin(pin.toCharArray())
        repo.saveReminder(ReminderSettings())
    }

    @Test
    fun settingsReminderAndBackupScreensAreUsable() {
        waitText("วันนี้เป็นยังไงบ้าง")
        val dailyMessage = ReminderMessages.defaultForDay(LocalDate.now())
        saveScreen("home")
        compose.onNode(hasText("ความทรงจำ") and hasClickAction()).performClick()
        compose.onNodeWithContentDescription("เลือกวันจากปฏิทิน").performClick()
        waitText("เลือกวัน")
        saveScreen("calendar")
        compose.onNodeWithContentDescription("ปิดปฏิทิน").performClick()
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        waitText("หน้าตาแอป")
        saveScreen("settings")
        compose.onNodeWithText("นำเข้าไฟล์").assertExists()
        compose.onNodeWithText("แจ้งเตือน").performScrollTo().performClick()
        waitText("คำชวนของวันนี้")
        compose.onNodeWithText(dailyMessage).assertIsDisplayed()
        saveScreen("reminders")
        compose.onNodeWithText("21:00").performClick()
        waitText("บันทึกเวลา")
        saveScreen("reminder-time")
        compose.onNodeWithText("บันทึกเวลา").performClick()
        compose.onNodeWithText("21:00").assertExists()
        compose.onNodeWithText("21:00").performClick()
        waitText("บันทึกเวลา")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("บันทึกเวลา").assertDoesNotExist()
        compose.onNodeWithText("เขียนเอง").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("ทดสอบการบันทึกข้อความแจ้งเตือน")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("บันทึกการตั้งค่าแล้ว").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("กลับ").performClick()
        compose.onNodeWithText("ส่งออกไฟล์").performScrollTo().performClick()
        waitText("สร้างไฟล์สำรอง .pie")
        compose.onNodeWithText("เลือกที่บันทึกไฟล์").assertIsNotEnabled()
        saveScreen("backup")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("a-test-backup-password")
        fields[1].performTextReplacement("a-test-backup-password")
        compose.onNodeWithText("เลือกที่บันทึกไฟล์").assertIsEnabled()
    }

    @Test
    fun pinLocksAfterBackgroundAndRejectsWrongCode() {
        waitText("วันนี้เป็นยังไงบ้าง")
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("รหัส PIN").performScrollTo().performClick()
        waitText("ตั้ง PIN ของคุณ")
        typePin(pin)
        waitText("ยืนยัน PIN อีกครั้ง")
        typePin(pin)
        waitText("เปิดอยู่")
        compose.onNodeWithText("รหัส PIN").performScrollTo().performClick()
        waitText("เปลี่ยน PIN")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitText("พื้นที่ของคุณ")
        compose.onNodeWithTag("pet-companion").assertDoesNotExist()
        typePin("999999")
        waitText("PIN ไม่ถูกต้อง ลองอีกครั้ง")
        typePin(pin)
        waitText("หน้าตาแอป")
        compose.onNodeWithText("เปลี่ยน PIN").assertDoesNotExist()
    }

    private fun typePin(value: String) {
        value.forEach { compose.onNodeWithText(it.toString()).performClick() }
    }

    private fun waitText(text: String) {
        compose.waitUntil(20_000) { (compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() || (text == "วันนี้เป็นยังไงบ้าง" && compose.onAllNodesWithText("กลับมาเล่าต่อได้นะ").fetchSemanticsNodes().isNotEmpty())) }
    }

    private fun saveScreen(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-pen-$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PIE-UI-tests")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(uri)!!.use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
