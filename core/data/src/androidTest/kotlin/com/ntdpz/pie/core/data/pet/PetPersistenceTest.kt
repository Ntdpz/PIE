package com.ntdpz.pie.core.data.pet

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ntdpz.pie.core.domain.pet.PetPosition
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPersistenceTest {
    @Test
    fun reopeningKeepsPositionVisibilityAndMotionWithoutChangingOtherFields() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("companion-", ".preferences_pb", context.cacheDir)
        file.delete()
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val first = LocalPetRepository(PreferenceDataStoreFactory.create(
                scope = CoroutineScope(firstJob + Dispatchers.IO), produceFile = { file },
            ))
            first.setEnabled(false)
            first.setPosition(PetPosition(0.15f, 0.85f))
            first.setReducedMotion(true)
            firstJob.cancel()
            firstJob.join()
            val second = LocalPetRepository(PreferenceDataStoreFactory.create(
                scope = CoroutineScope(secondJob + Dispatchers.IO), produceFile = { file },
            ))
            val restored = second.preferences.first()
            assertFalse(restored.enabled)
            assertTrue(restored.reducedMotion)
            assertEquals(PetPosition(0.15f, 0.85f), restored.position)
            second.setEnabled(true)
            assertEquals(restored.copy(enabled = true), second.preferences.first())
        } finally {
            firstJob.cancel(); firstJob.join()
            secondJob.cancel(); secondJob.join()
            file.delete()
        }
    }

    @Test
    fun invalidStoredCoordinatesUseSafeBounds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("companion-invalid-", ".preferences_pb", context.cacheDir)
        file.delete()
        val job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file },
            )
            store.edit {
                it[floatPreferencesKey("position_x")] = Float.NaN
                it[floatPreferencesKey("position_y")] = -10f
            }
            assertEquals(PetPosition(1f, 0f), LocalPetRepository(store).preferences.first().position)
        } finally {
            job.cancel(); job.join()
            file.delete()
        }
    }
}
