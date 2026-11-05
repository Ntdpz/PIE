package com.ntdpz.pie.feature.editor

import com.ntdpz.pie.core.domain.diary.DiaryEntry
import com.ntdpz.pie.core.domain.diary.DiaryRepository
import com.ntdpz.pie.core.domain.diary.DiarySummary
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.core.domain.media.DiaryMedia
import com.ntdpz.pie.core.domain.media.DiaryMediaRepository
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(Instant.parse("2026-11-03T01:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun failedReadRejectsMutationsAndRetryRestoresExistingText() = runTest(dispatcher) {
        val repository = EditorDiaryFixture(date).apply { failRead = true }
        val media = EditorMediaFixture()
        val writer = DiaryWriter(repository, backgroundScope, clock)
        val vm = EditorViewModel(date, repository, writer, media)
        runCurrent()
        assertTrue(vm.state.value.loadFailed)
        vm.onTextChanged("replacement")
        vm.retrySave()
        vm.deleteEntry()
        vm.importMedia(listOf("fixture"))
        vm.deleteMedia("fixture")
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        assertEquals(0, repository.deletions)
        assertEquals(0, media.mutations)
        repository.failRead = false
        vm.load()
        runCurrent()
        assertEquals("existing", vm.state.value.text)
        assertFalse(vm.state.value.loadFailed)
    }

    @Test
    fun repeatedLoadWhileReadingDoesNotStartAnotherRead() = runTest(dispatcher) {
        val repository = EditorDiaryFixture(date).apply { readGate = CompletableDeferred() }
        val vm = EditorViewModel(date, repository, DiaryWriter(repository, backgroundScope, clock), EditorMediaFixture())
        runCurrent()
        vm.load()
        vm.load()
        runCurrent()
        assertEquals(1, repository.reads)
        repository.readGate!!.complete(Unit)
        runCurrent()
        assertEquals("existing", vm.state.value.text)
    }

    @Test
    fun failedWriteKeepsTextAndRetryUsesOriginalSelectedDate() = runTest(dispatcher) {
        val repository = EditorDiaryFixture(date).apply { failWrite = true }
        val vm = EditorViewModel(date, repository, DiaryWriter(repository, backgroundScope, clock), EditorMediaFixture())
        runCurrent()
        vm.onTextChanged("ข้อความไทย 🫶")
        runCurrent()
        assertTrue(vm.state.value.saveState is SaveState.Failed)
        assertEquals("ข้อความไทย 🫶", vm.state.value.text)
        repository.failWrite = false
        vm.retrySave()
        runCurrent()
        assertEquals(date, repository.writes.single().date)
        assertEquals("ข้อความไทย 🫶", repository.writes.single().text)
        assertTrue(vm.state.value.saveState is SaveState.Saved)
    }

    @Test
    fun reopeningFailedDraftDoesNotReplaceItWithDiskText() = runTest(dispatcher) {
        val repository = EditorDiaryFixture(date).apply { failWrite = true }
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "unsaved latest")
        runCurrent()
        val vm = EditorViewModel(date, repository, writer, EditorMediaFixture())
        runCurrent()
        assertEquals("unsaved latest", vm.state.value.text)
        assertEquals(0, repository.reads)
        assertTrue(vm.state.value.saveState is SaveState.Failed)
    }

    @Test
    fun completedDeleteRejectsLateEditAndMediaCallbacks() = runTest(dispatcher) {
        val repository = EditorDiaryFixture(date)
        val media = EditorMediaFixture()
        val vm = EditorViewModel(date, repository, DiaryWriter(repository, backgroundScope, clock), media)
        runCurrent()
        vm.deleteEntry()
        runCurrent()
        assertTrue(vm.state.value.deleted)
        vm.onTextChanged("late callback")
        vm.retrySave()
        vm.deleteEntry()
        vm.importMedia(listOf("fixture"))
        vm.deleteMedia("fixture")
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        assertEquals(1, repository.deletions)
        assertEquals(0, media.mutations)
    }

    @Test
    fun pendingDeleteRejectsWritesAndFailedDeleteAllowsRecovery() = runTest(dispatcher) {
        val repository = EditorDiaryFixture(date).apply {
            deleteGate = CompletableDeferred()
            failDelete = true
        }
        val vm = EditorViewModel(date, repository, DiaryWriter(repository, backgroundScope, clock), EditorMediaFixture())
        runCurrent()
        vm.deleteEntry()
        runCurrent()
        assertTrue(vm.state.value.deleting)
        vm.onTextChanged("late callback")
        vm.retrySave()
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        repository.deleteGate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.deleted)
        assertFalse(vm.state.value.deleting)
        assertNotNull(vm.state.value.message)
        assertEquals("existing", vm.state.value.text)
        vm.onTextChanged("recovered")
        runCurrent()
        assertEquals("recovered", repository.writes.single().text)
    }
}

private class EditorDiaryFixture(date: LocalDate) : DiaryRepository {
    private val entry = DiaryEntry(date, "existing", Instant.EPOCH)
    var failRead = false
    var failWrite = false
    var failDelete = false
    var readGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null
    var reads = 0
    var deletions = 0
    val writes = mutableListOf<DiaryEntry>()

    override fun observeEntry(date: LocalDate) = flowOf(entry)
    override fun observeSummaries() = flowOf(emptyList<DiarySummary>())
    override suspend fun readEntry(date: LocalDate): DiaryEntry {
        reads++
        readGate?.await()
        if (failRead) throw IOException("fixture read failure")
        return entry
    }
    override suspend fun saveEntry(entry: DiaryEntry) {
        if (failWrite) throw IOException("fixture write failure")
        writes += entry
    }
    override suspend fun deleteEntry(date: LocalDate) {
        deletions++
        deleteGate?.await()
        if (failDelete) throw IOException("fixture delete failure")
    }
}

private class EditorMediaFixture : DiaryMediaRepository {
    var mutations = 0
    override fun observeAllMedia() = flowOf(emptyMap<LocalDate, List<DiaryMedia>>())
    override fun observeMedia(date: LocalDate) = flowOf(emptyList<DiaryMedia>())
    override suspend fun importMedia(date: LocalDate, uri: String) {
        mutations++
    }
    override suspend fun deleteMedia(id: String) {
        mutations++
    }
}
