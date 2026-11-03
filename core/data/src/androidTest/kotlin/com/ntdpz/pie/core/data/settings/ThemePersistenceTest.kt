package com.ntdpz.pie.core.data.settings

import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.domain.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePersistenceTest {
    @Test
    fun themeIsRetainedAndDoesNotChangeReminderPreferences() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = LocalSettingsRepository(context)
        val original = repo.settings.first()
        try {
            for (mode in listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.System)) {
                repo.saveTheme(mode)
                val reloaded = LocalSettingsRepository(context).settings.first()
                assertEquals(mode, reloaded.themeMode)
                assertEquals(original.reminder, reloaded.reminder)
                assertEquals(original.pinEnabled, reloaded.pinEnabled)
            }
        } finally { repo.saveTheme(original.themeMode) }
    }
}
