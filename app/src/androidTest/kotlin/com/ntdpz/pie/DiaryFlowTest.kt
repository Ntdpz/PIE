package com.ntdpz.pie

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import com.ntdpz.pie.core.domain.diary.TextFormat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test

class DiaryFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun formattingSurvivesReopenAndLaterTyping() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("เริ่มเขียน").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("เขียนต่อ").fetchSemanticsNodes().isNotEmpty()
        }
        val label = if (compose.onAllNodesWithText("เขียนต่อ").fetchSemanticsNodes().isNotEmpty()) "เขียนต่อ" else "เริ่มเขียน"
        compose.onNodeWithText(label).performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        val editor = compose.onNode(hasSetTextAction())
        val fixture = "หัวข้อวันนี้\nข้อความเดิม"
        editor.performTextReplacement(fixture)
        editor.performSemanticsAction(SemanticsActions.SetSelection) { it(0, fixture.length, false) }
        compose.onNodeWithContentDescription("จัดรูปแบบข้อความ").performClick()
        compose.onNodeWithContentDescription("ข้อความปกติ").performClick()
        editor.performSemanticsAction(SemanticsActions.SetSelection) { it(0, "หัวข้อวันนี้".length, false) }
        compose.onNodeWithContentDescription("หัวข้อ").performClick()
        compose.onNodeWithContentDescription("หัวข้อ").assertIsSelected()
        compose.onNodeWithContentDescription("ปิดแผงจัดรูปแบบ").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("บันทึกแล้ว").fetchSemanticsNodes().isNotEmpty() }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as PieApplication).container
        val date = java.time.LocalDate.now(container.clock)
        runBlocking { container.diaryWriter.awaitIdle() }
        val stored = runBlocking { container.diaryRepository.readEntry(date)!! }
        org.junit.Assert.assertEquals(
            listOf(com.ntdpz.pie.core.domain.diary.TextFormatRange(0, "หัวข้อวันนี้".length, TextFormat.Heading)),
            stored.formatting.filter { it.format == TextFormat.Heading },
        )
        compose.onNodeWithContentDescription("กลับ").performClick()
        compose.onNodeWithText("เขียนต่อ").performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).assertTextEquals(stored.text)
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetSelection) { it(0, 0, false) }
        compose.onNodeWithContentDescription("จัดรูปแบบข้อความ").performClick()
        java.io.File(context.getExternalFilesDir(null), "editor-format-device.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithContentDescription("หัวข้อ").assertIsSelected()
        compose.onNodeWithContentDescription("ปิดแผงจัดรูปแบบ").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("ใหม่ ")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("บันทึกแล้ว").fetchSemanticsNodes().isNotEmpty() }
        org.junit.Assert.assertTrue(runBlocking { container.diaryRepository.readEntry(date)!! }.formatting.isNotEmpty())
    }

    @Test
    fun savedTextSurvivesNavigationAndActivityRecreation() {
        val text = "PIE integration test 🙂 #test"
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("เริ่มเขียน").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("เขียนต่อ").fetchSemanticsNodes().isNotEmpty()
        }
        val label = if (
            compose.onAllNodesWithText("เขียนต่อ").fetchSemanticsNodes().isNotEmpty()
        ) "เขียนต่อ" else "เริ่มเขียน"
        compose.onNodeWithText(label).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextReplacement(text)
        compose.onNodeWithContentDescription("เพิ่ม Emoji").performClick()
        compose.onNodeWithText("🫶").assertIsDisplayed().performClick()
        val savedText = "$text🫶"
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("บันทึกแล้ว").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("กลับ").performClick()
        compose.onNodeWithText("เขียนต่อ").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals(savedText)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertTextEquals(savedText)
    }

    @Test
    fun thaiTextInsertedAtCursorSurvivesRecreation() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("เริ่มเขียน").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("เขียนต่อ").fetchSemanticsNodes().isNotEmpty()
        }
        val label = if (compose.onAllNodesWithText("เขียนต่อ").fetchSemanticsNodes().isNotEmpty()) {
            "เขียนต่อ"
        } else {
            "เริ่มเขียน"
        }
        compose.onNodeWithText(label).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        val editor = compose.onNode(hasSetTextAction())
        editor.performTextReplacement("AความสุขB")
        editor.performSemanticsAction(SemanticsActions.SetSelection) { setSelection ->
            setSelection(1, 1, false)
        }
        editor.performTextInput("วันนี้")
        editor.assertTextEquals("Aวันนี้ความสุขB")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("บันทึกแล้ว").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertTextEquals("Aวันนี้ความสุขB")
    }
}
