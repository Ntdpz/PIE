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
    @Test fun videoAutoplaysMutedAndResetsSoundWhenBackgroundedOrScrolledAway() = exercise(false, video = true)

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
            val calendar = compose.onNodeWithContentDescription("เลือกวันจากปฏิทิน")
            val calendarBounds = calendar.assertIsDisplayed().getUnclippedBoundsInRoot()
            // Samsung's fractional density can round an exact 48 dp bound below 48f.
            assertTrue("Calendar touch target must be at least 48 dp wide", (calendarBounds.right - calendarBounds.left).value >= 47.99f)
            assertTrue("Calendar touch target must be at least 48 dp high", (calendarBounds.bottom - calendarBounds.top).value >= 47.99f)
            val gallery = compose.onNodeWithTag("memory-gallery")
            val tileTags = added.map { "memory-tile-$day-$it" }
            assertEquals(4, tileTags.size)
            tileTags.forEach { tag ->
                gallery.performScrollToNode(hasTestTag(tag))
                compose.onNodeWithTag(tag).assertIsDisplayed()
            }
            val firstTile = tileTags.first()
            gallery.performScrollToNode(hasTestTag(firstTile))
            val tile = compose.onNodeWithTag(firstTile)
            val bounds = tile.getUnclippedBoundsInRoot()
            assertEquals((bounds.right - bounds.left).value, (bounds.bottom - bounds.top).value, 1f)
            tile.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnLongClick)
            compose.onNodeWithTag("memory-hold-preview").assertIsDisplayed()
            compose.onNodeWithTag("memory-hold-preview").performTouchInput { click() }
            compose.onNodeWithTag("memory-hold-preview").assertDoesNotExist()
            tile.performTouchInput { longClick() }
            compose.onNodeWithTag("memory-hold-preview").assertDoesNotExist()
            saveScreen(if (dark) "gallery-dark" else "gallery-light")
            compose.onNodeWithTag(firstTile).performClick()
            waitTag("memory-photos-$day")
            if (video) {
                compose.waitUntil(15_000) { compose.onAllNodesWithText("เปิดเสียง").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("เปิดเสียง").performClick()
                compose.onNodeWithText("ปิดเสียง").assertIsDisplayed()
                compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                compose.waitUntil(15_000) { compose.onNodeWithText("เปิดเสียง").isDisplayed() }
            } else assertMediaRatio(day, 16f / 9)
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            waitCounter("2 จาก 4")
            if (video) compose.onNodeWithText("เปิดเสียง").assertDoesNotExist()
            assertMediaRatio(day, 4f / 3)
            saveScreen(if (dark) "feed-landscape-dark" else "feed-landscape-light")
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            waitCounter("3 จาก 4")
            assertPortraitFillsFeed(day)
            compose.onNodeWithTag("memory-photos-$day").performTouchInput { swipeLeft() }
            waitCounter("4 จาก 4")
            assertPortraitFillsFeed(day)
            waitTag("memory-story-$day")
            compose.onNodeWithTag("memory-story-$day").assertTextEquals(fullText)
            compose.onNodeWithText("อ่านต่อ").assertDoesNotExist()
            compose.onNodeWithContentDescription("ปิดเรื่องเล่า").assertDoesNotExist()
            compose.onNodeWithTag("memory-entry-$day").performTouchInput { swipeUp() }
            compose.onNodeWithTag("memory-story-$day").assertTextEquals(fullText)
            saveScreen(if (dark) "feed-reading-dark" else "feed-reading-light")
            compose.onNodeWithTag("memory-feed").performTouchInput { swipeLeft() }
            compose.onNodeWithTag("memory-entry-$oldDay").assertDoesNotExist()
            compose.onNodeWithContentDescription("กลับสู่ความทรงจำ").performClick()
            val storyTile = "memory-tile-$oldDay-"
            gallery.performScrollToNode(hasTestTag(storyTile))
            compose.onNodeWithTag(storyTile).performClick()
            waitTag("memory-story-$oldDay")
            compose.onNodeWithTag("memory-story-$oldDay").assertTextEquals("เรื่องเล่าในวันที่ไม่มีรูป")
            compose.onNodeWithTag("memory-media-$oldDay").assertDoesNotExist()
            compose.onNodeWithTag("memory-edit-$oldDay").performClick()
            compose.onNode(hasSetTextAction()).assertTextEquals("เรื่องเล่าในวันที่ไม่มีรูป")
            compose.onNodeWithContentDescription("กลับ").performClick()
            compose.onNodeWithContentDescription("กลับสู่ความทรงจำ").performClick()
            compose.onNodeWithTag(storyTile).assertIsDisplayed()
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

    private fun waitTag(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun waitCounter(label: String) {
        compose.waitUntil(15_000) { compose.onAllNodes(hasContentDescription("รูปที่ $label") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun assertPortraitFillsFeed(day: LocalDate) {
        compose.waitUntil(15_000) {
            val media = compose.onNodeWithTag("memory-media-$day").getUnclippedBoundsInRoot()
            val feed = compose.onNodeWithTag("memory-feed").getUnclippedBoundsInRoot()
            kotlin.math.abs((media.bottom - media.top - (feed.bottom - feed.top)).value) < 2f
        }
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
            put(MediaStore.Images.Media.DISPLAY_NAME, "PIE-R3-$name-${System.nanoTime()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PIE-r3-tests")
        })!!
        context.contentResolver.openOutputStream(uri)!!.use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
