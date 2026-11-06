package com.ntdpz.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
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
import com.ntdpz.pie.core.domain.settings.ReminderMessages
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PetCompanionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = (context.applicationContext as PieApplication).container.petRepository
    private var previous: PetPreferences? = null

    @Before
    fun prepareIsolatedCompanion() = runBlocking {
        check(context.packageName.endsWith(".devicetest")) { "Run with -Ppie.deviceTest=true" }
        previous = repository.preferences.first()
        repository.setPosition(PetPosition())
        repository.setReducedMotion(true)
        repository.setEnabled(true)
    }

    @After
    fun restoreCompanion(): Unit = runBlocking {
        previous?.let {
            repository.setEnabled(it.enabled)
            repository.setReducedMotion(it.reducedMotion)
            repository.setPosition(it.position)
        }
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
        compose.onNodeWithText("เพื่อนของฉัน · PIE").assertIsDisplayed()
        compose.onNodeWithTag("pet-companion").assertDoesNotExist()
        compose.onNodeWithTag("pet-enabled").assertIsOn()
        compose.onNodeWithTag("pet-reduced-motion").assertIsOn()
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
}
