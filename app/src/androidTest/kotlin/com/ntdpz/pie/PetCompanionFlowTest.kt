package com.ntdpz.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.domain.pet.PetPosition
import com.ntdpz.pie.core.domain.pet.PetPreferences
import com.ntdpz.pie.core.domain.pet.PetReaction
import com.ntdpz.pie.core.domain.settings.ReminderMessages
import com.ntdpz.pie.core.domain.settings.ThemeMode
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PetCompanionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = (context.applicationContext as PieApplication).container.petRepository
    private var previous: PetPreferences? = null
    private val settingsRepository get() = (context.applicationContext as PieApplication).container.settingsRepository
    private var previousTheme: ThemeMode? = null

    @Before
    fun prepareIsolatedCompanion() = runBlocking {
        check(context.packageName.endsWith(".devicetest")) { "Run with -Ppie.deviceTest=true" }
        previous = repository.preferences.first()
        previousTheme = settingsRepository.settings.first().themeMode
        repository.setPosition(PetPosition())
        repository.setReducedMotion(true)
        repository.setEnabled(true)
    }

    @After
    fun restoreCompanion(): Unit = runBlocking {
        previousTheme?.let { settingsRepository.saveTheme(it) }
        previous?.let {
            repository.setEnabled(it.enabled)
            repository.setReducedMotion(it.reducedMotion)
            repository.setPosition(it.position)
        }
    }

    @Test
    fun companionAndPoseGalleryFollowDarkLightAndSystemOverrides() {
        runBlocking { settingsRepository.saveTheme(ThemeMode.Dark) }
        waitForPet()
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("เพื่อนของฉัน · PIE").performScrollTo().performClick()
        assertBackground(0xFF302A32.toInt())
        savePreview("companion-dark.png")
        compose.onNodeWithTag("pet-all-poses").performScrollTo().performClick()
        assertBackground(0xFF302A32.toInt())
        savePreview("companion-poses-dark.png")
        compose.onNodeWithTag("pet-poses-back").performScrollTo().performClick()
        runBlocking { settingsRepository.saveTheme(ThemeMode.Light) }
        assertBackground(0xFFFFF4E8.toInt())
        compose.onNodeWithTag("pet-all-poses").performScrollTo().performClick()
        assertBackground(0xFFFFF4E8.toInt())
        runBlocking { settingsRepository.saveTheme(ThemeMode.System) }
        val systemDark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        assertBackground(if (systemDark) 0xFF302A32.toInt() else 0xFFFFF4E8.toInt())
    }

    private fun assertBackground(expected: Int) {
        compose.waitUntil(10_000) {
            compose.onRoot().captureToImage().asAndroidBitmap().getPixel(2, 2) == expected
        }
    }

    @Test
    fun screenTouchesReactWithoutBlockingScrollOrNavigation() {
        waitForPet()
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        var previous: String? = null
        repeat(2) {
            compose.waitUntil(6_000) {
                compose.onNodeWithTag("pet-companion").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "อยู่เป็นเพื่อน"
            }
            compose.onNodeWithText("พื้นที่ของคุณ").performTouchInput { down(center); up() }
            val reaction = compose.onNodeWithTag("pet-companion").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertNotEquals("อยู่เป็นเพื่อน", reaction)
            assertNotEquals(previous, reaction)
            previous = reaction
            compose.onNodeWithText(ReminderMessages.defaultForDay(LocalDate.now())).assertDoesNotExist()
        }
        compose.onRoot().performTouchInput { down(center); moveBy(Offset(0f, -500f)); up() }
        compose.onNodeWithText("พื้นที่ของคุณ").assertIsNotDisplayed()
        compose.onNode(hasText("วันนี้") and hasClickAction()).performTouchInput { down(center); up() }
        compose.onNodeWithText("พื้นที่ของคุณ").assertDoesNotExist()
        waitForPet()
    }

    @Test
    fun everyReactionCanBePreviewedAndPreviewDoesNotBecomeSaveSuccess() {
        waitForPet()
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("เพื่อนของฉัน · PIE").performScrollTo().performClick()
        compose.onNodeWithTag("pet-all-poses").performScrollTo().performClick()
        PetReaction.entries.forEach { reaction ->
            compose.onNodeWithTag("pet-preview-${reaction.name}").performScrollTo().performClick()
            compose.onNodeWithTag("pet-reaction-preview").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("เปิดภาพ PIE ไม่สำเร็จ").assertDoesNotExist()
            compose.onNodeWithText("บันทึกเรียบร้อยแล้ว").assertDoesNotExist()
        }
        compose.onNodeWithTag("pet-poses-back").performScrollTo().performClick()
        compose.onNodeWithTag("pet-enabled").assertIsOn()
        compose.onNodeWithTag("pet-reduced-motion").assertIsOn()
        compose.onNodeWithTag("pet-hero").performScrollTo().performClick()
        compose.onNodeWithContentDescription("กลับ").performScrollTo()
        compose.mainClock.advanceTimeBy(2_000)
        savePreview("companion-settings.png")
        compose.onNodeWithTag("pet-all-poses").performScrollTo().performClick()
        compose.onNodeWithTag("pet-reaction-preview").performScrollTo()
        savePreview("companion-poses.png")
        compose.onNodeWithContentDescription("กลับ").performScrollTo().performClick()
        compose.onNodeWithContentDescription("กลับ").performScrollTo().performClick()
        waitForPet()
    }

    @Test
    fun dragPersistsAcrossRecreationAndDoesNotBlockNavigation() {
        waitForPet()
        compose.onNodeWithTag("pet-companion").performTouchInput {
            down(center)
            moveBy(Offset(-140f, -100f))
            up()
        }
        compose.waitUntil(10_000) { runBlocking { repository.preferences.first().position != PetPosition() } }
        val saved = runBlocking { repository.preferences.first().position }
        compose.activityRule.scenario.recreate()
        waitForPet()
        assertEquals(saved, runBlocking { repository.preferences.first().position })
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("เพื่อนของฉัน · PIE").performScrollTo().performClick()
        compose.onNodeWithText("เพื่อนของฉัน").assertIsDisplayed()
        compose.onNodeWithTag("pet-companion").assertDoesNotExist()
        compose.onNodeWithTag("pet-enabled").assertIsOn()
        compose.onNodeWithTag("pet-reduced-motion").assertIsOn()
        compose.onNodeWithTag("pet-reset-position").performClick()
        compose.waitUntil(10_000) { runBlocking { repository.preferences.first().position == PetPosition() } }
    }

    @Test
    fun tapKeepsDailyMessageAndHideCanBeReversedInSettings() {
        waitForPet()
        val daily = ReminderMessages.defaultForDay(LocalDate.now())
        repeat(2) { compose.onNodeWithTag("pet-companion").performClick() }
        compose.onNodeWithText(daily).assertIsDisplayed()
        val actions = compose.onNodeWithTag("pet-companion").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { actions.first { it.label == "ซ่อน PIE" }.action() }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("pet-companion").fetchSemanticsNodes().isEmpty() }
        compose.onNode(hasText("ตั้งค่า") and hasClickAction()).performClick()
        compose.onNodeWithText("เพื่อนของฉัน · PIE").performScrollTo().performClick()
        compose.onNodeWithTag("pet-enabled").assertIsOff().performClick()
        compose.waitUntil(10_000) { runBlocking { repository.preferences.first().enabled } }
        compose.onNodeWithTag("pet-enabled").assertIsOn()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("pet-enabled").assertIsOn()
        compose.onNodeWithContentDescription("กลับ").performClick()
        waitForPet()
        compose.waitForIdle()
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "companion-test-preview.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun waitForPet() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pet-companion").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun savePreview(name: String) {
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
