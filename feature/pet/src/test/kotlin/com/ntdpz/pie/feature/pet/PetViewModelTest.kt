package com.ntdpz.pie.feature.pet

import androidx.lifecycle.ViewModelStore
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.pet.PetPose
import com.ntdpz.pie.core.domain.pet.PetPosition
import com.ntdpz.pie.core.domain.pet.PetPreferences
import com.ntdpz.pie.core.domain.pet.PetRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC)
    private var time = 0L

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun greetingIsStableTapDoesNotReshuffleAndIdleEventuallyRests() = runTest(dispatcher) {
        val vm = PetViewModel(MemoryPetRepository(), MutableStateFlow(emptyMap()), clock, { time }, backgroundScope)
        val store = ViewModelStore().apply { put("pet", vm) }
        try {
            runCurrent()
            vm.resume()
            val message = vm.state.value.dailyMessage
            assertTrue(message.isNotBlank())
            vm.tap()
            assertEquals(message, vm.state.value.dailyMessage)
            time = 50_000
            vm.tick()
            assertEquals(PetPose.Rest, vm.state.value.pose)
            vm.tap()
            assertEquals(PetPose.Tap, vm.state.value.pose)
            vm.pause()
            assertEquals(PetPose.Idle, vm.state.value.pose)
            vm.resume()
            assertEquals(message, vm.state.value.dailyMessage)
            assertEquals(PetPose.Idle, vm.state.value.pose)
        } finally { store.clear() }
    }

    @Test
    fun acceptedSettingsDrainInOrderAfterLeavingAndKeepOtherFields() = runTest(dispatcher) {
        val repository = MemoryPetRepository()
        val vm = PetViewModel(repository, MutableStateFlow(emptyMap()), clock, { time }, backgroundScope)
        val store = ViewModelStore().apply { put("pet", vm) }
        runCurrent()
        vm.setEnabled(false)
        vm.setPosition(PetPosition(0.2f, 0.8f))
        vm.setReducedMotion(true)
        vm.setEnabled(true)
        store.clear()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf("enabled:false", "position", "reduced:true", "enabled:true"), repository.writes)
        assertEquals(PetPreferences(true, true, PetPosition(0.2f, 0.8f)), repository.preferences.value)
    }

    @Test
    fun failedPreferenceSaveKeepsPreviousStateAndCanBeRetried() = runTest(dispatcher) {
        val repository = MemoryPetRepository()
        val vm = PetViewModel(repository, MutableStateFlow(emptyMap()), clock, { time }, backgroundScope)
        val store = ViewModelStore().apply { put("pet", vm) }
        try {
            runCurrent()
            repository.failNext = true
            vm.setEnabled(false)
            advanceTimeBy(20)
            runCurrent()
            assertTrue(vm.state.value.preferences.enabled)
            assertTrue(vm.state.value.error != null)
            vm.setEnabled(false)
            advanceTimeBy(20)
            runCurrent()
            assertFalse(vm.state.value.preferences.enabled)
            assertNull(vm.state.value.error)
        } finally { store.clear() }
    }

    @Test
    fun onlyCompletedEditsCelebrateOnceOnExitNotEveryAutosave() = runTest(dispatcher) {
        val saves = MutableStateFlow<Map<LocalDate, SaveState>>(emptyMap())
        val vm = PetViewModel(MemoryPetRepository(), saves, clock, { time }, backgroundScope)
        val store = ViewModelStore().apply { put("pet", vm) }
        val date = LocalDate.now(clock)
        try {
            runCurrent()
            vm.resume()
            vm.beginEditing(date)
            saves.value = mapOf(date to SaveState.Saved(1))
            runCurrent()
            assertEquals(PetPose.Greeting, vm.state.value.pose)
            vm.finishEditing(date)
            assertEquals(PetPose.Saved, vm.state.value.pose)
            time = 3_000
            vm.tick()
            saves.value = mapOf(date.plusDays(1) to SaveState.Saved(2))
            runCurrent()
            assertEquals(PetPose.Idle, vm.state.value.pose)
            vm.beginEditing(date)
            vm.finishEditing(date)
            vm.pause()
            saves.value = mapOf(date to SaveState.Saved(3))
            runCurrent()
            vm.resume()
            assertEquals(PetPose.Idle, vm.state.value.pose)
        } finally { store.clear() }
    }

    @Test
    fun readCancellationIsNotPresentedAsASettingsFailure() = runTest(dispatcher) {
        val repository = object : PetRepository by MemoryPetRepository() {
            override val preferences = kotlinx.coroutines.flow.flow<PetPreferences> { throw CancellationException() }
        }
        val vm = PetViewModel(repository, MutableStateFlow(emptyMap()), clock, { time }, backgroundScope)
        val store = ViewModelStore().apply { put("pet", vm) }
        runCurrent()
        assertNull(vm.state.value.error)
        store.clear()
    }
}

private class MemoryPetRepository : PetRepository {
    override val preferences = MutableStateFlow(PetPreferences())
    val writes = mutableListOf<String>()
    var failNext = false

    private suspend fun beforeWrite(label: String) {
        delay(10)
        if (failNext) {
            failNext = false
            error("Simulated storage failure")
        }
        writes += label
    }

    override suspend fun setEnabled(enabled: Boolean) {
        beforeWrite("enabled:$enabled")
        preferences.value = preferences.value.copy(enabled = enabled)
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        beforeWrite("reduced:$enabled")
        preferences.value = preferences.value.copy(reducedMotion = enabled)
    }

    override suspend fun setPosition(position: PetPosition) {
        beforeWrite("position")
        preferences.value = preferences.value.copy(position = position)
    }
}
