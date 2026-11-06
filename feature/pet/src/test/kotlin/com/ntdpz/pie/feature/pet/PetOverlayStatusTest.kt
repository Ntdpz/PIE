package com.ntdpz.pie.feature.pet

import com.ntdpz.pie.feature.pet.overlay.PetOverlayPhase
import com.ntdpz.pie.feature.pet.overlay.PetOverlayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetOverlayStatusTest {
    @Test
    fun failureSurvivesServiceDestructionAndRetryClearsIt() {
        val status = PetOverlayStatus()
        status.starting()
        status.failed("permission denied")
        status.stopped()
        assertEquals(PetOverlayPhase.Failed, status.state.value.phase)
        assertEquals("permission denied", status.state.value.error)
        status.starting()
        assertNull(status.state.value.error)
        assertEquals(PetOverlayPhase.Starting, status.state.value.phase)
        status.running()
        assertEquals(PetOverlayPhase.Running, status.state.value.phase)
        status.stopped()
        assertEquals(PetOverlayPhase.Off, status.state.value.phase)
    }
}
