package com.ntdpz.pie

import android.content.ContentValues
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.data.settings.LocalSettingsRepository
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.core.domain.settings.ThemeMode
import com.ntdpz.pie.feature.settings.pin.PinPurpose
import com.ntdpz.pie.feature.settings.pin.PinScreen
import com.ntdpz.pie.feature.settings.pin.PinStage
import com.ntdpz.pie.feature.settings.pin.PinUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppearanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After fun restoreSystemTheme() = runBlocking { LocalSettingsRepository(context).saveTheme(ThemeMode.System) }

    @Test
    fun explicitThemeSurvivesRecreationAndCanReturnToLight() {
        compose.waitUntil(20_000) { compose.onAllNodesWithText("วันนี้เป็นยังไงบ้าง?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("มืด").performClick()
        waitTheme(ThemeMode.Dark)
        compose.waitForIdle()
        assertEquals(0xFF171518.toInt(), compose.onRoot().captureToImage().asAndroidBitmap().getPixel(2, 200))
        saveScreen("settings-dark")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("รูปแบบแอป").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(ThemeMode.Dark, runBlocking { LocalSettingsRepository(context).settings.first().themeMode })
        compose.onNode(hasText("หน้าแรก") and hasClickAction()).performClick()
        saveScreen("home-dark")
        compose.onNode(hasText("ย้อนวัน") and hasClickAction()).performClick()
        saveScreen("calendar-dark")
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("สว่าง").performClick()
        waitTheme(ThemeMode.Light)
        compose.waitForIdle()
        assertEquals(0xFFF7F8FA.toInt(), compose.onRoot().captureToImage().asAndroidBitmap().getPixel(2, 200))
        saveScreen("settings-light")
        compose.onNode(hasText("หน้าแรก") and hasClickAction()).performClick()
        saveScreen("home-light")
    }

    @Test
    fun pinKeypadSitsInLowerHalfInBothThemes() {
        for (dark in listOf(false, true)) {
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    PieTheme(darkTheme = dark) {
                        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                            PinScreen(PinUiState(PinPurpose.Unlock, PinStage.Verify, digits = 2), {}, {}, null)
                        }
                    }
                }
            }
            compose.waitForIdle()
            val root = compose.onRoot().getUnclippedBoundsInRoot()
            val first = compose.onNode(hasText("1") and hasClickAction()).getUnclippedBoundsInRoot()
            assertTrue("PIN keypad should start in the lower half", first.top.value > (root.bottom.value - root.top.value) / 2f)
            compose.onNodeWithText("0").assertIsDisplayed()
            compose.onNodeWithText("ลืม PIN?").assertIsDisplayed()
            saveScreen(if (dark) "pin-dark" else "pin-light")
        }
    }

    private fun waitTheme(mode: ThemeMode) {
        compose.waitUntil(10_000) { runBlocking { LocalSettingsRepository(context).settings.first().themeMode == mode } }
    }

    private fun saveScreen(name: String) {
        compose.waitForIdle()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PIE-appearance-tests")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(uri)!!.use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
