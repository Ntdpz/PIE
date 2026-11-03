package com.ntdpz.pie

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class DiaryFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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
}
