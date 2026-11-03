package com.ntdpz.pie.feature.history

import com.ntdpz.pie.core.domain.diary.*
import com.ntdpz.pie.core.domain.media.*
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 11, 3)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }

    @Test fun timelineSortsNewestFirstAndRetainsTextOnlyDays() = runTest(dispatcher) {
        val diary = FixtureDiary(listOf(entry(date.minusDays(1), "old"), entry(date, "new")))
        val media = FixtureMedia(mapOf(date to listOf(DiaryMedia("b", "fixture-b", true), DiaryMedia("a", "fixture-a", false))))
        val days = observeMemoryDays(diary, media).first()
        assertEquals(listOf(date, date.minusDays(1)), days.map { it.summary.date })
        assertTrue(days.last().media.isEmpty())
        assertEquals(listOf("b", "a"), days.first().media.map { it.id })
    }

    @Test fun selectedDayReceivesFullTextAndLiveUpdatesWithoutChangingTheDate() = runTest(dispatcher) {
        val old = date.minusDays(1)
        val longText = "full memory\n".repeat(200)
        val diary = FixtureDiary(listOf(entry(date, "today"), entry(old, longText)))
        val vm = MemoryViewModel(diary, FixtureMedia(), date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        vm.selectDate(old); runCurrent()
        assertEquals(old, vm.state.value.date)
        assertEquals(longText, vm.state.value.text)
        diary.entries.value = listOf(entry(date, "today"), entry(old, "edited old day"))
        runCurrent()
        assertEquals(old, vm.state.value.date)
        assertEquals("edited old day", vm.state.value.text)
        assertEquals(date, vm.initialDate)
    }

    @Test fun deletingEntriesRemovesCardsAndFailureCanBeRetried() = runTest(dispatcher) {
        val diary = FixtureDiary(listOf(entry(date, "fixture")))
        diary.fail = true
        val vm = MemoryViewModel(diary, FixtureMedia(), date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent(); assertTrue(vm.state.value.failed)
        diary.fail = false; vm.retry(); runCurrent()
        assertFalse(vm.state.value.failed)
        assertEquals(1, vm.state.value.days.size)
        diary.entries.value = emptyList(); runCurrent()
        assertTrue(vm.state.value.days.isEmpty())
        assertEquals("", vm.state.value.text)
    }

    private fun entry(date: LocalDate, text: String) = DiaryEntry(date, text, Instant.EPOCH)
}

private class FixtureDiary(initial: List<DiaryEntry>) : DiaryRepository {
    val entries = MutableStateFlow(initial)
    var fail = false
    override fun observeSummaries(): Flow<List<DiarySummary>> = flow {
        if (fail) error("fixture failure")
        emitAll(entries.map { it.map { e -> DiarySummary(e.date, e.text.take(160), e.updatedAt) } })
    }
    override fun observeEntry(date: LocalDate) = entries.map { it.find { e -> e.date == date } }
    override suspend fun readEntry(date: LocalDate) = entries.value.find { it.date == date }
    override suspend fun saveEntry(entry: DiaryEntry) = Unit
    override suspend fun deleteEntry(date: LocalDate) = Unit
}
private class FixtureMedia(initial: Map<LocalDate, List<DiaryMedia>> = emptyMap()) : DiaryMediaRepository {
    private val items = MutableStateFlow(initial)
    override fun observeAllMedia() = items
    override fun observeMedia(date: LocalDate) = items.map { it[date].orEmpty() }
    override suspend fun importMedia(date: LocalDate, uri: String) = Unit
    override suspend fun deleteMedia(id: String) = Unit
}
