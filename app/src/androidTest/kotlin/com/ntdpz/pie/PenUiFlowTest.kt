package com.ntdpz.pie

import android.content.ContentValues
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
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
class PenUiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val pin = "123456"

    @After
    fun removeOnlyTestPreferences() = runBlocking {
        val repo = LocalSettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.disablePin(pin.toCharArray())
        repo.saveReminder(ReminderSettings())
    }

    @Test
    fun settingsReminderAndBackupScreensAreUsable() {
        waitText("วันนี้เป็นยังไงบ้าง?")
        val dailyMessage = ReminderMessages.defaultForDay(LocalDate.now())
        compose.onNodeWithText(dailyMessage).assertIsDisplayed()
        saveScreen("home")
        compose.onNode(hasText("ย้อนวัน") and hasClickAction()).performClick()
        waitText("วันที่มีบันทึก")
        saveScreen("calendar")
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        waitText("ความเป็นส่วนตัว และบันทึกของคุณ")
        saveScreen("settings")
        compose.onNodeWithText("นำเข้าไฟล์สำรอง").assertExists()
        compose.onNodeWithText("แจ้งเตือนให้เขียน").performClick()
        waitText("ข้อความที่จะได้รับ")
        compose.onNodeWithText(dailyMessage).assertIsDisplayed()
        compose.onNodeWithText("หมุนเวียน 240 ประโยคตามวัน\nหากตั้งหลายเวลา จะใช้ข้อความเดียวกันในวันนั้น").assertIsDisplayed()
        saveScreen("reminders")
        compose.onNodeWithText("เขียนข้อความเอง").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("ทดสอบการบันทึกข้อความแจ้งเตือน")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("บันทึกการตั้งค่าแล้ว").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("กลับ").performClick()
        compose.onNodeWithText("ส่งออกไฟล์สำรอง").performClick()
        waitText("เก็บบันทึกไว้อีกที่")
        compose.onNodeWithText("เลือกที่เก็บและส่งออก").assertIsNotEnabled()
        saveScreen("backup")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("a-test-backup-password")
        fields[1].performTextReplacement("a-test-backup-password")
        compose.onNodeWithText("เลือกที่เก็บและส่งออก").assertIsEnabled()
    }

    @Test
    fun pinLocksAfterBackgroundAndRejectsWrongCode() {
        waitText("วันนี้เป็นยังไงบ้าง?")
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("ล็อกด้วย PIN").performClick()
        waitText("ตั้ง PIN ของคุณ")
        typePin(pin)
        waitText("ยืนยัน PIN อีกครั้ง")
        typePin(pin)
        waitText("เปิดอยู่ · เปลี่ยนหรือปิด PIN")
        compose.onNodeWithText("ล็อกด้วย PIN").performClick()
        waitText("เปลี่ยน PIN")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitText("กลับมาแล้ว :)")
        typePin("999999")
        waitText("PIN ไม่ถูกต้อง ลองอีกครั้ง")
        typePin(pin)
        waitText("ความเป็นส่วนตัว และบันทึกของคุณ")
        compose.onNodeWithText("เปลี่ยน PIN").assertDoesNotExist()
    }

    private fun typePin(value: String) {
        value.forEach { compose.onNodeWithText(it.toString()).performClick() }
    }

    private fun waitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
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
