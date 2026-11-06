package com.ntdpz.pie

import android.content.Intent
import android.content.ComponentName
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.ntdpz.pie.core.domain.pet.PetPosition

/** Permissions are configured before instrumentation; revocation can kill its process. */
class PetOverlayFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Before
    fun isolate() {
        check(context.packageName.endsWith(".devicetest"))
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    @After
    fun stop() {
        context.stopService(Intent(context, PiePetOverlayService::class.java))
    }

    @Test
    fun overlayAppearsOutsideAppHidesOnReturnAndNotificationActionStopsIt() {
        assertTrue(Settings.canDrawOverlays(context))
        compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
        compose.waitUntil(10_000) { shell("dumpsys activity services ${context.packageName}").contains("isForeground=true") }
        assertFalse(hasWindow())
        shell("input keyevent KEYCODE_HOME")
        compose.waitUntil(30_000) { hasWindow() }
        assertTrue(hasWindow())
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(10_000) { !hasWindow() }
        shell("input keyevent KEYCODE_HOME")
        compose.waitUntil(10_000) { hasWindow() }
        context.startService(Intent(context, PiePetOverlayService::class.java).setAction("stop"))
        compose.waitUntil(10_000) { !hasWindow() }
        compose.waitUntil(10_000) { !shell("dumpsys activity services ${context.packageName}").contains("isForeground=true") }
    }

    @Test
    fun deniedPermissionNeverShowsWindow() {
        assertFalse(Settings.canDrawOverlays(context))
        compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
        shell("input keyevent KEYCODE_HOME")
        assertFalse(hasWindow())
        compose.waitUntil(10_000) { !shell("dumpsys activity services ${context.packageName}").contains("isForeground=true") }
    }

    @Test
    fun explicitStopRemovesWindow() {
        assertTrue(Settings.canDrawOverlays(context))
        compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
        shell("input keyevent KEYCODE_HOME")
        compose.waitUntil(10_000) { hasWindow() }
        context.stopService(Intent(context, PiePetOverlayService::class.java))
        compose.waitUntil(10_000) { !hasWindow() }
    }

    @Test
    fun dragPersistsAndLongPressStopsCompanion() {
        assertTrue(Settings.canDrawOverlays(context))
        val repository = (context.applicationContext as PieApplication).container.petRepository
        val previous = runBlocking { repository.preferences.first().position }
        try {
            runBlocking { repository.setPosition(PetPosition(0.5f, 0.5f)) }
            compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
            shell("input keyevent KEYCODE_HOME")
            compose.waitUntil(15_000) { companionNode() != null }
            val bounds = Rect().also { companionNode()!!.getBoundsInScreen(it) }
            val x = bounds.centerX()
            val y = bounds.centerY()
            shell("input swipe $x $y ${x - 160} ${y - 180} 600")
            compose.waitUntil(10_000) {
                runBlocking { repository.preferences.first().position != PetPosition(0.5f, 0.5f) }
            }
            context.stopService(Intent(context, PiePetOverlayService::class.java))
            compose.waitUntil(10_000) { !hasWindow() }
            val saved = runBlocking { repository.preferences.first().position }
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            compose.waitForIdle()
            compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
            shell("input keyevent KEYCODE_HOME")
            compose.waitUntil(15_000) { companionNode() != null }
            org.junit.Assert.assertEquals(saved, runBlocking { repository.preferences.first().position })
            assertTrue(companionNode()!!.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
            compose.waitUntil(10_000) { !hasWindow() }
        } finally {
            runBlocking { repository.setPosition(previous) }
        }
    }

    @Test
    fun outsideTouchesReactRepeatedlyAndStillReachOtherApp() {
        assertTrue(Settings.canDrawOverlays(context))
        compose.runOnIdle { context.startForegroundService(Intent(context, PiePetOverlayService::class.java)) }
        context.startActivity(Intent().apply {
            component = ComponentName(context.packageName, OverlayTouchProbeActivity::class.java.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        compose.waitUntil(15_000) { companionNode() != null && probeNode() != null }
        repeat(3) { index ->
            compose.waitUntil(6_000) { companionNode()?.contentDescription?.contains("อยู่เป็นเพื่อน") == true }
            val bounds = Rect().also { probeNode()!!.getBoundsInScreen(it) }
            shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
            compose.waitUntil(2_000) { companionNode()?.contentDescription?.contains("ทักทาย") == true }
            compose.waitUntil(2_000) { probeNode()?.text?.toString()?.equals("Probe touches: ${index + 1}", ignoreCase = true) == true }
        }
        shell("input keyevent KEYCODE_BACK")
    }

    private fun probeNode(): AccessibilityNodeInfo? = findNode {
        it.packageName == context.packageName && it.contentDescription == "overlay-touch-probe"
    }

    private fun companionNode(): AccessibilityNodeInfo? = findNode {
        it.packageName == context.packageName && it.contentDescription?.startsWith("PIE นอกแอป") == true
    }

    private fun findNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (matches(node)) return node
            for (index in 0 until node.childCount) node.getChild(index)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return instrumentation.uiAutomation.windows.firstNotNullOfOrNull { it.root?.let(::find) }
    }

    private fun hasWindow() = shell("dumpsys window windows").split(Regex("  Window #\\d+ ")).any {
        it.contains("PIE companion") && it.contains("package=${context.packageName} ")
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
