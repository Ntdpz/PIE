package com.ntdpz.pie

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
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
    @Test fun lightFeedPreservesMediaRatiosAndReadsFullStoryInline() = exercise(false)
    @Test fun darkFeedPreservesMediaRatiosAndReadsFullStoryInline() = exercise(true)
    @Test fun videoRequiresPlayAndStopsWhenBackgroundedOrScrolledAway() = exercise(false, video = true)

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
                repeat(4) { index ->
                    val isVideo = video && index == 0
                    val jpeg = index == 3
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-feed-fixture-${System.nanoTime()}." + if (isVideo) "mp4" else if (jpeg) "jpg" else "png")
                        put(MediaStore.Images.Media.MIME_TYPE, if (isVideo) "video/mp4" else if (jpeg) "image/jpeg" else "image/png")
                    }
                    val uri = requireNotNull(context.contentResolver.insert(if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                    try {
                        if (isVideo) {
                            InstrumentationRegistry.getInstrumentation().context.assets.open("memory_fixture.mp4").use { input -> context.contentResolver.openOutputStream(uri)!!.use { input.copyTo(it) } }
                        } else {
                            val width = listOf(320, 320, 180, 400)[index]
                            val height = listOf(180, 240, 320, 300)[index]
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(if (index == 0) Color.rgb(185, 120, 95) else Color.rgb(85, 115, 100)) }
                            context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 100, it) }
                            bitmap.recycle()
                            if (jpeg) context.contentResolver.openFileDescriptor(uri, "rw")!!.use { descriptor ->
                                ExifInterface(descriptor.fileDescriptor).apply {
                                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                                    saveAttributes()
                                }
                            }
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
            waitTag("memory-photos-$day")
            if (video) {
                compose.waitUntil(15_000) { compose.onAllNodesWithText("เล่นวิดีโอ").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("เล่นวิดีโอ").performClick()
                compose.onNodeWithText("เปิดเสียง").performClick()
                compose.onNodeWithText("ปิดเสียง").assertIsDisplayed()
                compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                compose.onNodeWithText("เล่นวิดีโอ").assertIsDisplayed().performClick()
            } else assertMediaRatio(day, 16f / 9)
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            waitCounter("2 / 4")
            if (video) compose.onNodeWithText("เปิดเสียง").assertDoesNotExist()
            assertMediaRatio(day, 4f / 3)
            saveScreen(if (dark) "feed-landscape-dark" else "feed-landscape-light")
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            waitCounter("3 / 4")
            assertMediaRatio(day, 9f / 16)
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            waitCounter("4 / 4")
            assertMediaRatio(day, 3f / 4)
            waitTag("memory-story-$day")
            compose.onNodeWithTag("memory-story-$day").assertTextEquals(fullText)
            compose.onNodeWithText("อ่านต่อ").assertDoesNotExist()
            compose.onNodeWithContentDescription("ปิดเรื่องเล่า").assertDoesNotExist()
            compose.onNodeWithTag("memory-feed").performTouchInput { swipeUp() }
            compose.onNodeWithTag("memory-story-$day").assertTextEquals(fullText)
            saveScreen(if (dark) "feed-reading-dark" else "feed-reading-light")
            scrollToDay(oldDay)
            waitTag("memory-story-$oldDay")
            compose.onNodeWithTag("memory-story-$oldDay").assertTextEquals("เรื่องเล่าในวันที่ไม่มีรูป")
            compose.onNodeWithTag("memory-media-$oldDay").assertDoesNotExist()
            if (video) {
                scrollToDay(day)
                repeat(3) { compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeRight() } }
                waitCounter("1 / 4")
                compose.onNodeWithText("เล่นวิดีโอ").performClick()
                scrollToDay(oldDay)
                scrollToDay(day)
                compose.onNodeWithText("เล่นวิดีโอ").assertIsDisplayed()
                scrollToDay(oldDay)
            }
            compose.onNodeWithTag("memory-edit-$oldDay").performClick()
            compose.onNode(hasSetTextAction()).assertTextEquals("เรื่องเล่าในวันที่ไม่มีรูป")
            compose.onNodeWithContentDescription("กลับ").performClick()
            compose.onNodeWithContentDescription("กลับสู่ความทรงจำ").performClick()
            compose.onNodeWithText(PieDates.shortEntry(day)).assertIsDisplayed()
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

    private fun scrollToDay(day: LocalDate) {
        compose.onNodeWithTag("memory-feed").performScrollToNode(hasTestTag("memory-edit-$day"))
        compose.waitForIdle()
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun waitCounter(label: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun assertMediaRatio(day: LocalDate, ratio: Float) {
        compose.waitUntil(15_000) {
            val bounds = compose.onNodeWithTag("memory-media-$day").getUnclippedBoundsInRoot()
            kotlin.math.abs((bounds.right.value - bounds.left.value) / (bounds.bottom.value - bounds.top.value) - ratio) < .02f
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
