package com.ntdpz.pie

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.domain.settings.ThemeMode
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.feature.settings.drive.DriveScreen
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Run only with -Ppie.deviceTest=true: never automate the user's diary package. */
class DriveBackupFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun driveIntroductionAndImportAreReachable() {
        requireIsolatedPackage()
        openDrive()
        compose.onNodeWithText("สำรองข้อมูล\nบน Google Drive").assertIsDisplayed()
        compose.onNodeWithText("สำรองไป Google Drive").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("นำไฟล์จาก Drive กลับมา").performScrollTo().performClick()
        waitText("เลือกไฟล์ .pie")
        compose.onNodeWithText("คุณจะได้ตรวจรายการก่อนยืนยันการรวม").assertExists()
        compose.onNodeWithContentDescription("กลับ").performClick()
        waitText("สำรองข้อมูล\nบน Google Drive")
        compose.onNodeWithText("ไว้ทีหลัง").performClick()
        waitText("พื้นที่ของคุณ")
    }

    @Test
    fun cancelledDocumentPickerDoesNotExportOrChangeDiary() {
        requireIsolatedPackage()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = (compose.activity.application as PieApplication).container
        val before = runBlocking { container.diaryRepository.observeSummaries().first() }
        val exportedAt = runBlocking { container.settingsRepository.settings.first().lastBackupAtMillis }
        var pickerIntent: Intent? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CREATE_DOCUMENT) return null
                pickerIntent = intent
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            openDrive()
            compose.onNodeWithText("สำรองไป Google Drive").performClick()
            waitText("เลือกที่เก็บบน Google Drive")
            val action = compose.onNodeWithText("เลือกที่เก็บบน Google Drive")
            action.assertIsNotEnabled()
            val fields = compose.onAllNodes(hasSetTextAction())
            fields[0].performTextReplacement("drive-fixture-password")
            fields[1].performTextReplacement("mismatched-password")
            action.assertIsNotEnabled()
            fields[1].performTextReplacement("drive-fixture-password")
            action.assertIsEnabled().performClick()
            compose.waitUntil(10_000) { pickerIntent != null }
            assertEquals("application/octet-stream", pickerIntent!!.type)
            assertFalse(pickerIntent!!.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY, false))
            compose.waitForIdle()
            action.assertIsNotEnabled()
            assertEquals(exportedAt, runBlocking { container.settingsRepository.settings.first().lastBackupAtMillis })
            assertEquals(before, runBlocking { container.diaryRepository.observeSummaries().first() })
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun lightAndDarkScreensUseRealThemeAndStayReachable() {
        requireIsolatedPackage()
        val container = (compose.activity.application as PieApplication).container
        val previous = runBlocking { container.settingsRepository.settings.first().themeMode }
        try {
            openDrive()
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                runBlocking { container.settingsRepository.saveTheme(mode) }
                compose.waitForIdle()
                compose.onNodeWithText("สำรองไป Google Drive").assertIsDisplayed()
                compose.onNodeWithText("นำไฟล์จาก Drive กลับมา").performScrollTo().assertIsDisplayed()
                val file = File(compose.activity.filesDir, "drive-${mode.name.lowercase()}.png")
                file.outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        } finally {
            runBlocking { container.settingsRepository.saveTheme(previous) }
        }
    }

    @Test
    fun realSystemDocumentPickerOpensAndCancelsWithoutExporting() {
        requireIsolatedPackage()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = (compose.activity.application as PieApplication).container
        val before = runBlocking { container.settingsRepository.settings.first().lastBackupAtMillis }
        openDrive()
        compose.onNodeWithText("สำรองไป Google Drive").performClick()
        waitText("เลือกที่เก็บบน Google Drive")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("drive-fixture-password")
        fields[1].performTextReplacement("drive-fixture-password")
        compose.onNodeWithText("เลือกที่เก็บบน Google Drive").performClick()
        try {
            compose.waitUntil(15_000) {
                instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") == true
            }
        } finally {
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        }
        waitText("เลือกที่เก็บบน Google Drive")
        compose.onNodeWithText("เลือกที่เก็บบน Google Drive").assertIsNotEnabled()
        assertEquals(before, runBlocking { container.settingsRepository.settings.first().lastBackupAtMillis })
    }

    @Test
    fun selectedDocumentIsEncryptedAndVerifiedBeforeSuccess() {
        requireIsolatedPackage()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = (compose.activity.application as PieApplication).container
        val before = runBlocking { container.diaryRepository.observeSummaries().first() }
        val destination = File.createTempFile("drive-fixture-", ".pie", compose.activity.cacheDir)
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CREATE_DOCUMENT) return null
                return Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(destination)))
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            openDrive()
            compose.onNodeWithText("สำรองไป Google Drive").performClick()
            waitText("เลือกที่เก็บบน Google Drive")
            val fields = compose.onAllNodes(hasSetTextAction())
            fields[0].performTextReplacement("drive-fixture-password")
            fields[1].performTextReplacement("drive-fixture-password")
            compose.onNodeWithText("เลือกที่เก็บบน Google Drive").performClick()
            waitText("สร้างไฟล์สำรองแล้ว · ตรวจสถานะอัปโหลดในแอป Drive")
            val header = ByteArray(8)
            destination.inputStream().use { java.io.DataInputStream(it).readFully(header) }
            assertEquals("PIEBKP02", String(header, Charsets.US_ASCII))
            assertTrue(destination.length() > 24)
            assertTrue(runBlocking { container.settingsRepository.settings.first().lastBackupAtMillis } != null)
            assertEquals(before, runBlocking { container.diaryRepository.observeSummaries().first() })
        } finally {
            instrumentation.removeMonitor(monitor)
            destination.delete()
        }
    }

    private fun requireIsolatedPackage() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".devicetest"))
    }

    @Test
    fun narrowScreenWithLargeTextKeepsActionsReachable() {
        requireIsolatedPackage()
        var exported = false
        var imported = false
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.5f)) {
                    PieTheme {
                        Box(Modifier.width(320.dp).fillMaxHeight()) {
                            DriveScreen(onBack = {}, onExport = { exported = true }, onImport = { imported = true })
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("สำรองไป Google Drive").assertIsDisplayed().performClick()
        assertTrue(exported)
        compose.onNodeWithText("นำไฟล์จาก Drive กลับมา").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(imported)
    }

    private fun openDrive() {
        waitText("ตั้งค่า")
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        waitText("Google Drive")
        compose.onNodeWithText("Google Drive").performScrollTo().performClick()
        waitText("สำรองข้อมูล\nบน Google Drive")
    }

    private fun waitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}
