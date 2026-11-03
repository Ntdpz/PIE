package com.ntdpz.pie

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.domain.diary.DiaryEntry
import com.ntdpz.pie.core.domain.settings.ThemeMode
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GalleryFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun lightGalleryOpensPhotosAndFullStoryAndSwipesToTextOnlyDay() = exercise(false)
    @Test fun darkGalleryOpensPhotosAndFullStoryAndSwipesToTextOnlyDay() = exercise(true)

    @Test fun videoRequiresPlayAndStopsWhenBackgroundedOrSwipedAway() = exercise(false, video = true)

    private fun exercise(dark: Boolean, video: Boolean = false) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = (context.applicationContext as PieApplication).container
        val day = LocalDate.of(1991, 3, if (video) 23 else if (dark) 13 else 3)
        val oldDay = day.minusDays(1)
        val fullText = "เรื่องเล่าทั้งหมด\n" + "บันทึกยาวที่ต้องอ่านต่อได้ครบ\n".repeat(60) + "ท้ายเรื่องที่ต้องยังอยู่"
        val original = runBlocking { listOf(app.diaryRepository.readEntry(day), app.diaryRepository.readEntry(oldDay)) }
        val previousTheme = runBlocking { app.settingsRepository.settings.first().themeMode }
        val added = mutableListOf<String>()
        try {
            runBlocking {
                app.diaryWriter.awaitIdle()
                app.settingsRepository.saveTheme(if (dark) ThemeMode.Dark else ThemeMode.Light)
                app.diaryRepository.saveEntry(DiaryEntry(day, fullText, Instant.now()))
                app.diaryRepository.saveEntry(DiaryEntry(oldDay, "เรื่องเล่าในวันที่ไม่มีรูป", Instant.now()))
                val before = app.mediaRepository.observeMedia(day).first().map { it.id }.toSet()
                repeat(2) { index ->
                    val isVideo = video && index == 0
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-gallery-fixture-${System.nanoTime()}." + if (isVideo) "mp4" else "png")
                        put(MediaStore.Images.Media.MIME_TYPE, if (isVideo) "video/mp4" else "image/png")
                    }
                    val uri = requireNotNull(context.contentResolver.insert(if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                    try {
                        if (isVideo) {
                            InstrumentationRegistry.getInstrumentation().context.assets.open("memory_fixture.mp4").use { input -> context.contentResolver.openOutputStream(uri)!!.use { input.copyTo(it) } }
                        } else {
                        val bitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888).apply { eraseColor(if (index == 0) Color.rgb(185, 120, 95) else Color.rgb(85, 115, 100)) }
                        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                        }
                        app.mediaRepository.importMedia(day, uri.toString())
                    } finally { context.contentResolver.delete(uri, null, null) }
                }
                added += app.mediaRepository.observeMedia(day).first().map { it.id }.filter { it !in before }
            }
            compose.onNode(hasText("ความทรงจำ") and hasClickAction()).performClick()
            compose.onNodeWithTag("memory-gallery").performScrollToNode(hasText(PieDates.shortEntry(day)))
            saveScreen(if (dark) "gallery-dark" else "gallery-light")
            compose.onNodeWithText(PieDates.shortEntry(day)).performClick()
            if (video) {
                compose.onNodeWithText("เล่นวิดีโอ").performClick()
                compose.onNodeWithText("เปิดเสียง").performClick()
                compose.onNodeWithText("ปิดเสียง").assertIsDisplayed()
                compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                compose.onNodeWithText("เล่นวิดีโอ").assertIsDisplayed()
                compose.onNodeWithText("เล่นวิดีโอ").performClick()
            }
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            if (video) compose.onNodeWithText("เปิดเสียง").assertDoesNotExist()
            compose.onNodeWithText("2 / 2").assertIsDisplayed()
            compose.onNodeWithText("อ่านต่อ").performClick()
            compose.onNodeWithTag("memory-story").assertTextEquals(fullText)
            compose.onNodeWithTag("memory-story").performTouchInput { swipeUp() }
            compose.onNodeWithTag("memory-story").assertTextEquals(fullText)
            compose.onNodeWithContentDescription("ปิดเรื่องเล่า").performClick()
            saveScreen(if (dark) "memory-dark" else "memory-light")
            compose.onNodeWithTag("memory-media-$day").performTouchInput { swipeUp() }
            compose.onNodeWithTag("memory-story").assertTextEquals("เรื่องเล่าในวันที่ไม่มีรูป")
            compose.onNodeWithContentDescription("ปิดเรื่องเล่า").performClick()
            compose.onNodeWithContentDescription("แก้บันทึก").performClick()
            compose.onNode(hasSetTextAction()).assertTextEquals("เรื่องเล่าในวันที่ไม่มีรูป")
            compose.onNodeWithContentDescription("กลับ").performClick()
            compose.onNodeWithContentDescription("กลับสู่ความทรงจำ").performClick()
            compose.onNodeWithContentDescription("เลือกวันจากปฏิทิน").performClick()
            compose.onNodeWithText("เลือกวัน").assertIsDisplayed()
            compose.onNodeWithContentDescription("ปิดปฏิทิน").performClick()
            assertEquals(fullText, runBlocking { app.diaryRepository.readEntry(day)!!.text })
        } finally {
            runBlocking {
                app.diaryWriter.awaitIdle()
                added.forEach { app.mediaRepository.deleteMedia(it) }
                for ((index, date) in listOf(day, oldDay).withIndex()) {
                    original[index]?.let { app.diaryRepository.saveEntry(it) } ?: app.diaryRepository.deleteEntry(date)
                }
                app.settingsRepository.saveTheme(previousTheme)
            }
        }
    }

    private fun saveScreen(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-R3-$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PIE-r3-tests")
        })!!
        context.contentResolver.openOutputStream(uri)!!.use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
