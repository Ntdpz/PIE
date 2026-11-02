package com.ntdpz.pie

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertTextEquals
import org.junit.Rule
import org.junit.Test

class DiaryFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun savedTextSurvivesNavigationAndActivityRecreation() {
        val text = "PIE integration test 🙂 #test"
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("เริ่มเขียนวันนี้").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("เขียนต่อวันนี้").fetchSemanticsNodes().isNotEmpty()
        }
        val label = if (
            compose.onAllNodesWithText("เขียนต่อวันนี้").fetchSemanticsNodes().isNotEmpty()
        ) "เขียนต่อวันนี้" else "เริ่มเขียนวันนี้"
        compose.onNodeWithText(label).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextReplacement(text)
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("บันทึกแล้ว").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("กลับ").performClick()
        compose.onNodeWithText("ย้อนวัน").performClick()
        compose.onNodeWithText("เปิดบันทึก").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals(text)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertTextEquals(text)
    }
}
