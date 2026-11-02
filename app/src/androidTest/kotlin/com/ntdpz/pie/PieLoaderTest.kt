package com.ntdpz.pie

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.ntdpz.pie.core.designsystem.component.PieLoader
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertFalse

class PieLoaderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun pendingWorkIsAccessibleAndCompletionRemovesLoaderImmediately() {
        val pending = mutableStateOf(true)
        showLoader(pending)
        compose.onNodeWithText("กำลังเปิดบันทึก…").assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        )
        compose.runOnIdle { pending.value = false }
        compose.onNodeWithText("กำลังเปิดบันทึก…").assertDoesNotExist()
        compose.onNodeWithText("พร้อมเขียน").assertIsDisplayed()
    }

    @Test
    fun loaderDrawsMovingFrames() {
        compose.mainClock.autoAdvance = false
        showLoader(mutableStateOf(true))
        val frames = (0 until 12).map { index ->
            compose.mainClock.advanceTimeBy(100)
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            saveFrame(index, bitmap)
            bitmap
        }
        assertFalse("The loader should change between animation phases", frames[0].sameAs(frames[3]))
    }

    private fun showLoader(pending: androidx.compose.runtime.State<Boolean>) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                PieTheme {
                    Surface {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (pending.value) PieLoader("กำลังเปิดบันทึก…") else Text("พร้อมเขียน")
                        }
                    }
                }
            }
        }
    }

    private fun saveFrame(index: Int, bitmap: Bitmap) {
        val resolver = compose.activity.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-loader-%02d.png".format(index))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PIE-loader-tests")
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
