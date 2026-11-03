package com.ntdpz.pie

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.designsystem.theme.PieTheme
import com.ntdpz.pie.core.domain.diary.DiaryEntry
import com.ntdpz.pie.feature.settings.backup.BackupRoute
import com.ntdpz.pie.feature.settings.backup.BackupViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MergeImportFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun reviewChoicesApplyToRealEncryptedArchiveAndRepeatedImportDoesNotDuplicateText() = runMergeFlow(false)

    @Test fun darkMergeFlowKeepsChoicesWhenReturningToOverview() = runMergeFlow(true)

    private fun runMergeFlow(dark: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as PieApplication).container
        val date = LocalDate.of(1998, 12, if (dark) 27 else 28)
        val original = runBlocking { container.diaryRepository.readEntry(date) }
        val archive = File.createTempFile("pie-ui-merge", ".pie", context.cacheDir)
        val vm = BackupViewModel(container.backupRepository, container.diaryWriter, container.mergeImportRepository, container.applicationScope)
        val store = ViewModelStore().apply { put("merge-test", vm) }
        try {
            runBlocking {
                container.diaryWriter.awaitIdle()
                container.diaryRepository.saveEntry(DiaryEntry(date, "ข้อความจากไฟล์ทดสอบ", Instant.now()))
                container.backupRepository.export(Uri.fromFile(archive).toString(), "test-backup-password".toCharArray())
                container.diaryRepository.saveEntry(DiaryEntry(date, "ข้อความในเครื่องทดสอบ", Instant.now()))
            }
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent { PieTheme(darkTheme = dark) { BackupRoute(vm, restoring = true, onBack = {}) } }
            }
            vm.run(Uri.fromFile(archive).toString(), "test-backup-password".toCharArray(), true)
            compose.waitUntil(30_000) { vm.state.value.preview != null }
            compose.onNodeWithText("ตรวจรายการก่อนนำเข้า").assertIsDisplayed()
            saveReview(context, if (dark) "overview-dark" else "overview")
            compose.onNodeWithText("เลือกข้อความ 1 วัน").performClick()
            compose.onNodeWithText("วันนี้อยากเก็บแบบไหน?").assertIsDisplayed()
            compose.onNodeWithText("เก็บทั้งสองข้อความ").performScrollTo().performClick()
            if (dark) {
                compose.onNodeWithText("กลับไปภาพรวม").performClick()
                compose.onNodeWithText("เลือกข้อความ 1 วัน").performClick()
                compose.onNodeWithText("เก็บทั้งสองข้อความ").assertIsSelected()
            }
            saveReview(context, if (dark) "choice-dark" else "choice")
            assertEquals("ข้อความในเครื่องทดสอบ", runBlocking { container.diaryRepository.readEntry(date)!!.text })
            compose.onNodeWithText("ใช้ตัวเลือกนี้ · ตรวจสรุป").performClick()
            compose.onNodeWithText("รวมเรื่องเล่าเข้าด้วยกัน").assertIsDisplayed()
            saveReview(context, if (dark) "confirm-dark" else "confirm")
            compose.onNodeWithText("ยืนยันนำเข้า").performClick()
            compose.waitUntil(30_000) { vm.state.value.completed }
            compose.onNodeWithText("เรื่องเล่ากลับมาอยู่ด้วยกันแล้ว").assertIsDisplayed()
            saveReview(context, if (dark) "success-dark" else "success")
            val merged = runBlocking { container.diaryRepository.readEntry(date)!!.text }
            assertTrue(merged.startsWith("ข้อความในเครื่องทดสอบ"))
            assertTrue(merged.endsWith("ข้อความจากไฟล์ทดสอบ"))
            assertEquals(1, vm.state.value.result!!.changedDays)

            vm.run(Uri.fromFile(archive).toString(), "test-backup-password".toCharArray(), true)
            compose.waitUntil(30_000) { vm.state.value.preview != null }
            assertTrue(vm.state.value.preview!!.conflicts.isEmpty())
            compose.onNodeWithText("ตรวจสรุปก่อนนำเข้า").performClick()
            compose.onNodeWithText("ยืนยันนำเข้า").performClick()
            compose.waitUntil(30_000) { vm.state.value.completed }
            assertEquals(merged, runBlocking { container.diaryRepository.readEntry(date)!!.text })
            assertEquals(0, vm.state.value.result!!.changedDays)
        } finally {
            store.clear()
            archive.delete()
            runBlocking {
                if (original == null) container.diaryRepository.deleteEntry(date) else container.diaryRepository.saveEntry(original)
            }
        }
    }

    private fun saveReview(context: android.content.Context, stage: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-merge-$stage.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PIE-merge-tests")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(uri)!!.use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
