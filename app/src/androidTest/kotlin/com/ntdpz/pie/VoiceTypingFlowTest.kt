package com.ntdpz.pie

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.feature.editor.EditorScreen
import com.ntdpz.pie.feature.editor.EditorUiState
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Run in the isolated device-test application, with RECORD_AUDIO granted beforehand. */
class VoiceTypingFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun cancellingVoiceDoesNotSavePartialText() = exercise(background = false)

    @Test fun backgroundingVoiceDoesNotSavePartialText() = exercise(background = true)

    private fun exercise(background: Boolean) {
        val writes = mutableListOf<String>()
        val original = "ข้อความเดิมของการทดสอบ"
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                PieTheme {
                    Surface {
                    EditorScreen(
                        state = EditorUiState(LocalDate.of(1991, 1, 1), text = original, loading = false),
                        onTextChanged = { writes += it },
                        onRetryLoad = {}, onRetrySave = {}, onBack = {}, onAddMedia = {},
                        onDeleteEntry = {}, onDeleteMedia = {},
                    )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("พิมพ์ด้วยเสียง").performClick()
        val cancel = hasContentDescription("ยกเลิกพิมพ์เสียง")
        val explanation = hasText("เครื่องนี้ยังไม่มีบริการ", substring = true) or
            hasText("โมเดลเสียงภาษาไทย", substring = true) or
            hasText("ถอดเสียงไม่สำเร็จ", substring = true) or
            hasText("เปิดการพิมพ์เสียงในเครื่องไม่สำเร็จ", substring = true) or
            hasText("ยังไม่ได้ยินคำพูด", substring = true)
        compose.waitUntil(10_000) {
            compose.onAllNodes(cancel or explanation).fetchSemanticsNodes().isNotEmpty()
        }
        if (!background) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "editor-voice-device.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        if (background) {
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        } else if (compose.onAllNodes(cancel).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNode(cancel).performClick()
        }
        compose.onNodeWithContentDescription("พิมพ์ด้วยเสียง").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextEquals(original)
        compose.runOnIdle { assertTrue("Interim recognition must not save a draft", writes.isEmpty()) }
    }
}
